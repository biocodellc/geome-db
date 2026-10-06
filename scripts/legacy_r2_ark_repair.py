#!/usr/bin/env python3
"""Inventory and optionally publish exact EZID records for legacy R2/Q2 ARKs.

The script is intentionally dry-run by default. Use --apply with EZID_USER and
EZID_PASS in the environment to create/update exact EZID identifiers.
"""

import argparse
import base64
import csv
import gzip
import json
import os
import re
import sys
import threading
import time
from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from typing import Callable, Dict, Iterable, List, Optional, Sequence, Tuple
from urllib.error import HTTPError, URLError
from urllib.parse import quote, urlencode
from urllib.request import Request, urlopen


DEFAULT_API_BASE = "https://api.geome-db.org"
DEFAULT_EZID_BASE = "https://ezid.cdlib.org"
DEFAULT_OUTPUT_DIR = Path("scripts/output/legacy-r2-ark-repair")
DEFAULT_SOURCE_FIELDS = [
    "catalogNumber",
    "otherCatalogNumbers",
    "occurrenceID",
    "materialSampleID",
    "bcid",
    "projectId",
    "expeditionCode",
]
TISSUE_SOURCE_FIELDS = [
    "tissueCatalogNumber",
    "tissueOtherCatalogNumbers",
    "tissueID",
    "materialSampleID",
    "bcid",
    "projectId",
    "expeditionCode",
]

LEGACY_ARK_RE = re.compile(
    r"(?i)(?:https?://(?:n2t\.net|ezid\.cdlib\.org)(?:/id)?/)?"
    r"ark:/21547/[RQ]2[^\s,\"'<>\\)\]]+"
    r"|https?://arks\.org/ark:21547/[RQ]2[^\s,\"'<>\\)\]]+"
    r"|ark:21547/[RQ]2[^\s,\"'<>\\)\]]+"
)

TRAILING_PUNCTUATION = ".,;:"


class ScriptError(Exception):
    pass


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Extract legacy Moorea Biocode R2 specimen or Q2 tissue ARKs "
            "from current GEOME records and optionally publish exact "
            "EZID targets to current GEOME record pages."
        )
    )
    parser.add_argument("--api-base", default=DEFAULT_API_BASE)
    parser.add_argument("--ezid-base", default=DEFAULT_EZID_BASE)
    parser.add_argument("--project-id", type=int, default=75)
    parser.add_argument("--legacy-prefix", choices=("R2", "Q2"), default="R2")
    parser.add_argument("--entity", help="Defaults to Sample for R2, Tissue for Q2.")
    parser.add_argument("--page-size", type=int, default=500)
    parser.add_argument("--max-pages", type=int)
    parser.add_argument("--max-records", type=int)
    parser.add_argument(
        "--source-fields",
        default=None,
        help="Comma-separated fields to fetch from GEOME. Must include bcid.",
    )
    parser.add_argument("--output-dir", type=Path, help="Defaults to scripts/output/legacy-{r2,q2}-ark-repair.")
    parser.add_argument("--input-mapping", type=Path, help="Reuse a reviewed mapping CSV instead of querying GEOME.")
    parser.add_argument("--only-ark", action="append", help="Process only these exact old ARKs after conflict detection.")
    parser.add_argument("--credentials-file", type=Path, help="Read EZID_USER/EZID_PASS or ezidUser/ezidPass from a local config file.")
    parser.add_argument("--workers", type=int, default=1, choices=range(1, 9), help="Concurrent EZID workers (default: 1; maximum: 8 to limit EZID load).")
    parser.add_argument(
        "--apply",
        action="store_true",
        help="Publish non-conflicting rows to EZID. Dry-run is the default.",
    )
    parser.add_argument(
        "--allow-existing-target-change",
        action="store_true",
        help=(
            "Allow updating an exact EZID record that already has a different "
            "_target. Without this, such rows are logged and skipped."
        ),
    )
    parser.add_argument(
        "--skip-existing-exact",
        action="store_true",
        help=(
            "When applying, skip any old ARK that already has an exact EZID "
            "record, even if it points at the intended target. Use this to "
            "resume after a partial apply without rewriting previous rows."
        ),
    )
    parser.add_argument(
        "--skip-ezid-exact-check",
        action="store_true",
        help="Skip preflight exact EZID GET checks before applying.",
    )
    parser.add_argument(
        "--verify-after",
        action="store_true",
        help="After publishing, verify each old ARK through N2T redirect headers.",
    )
    parser.add_argument(
        "--delay",
        type=float,
        default=0.05,
        help="Seconds to sleep between EZID write calls.",
    )
    parser.add_argument(
        "--timeout",
        type=float,
        default=30.0,
        help="HTTP timeout in seconds.",
    )
    args = parser.parse_args()
    expected_entity = "Tissue" if args.legacy_prefix == "Q2" else "Sample"
    args.entity = args.entity or expected_entity
    if args.entity != expected_entity:
        parser.error(f"--legacy-prefix {args.legacy_prefix} requires --entity {expected_entity}")
    if args.source_fields is None:
        fields = TISSUE_SOURCE_FIELDS if args.entity == "Tissue" else DEFAULT_SOURCE_FIELDS
        args.source_fields = ",".join(fields)
    if args.output_dir is None:
        args.output_dir = DEFAULT_OUTPUT_DIR.with_name(f"legacy-{args.legacy_prefix.lower()}-ark-repair")
    return args


def request_text(
    url: str,
    *,
    method: str = "GET",
    data: Optional[bytes] = None,
    headers: Optional[Dict[str, str]] = None,
    timeout: float = 30.0,
) -> Tuple[int, str, Dict[str, str]]:
    request_headers = {
        "Accept-Encoding": "gzip",
        "User-Agent": "geome-legacy-r2-ark-repair/1.0",
    }
    request_headers.update(headers or {})
    request = Request(url, data=data, method=method, headers=request_headers)
    try:
        with urlopen(request, timeout=timeout) as response:
            body = decode_body(response.read(), response.headers.get("Content-Encoding", ""))
            return response.getcode(), body, dict(response.headers.items())
    except HTTPError as error:
        body = decode_body(error.read(), error.headers.get("Content-Encoding", ""))
        return error.code, body, dict(error.headers.items())
    except URLError as error:
        raise ScriptError(f"HTTP request failed for {url}: {error}") from error


def decode_body(body: bytes, content_encoding: str) -> str:
    if content_encoding.lower() == "gzip" or body.startswith(b"\x1f\x8b"):
        body = gzip.decompress(body)
    return body.decode("utf-8", errors="replace")


def fetch_geome_page(args: argparse.Namespace, page: int) -> List[Dict[str, object]]:
    query = f"_projects_:{args.project_id}"
    params = {
        "q": query,
        "page": str(page),
        "limit": str(args.page_size),
        "source": args.source_fields,
        "includeEmptyProperties": "false",
    }
    url = (
        args.api_base.rstrip("/")
        + f"/records/{quote(args.entity, safe='')}/json?"
        + urlencode(params)
    )
    status, body, _ = request_text(url, timeout=args.timeout)
    if status != 200:
        raise ScriptError(f"GEOME query failed with HTTP {status}: {body[:500]}")

    try:
        payload = json.loads(body)
    except json.JSONDecodeError as error:
        raise ScriptError(f"GEOME query returned non-JSON for page {page}") from error

    content = payload.get("content") or {}
    records = content.get(args.entity) or []
    if not isinstance(records, list):
        raise ScriptError(f"Unexpected GEOME response shape for page {page}")
    return records


def iter_geome_records(args: argparse.Namespace) -> Iterable[Dict[str, object]]:
    seen = 0
    page = 0
    while True:
        if args.max_pages is not None and page >= args.max_pages:
            return

        records = fetch_geome_page(args, page)
        print(f"Fetched page {page}: {len(records)} {args.entity} records", file=sys.stderr)

        for record in records:
            yield record
            seen += 1
            if args.max_records is not None and seen >= args.max_records:
                return

        if len(records) < args.page_size:
            return
        page += 1


def normalize_legacy_ark(value: str) -> str:
    value = value.strip().rstrip(TRAILING_PUNCTUATION)
    lower = value.lower()
    if lower.startswith("http://") or lower.startswith("https://"):
        ark_index = lower.find("ark:")
        if ark_index == -1:
            return value
        value = value[ark_index:]
    if value.lower().startswith("ark:21547/"):
        value = "ark:/21547/" + value[len("ark:21547/") :]
    return value


def extract_legacy_arks(value: object, prefix: str = "R2") -> List[str]:
    if value is None:
        return []
    if isinstance(value, (list, tuple)):
        found: List[str] = []
        for item in value:
            found.extend(extract_legacy_arks(item, prefix))
        return found
    if isinstance(value, dict):
        found = []
        for item in value.values():
            found.extend(extract_legacy_arks(item, prefix))
        return found

    text = str(value)
    arks = [normalize_legacy_ark(match.group(0)) for match in LEGACY_ARK_RE.finditer(text)]
    return [ark for ark in arks if ark.startswith(f"ark:/21547/{prefix}")]


def canonical_bcid(value: object) -> Optional[str]:
    if value is None:
        return None
    text = str(value).strip()
    if not text:
        return None
    lower = text.lower()
    if lower.startswith("http://") or lower.startswith("https://"):
        ark_index = lower.find("ark:")
        if ark_index != -1:
            text = text[ark_index:]
    if text.lower().startswith("ark:21547/"):
        text = "ark:/21547/" + text[len("ark:21547/") :]
    return text


def build_mapping_rows(records: Iterable[Dict[str, object]], entity: str, prefix: str = "R2") -> List[Dict[str, str]]:
    rows: List[Dict[str, str]] = []
    for record in records:
        current_bcid = canonical_bcid(record.get("bcid"))
        if not current_bcid:
            continue

        for field, value in record.items():
            if field == "bcid":
                continue
            for old_ark in extract_legacy_arks(value, prefix):
                if old_ark == current_bcid:
                    continue
                rows.append(
                    {
                        "old_ark": old_ark,
                        "current_bcid": current_bcid,
                        "target_url": f"https://geome-db.org/record/{current_bcid}",
                        "project_id": str(record.get("projectId", "")),
                        "expedition_code": str(record.get("expeditionCode", "")),
                        "material_sample_id": str(record.get("materialSampleID", "")),
                        "tissue_id": str(record.get("tissueID", "")),
                        "entity": entity,
                        "source_field": field,
                        "source_value": str(value),
                    }
                )
    return rows


def split_conflicts(rows: Sequence[Dict[str, str]]) -> Tuple[List[Dict[str, str]], List[Dict[str, str]]]:
    by_old_ark: Dict[str, List[Dict[str, str]]] = defaultdict(list)
    for row in rows:
        by_old_ark[row["old_ark"]].append(row)

    accepted: List[Dict[str, str]] = []
    conflicts: List[Dict[str, str]] = []
    for old_ark, grouped_rows in sorted(by_old_ark.items()):
        target_bcids = {row["current_bcid"] for row in grouped_rows}
        if len(target_bcids) > 1:
            for row in grouped_rows:
                conflict = dict(row)
                conflict["conflict_reason"] = "old_ark_maps_to_multiple_current_bcids"
                conflict["conflicting_bcids"] = "|".join(sorted(target_bcids))
                conflicts.append(conflict)
            continue

        accepted.append(grouped_rows[0])
    return accepted, conflicts


def write_csv(path: Path, rows: Sequence[Dict[str, str]], fieldnames: Sequence[str]) -> None:
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


def ezid_payload(row: Dict[str, str]) -> str:
    values = {
        "_target": row["target_url"],
        "_profile": "dc",
        "_status": "public",
        "_export": "yes",
        "dc.title": "Legacy Moorea Biocode tissue ARK" if row["entity"] == "Tissue" else "Legacy Moorea Biocode specimen ARK",
        "dc.creator": "Biocode Project",
        "dc.publisher": "GeOMe",
        "dc.type": "http://rs.tdwg.org/dwc/terms/MaterialSample",
        "dc.relation": row["current_bcid"],
    }
    return "".join(f"{key}: {escape_anvl(str(value))}\n" for key, value in values.items())


def escape_anvl(value: str) -> str:
    return value.replace("\r", " ").replace("\n", " ").strip()


def write_payload_preview(path: Path, rows: Sequence[Dict[str, str]]) -> None:
    with path.open("w", encoding="utf-8") as handle:
        for row in rows:
            handle.write(f"# {row['old_ark']}\n")
            handle.write(ezid_payload(row))
            handle.write("\n")


def auth_header(user: str, password: str) -> str:
    token = base64.b64encode(f"{user}:{password}".encode("utf-8")).decode("ascii")
    return f"Basic {token}"


def ezid_url(base: str, identifier: str, *, update_if_exists: bool = False) -> str:
    url = base.rstrip("/") + "/id/" + quote(identifier, safe=":/")
    if update_if_exists:
        url += "?update_if_exists=yes"
    return url


def parse_ezid_metadata(body: str) -> Dict[str, str]:
    metadata: Dict[str, str] = {}
    for line in body.splitlines():
        if ":" not in line:
            continue
        key, value = line.split(":", 1)
        metadata[key.strip()] = value.strip()
    return metadata


def ezid_exact_lookup(args: argparse.Namespace, row: Dict[str, str]) -> Tuple[str, str, str]:
    url = ezid_url(args.ezid_base, row["old_ark"])
    status, body, _ = request_text(url, timeout=args.timeout)
    if status == 200 and body.startswith("success:"):
        metadata = parse_ezid_metadata(body)
        return "exists", metadata.get("_target", ""), body
    if "no such identifier" in body.lower():
        return "missing", "", body
    return f"http_{status}", "", body


def put_ezid(args: argparse.Namespace, row: Dict[str, str], auth: str) -> Tuple[int, str]:
    headers = {
        "Authorization": auth,
        "Content-Type": "text/plain; charset=UTF-8",
    }
    body = ezid_payload(row).encode("utf-8")
    status, response_body, _ = request_text(
        ezid_url(args.ezid_base, row["old_ark"], update_if_exists=True),
        method="PUT",
        data=body,
        headers=headers,
        timeout=args.timeout,
    )
    return status, response_body


def verify_n2t(row: Dict[str, str], timeout: float) -> Tuple[str, str]:
    url = "https://n2t.net/" + row["old_ark"]
    # urllib follows redirects here; geturl() records the final landing page.
    request = Request(url, method="HEAD")
    try:
        with urlopen(request, timeout=timeout) as response:
            return str(response.getcode()), response.geturl()
    except HTTPError as error:
        location = error.headers.get("Location", "")
        return str(error.code), location
    except (URLError, TimeoutError) as error:
        return "error", str(error)


def ezid_credentials(args: argparse.Namespace) -> Tuple[str, str]:
    user = os.environ.get("EZID_USER")
    password = os.environ.get("EZID_PASS")
    if args.credentials_file:
        values = {}
        for line in args.credentials_file.read_text().splitlines():
            if line.lstrip().startswith(("#", "!")) or "=" not in line:
                continue
            key, value = line.split("=", 1)
            values[key.strip().removeprefix("export ").lower()] = value.strip().strip("\"'")
        user = user or values.get("ezid_user") or values.get("eziduser")
        password = password or values.get("ezid_pass") or values.get("ezidpass")
    if not user or not password:
        raise ScriptError("Set EZID_USER and EZID_PASS or provide --credentials-file before running with --apply")
    return user, password


def record_verification(result: Dict[str, str], row: Dict[str, str], timeout: float) -> None:
    status, location = verify_n2t(row, timeout)
    result["n2t_status"] = status
    result["n2t_location"] = location
    if not status.startswith("2") or location.rstrip("/") != row["target_url"].rstrip("/"):
        result["action"] += "_verify_mismatch"
        result["message"] = (result["message"] + " verification failed or target mismatch").strip()


def apply_ezid(args: argparse.Namespace, rows: Sequence[Dict[str, str]], on_result: Optional[Callable] = None) -> List[Dict[str, str]]:
    user, password = ezid_credentials(args)
    auth = auth_header(user, password)
    status, body, _ = request_text(args.ezid_base.rstrip("/") + "/login", headers={"Authorization": auth}, timeout=args.timeout)
    if status != 200 or not body.startswith("success:"):
        raise ScriptError(f"EZID authentication failed (HTTP {status}); no identifiers written")
    verification_paused = threading.Event()

    def verify_result(result: Dict[str, str], row: Dict[str, str]) -> None:
        if verification_paused.is_set():
            result["action"] += "_verify_deferred"
            result["n2t_status"] = "deferred"
            result["message"] += "; verification deferred after resolver HTTP 403/429"
            return
        record_verification(result, row, args.timeout)
        if result["n2t_status"] in ("403", "429"):
            # Stop new resolver checks in this run; in-flight requests may finish.
            verification_paused.set()

    def apply_row(row: Dict[str, str]) -> Dict[str, str]:
        result = {
            "old_ark": row["old_ark"],
            "current_bcid": row["current_bcid"],
            "target_url": row["target_url"],
            "action": "pending",
            "http_status": "",
            "ezid_status": "",
            "existing_target": "",
            "message": "",
            "n2t_status": "",
            "n2t_location": "",
        }

        if not args.skip_ezid_exact_check:
            ezid_status, existing_target, lookup_body = ezid_exact_lookup(args, row)
            result["ezid_status"] = ezid_status
            result["existing_target"] = existing_target
            if ezid_status.startswith("http_"):
                result["action"] = "skipped"
                result["message"] = lookup_body[:500]
                return result
            if ezid_status == "exists" and args.skip_existing_exact:
                result["action"] = "skipped_existing_exact"
                result["message"] = "exact EZID record already exists"
                if existing_target != row["target_url"]:
                    result["message"] += "; different _target; review required"
                elif args.verify_after:
                    verify_result(result, row)
                return result
            if (
                ezid_status == "exists"
                and existing_target
                and existing_target != row["target_url"]
                and not args.allow_existing_target_change
            ):
                result["action"] = "skipped"
                result["message"] = "exact EZID record exists with different _target"
                return result

        status, body = put_ezid(args, row, auth)
        result["http_status"] = str(status)
        result["message"] = body[:500]
        result["action"] = "published" if status in (200, 201) and body.startswith("success:") else "failed"

        if args.verify_after and result["action"] == "published":
            verify_result(result, row)

        if args.delay > 0:
            time.sleep(args.delay)
        return result

    def safely_apply(row: Dict[str, str]) -> Dict[str, str]:
        try:
            return apply_row(row)
        except (ScriptError, TimeoutError, OSError) as error:
            # A timed-out PUT can have succeeded. Resume with exact-record checks.
            return {"old_ark": row["old_ark"], "current_bcid": row["current_bcid"],
                    "target_url": row["target_url"], "action": "failed", "message": str(error)}

    results: List[Dict[str, str]] = []
    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        for index, result in enumerate(pool.map(safely_apply, rows), start=1):
            results.append(result)
            if on_result:
                on_result(result)
            print(f"[{index}/{len(rows)}] {result['action']} {result['old_ark']}", file=sys.stderr)
    return results


def read_mapping(args: argparse.Namespace) -> List[Dict[str, str]]:
    with args.input_mapping.open(newline="", encoding="utf-8") as handle:
        rows = list(csv.DictReader(handle))
    for row in rows:
        old_ark = row.get("old_ark", "")
        current = row.get("current_bcid", "")
        if (not old_ark.startswith(f"ark:/21547/{args.legacy_prefix}")
                or not current.startswith("ark:/21547/") or old_ark == current
                or row.get("entity") != args.entity
                or row.get("project_id") != str(args.project_id)
                or row.get("target_url") != f"https://geome-db.org/record/{current}"
                or old_ark not in extract_legacy_arks(row.get("source_value"), args.legacy_prefix)):
            raise ScriptError(f"Mapping CSV contains an invalid or out-of-scope row: {old_ark}")
    return rows


def dry_run_results(rows: Sequence[Dict[str, str]]) -> List[Dict[str, str]]:
    return [
        {
            "old_ark": row["old_ark"],
            "current_bcid": row["current_bcid"],
            "target_url": row["target_url"],
            "action": "dry_run",
            "http_status": "",
            "ezid_status": "",
            "existing_target": "",
            "message": "not published; rerun with --apply",
            "n2t_status": "",
            "n2t_location": "",
        }
        for row in rows
    ]


def main() -> int:
    args = parse_args()
    if args.page_size <= 0:
        raise ScriptError("--page-size must be positive")
    if "bcid" not in {field.strip() for field in args.source_fields.split(",")}:
        raise ScriptError("--source-fields must include bcid")
    if args.skip_existing_exact and args.skip_ezid_exact_check:
        raise ScriptError("--skip-existing-exact requires EZID exact preflight checks")
    if args.skip_existing_exact and args.allow_existing_target_change:
        raise ScriptError("--skip-existing-exact cannot be combined with --allow-existing-target-change")

    args.output_dir.mkdir(parents=True, exist_ok=True)

    rows = read_mapping(args) if args.input_mapping else build_mapping_rows(iter_geome_records(args), args.entity, args.legacy_prefix)
    accepted, conflicts = split_conflicts(rows)
    if args.input_mapping and conflicts:
        raise ScriptError("Reviewed mapping CSV contains conflicting identifiers; refusing to publish")

    mapping_fields = [
        "old_ark",
        "current_bcid",
        "target_url",
        "project_id",
        "expedition_code",
        "material_sample_id",
        "tissue_id",
        "entity",
        "source_field",
        "source_value",
    ]
    conflict_fields = mapping_fields + ["conflict_reason", "conflicting_bcids"]
    result_fields = [
        "old_ark",
        "current_bcid",
        "target_url",
        "action",
        "http_status",
        "ezid_status",
        "existing_target",
        "message",
        "n2t_status",
        "n2t_location",
    ]

    stem = f"legacy_{args.legacy_prefix.lower()}"
    if not args.input_mapping:
        write_csv(args.output_dir / f"{stem}_mapping.csv", accepted, mapping_fields)
        write_csv(args.output_dir / f"{stem}_conflicts.csv", conflicts, conflict_fields)
        write_payload_preview(args.output_dir / f"{stem}_ezid_payloads.anvl", accepted)

    if args.only_ark:
        selected = set(args.only_ark)
        accepted = [row for row in accepted if row["old_ark"] in selected]
        if selected != {row["old_ark"] for row in accepted}:
            raise ScriptError("A requested --only-ark is missing or conflicted; refusing to publish")

    if args.apply:
        # Append and flush every response, so an interrupted run retains its audit.
        with (args.output_dir / f"{stem}_ezid_journal.jsonl").open("a", encoding="utf-8") as journal:
            def checkpoint(result):
                journal.write(json.dumps({"timestamp": time.time(), **result}) + "\n")
                journal.flush()
            results = apply_ezid(args, accepted, on_result=checkpoint)
    else:
        results = dry_run_results(accepted)

    write_csv(args.output_dir / f"{stem}_ezid_results.csv", results, result_fields)

    print(
        f"Done. found_rows={len(rows)} accepted={len(accepted)} "
        f"conflicts={len(conflicts)} output_dir={args.output_dir}",
        file=sys.stderr,
    )
    if not args.apply:
        print("Dry-run only. Set EZID_USER/EZID_PASS and rerun with --apply to publish.", file=sys.stderr)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except ScriptError as error:
        print(f"error: {error}", file=sys.stderr)
        raise SystemExit(1)
