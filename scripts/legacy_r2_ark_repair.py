#!/usr/bin/env python3
"""Inventory and optionally publish exact EZID records for legacy R2 ARKs.

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
import time
from collections import defaultdict
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Tuple
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

R2_ARK_RE = re.compile(
    r"(?i)(?:https?://(?:n2t\.net|ezid\.cdlib\.org)(?:/id)?/)?"
    r"ark:/21547/R2[^\s,\"'<>\\)\]]+"
    r"|https?://arks\.org/ark:21547/R2[^\s,\"'<>\\)\]]+"
    r"|ark:21547/R2[^\s,\"'<>\\)\]]+"
)

TRAILING_PUNCTUATION = ".,;:"


class ScriptError(Exception):
    pass


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Extract legacy Moorea Biocode specimen ARKs under ark:/21547/R2 "
            "from current GEOME Sample records and optionally publish exact "
            "EZID targets to current GEOME record pages."
        )
    )
    parser.add_argument("--api-base", default=DEFAULT_API_BASE)
    parser.add_argument("--ezid-base", default=DEFAULT_EZID_BASE)
    parser.add_argument("--project-id", type=int, default=75)
    parser.add_argument("--entity", default="Sample")
    parser.add_argument("--page-size", type=int, default=500)
    parser.add_argument("--max-pages", type=int)
    parser.add_argument("--max-records", type=int)
    parser.add_argument(
        "--source-fields",
        default=",".join(DEFAULT_SOURCE_FIELDS),
        help="Comma-separated fields to fetch from GEOME. Must include bcid.",
    )
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR)
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
    return parser.parse_args()


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


def normalize_r2_ark(value: str) -> str:
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


def extract_r2_arks(value: object) -> List[str]:
    if value is None:
        return []
    if isinstance(value, (list, tuple)):
        found: List[str] = []
        for item in value:
            found.extend(extract_r2_arks(item))
        return found
    if isinstance(value, dict):
        found = []
        for item in value.values():
            found.extend(extract_r2_arks(item))
        return found

    text = str(value)
    return [normalize_r2_ark(match.group(0)) for match in R2_ARK_RE.finditer(text)]


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


def build_mapping_rows(records: Iterable[Dict[str, object]], entity: str) -> List[Dict[str, str]]:
    rows: List[Dict[str, str]] = []
    for record in records:
        current_bcid = canonical_bcid(record.get("bcid"))
        if not current_bcid:
            continue

        for field, value in record.items():
            if field == "bcid":
                continue
            for old_ark in extract_r2_arks(value):
                if not old_ark.startswith("ark:/21547/R2"):
                    continue
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
        "dc.title": "Legacy Moorea Biocode specimen ARK",
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
    except URLError as error:
        return "error", str(error)


def apply_ezid(args: argparse.Namespace, rows: Sequence[Dict[str, str]]) -> List[Dict[str, str]]:
    user = os.environ.get("EZID_USER")
    password = os.environ.get("EZID_PASS")
    if not user or not password:
        raise ScriptError("Set EZID_USER and EZID_PASS before running with --apply")

    auth = auth_header(user, password)
    results: List[Dict[str, str]] = []
    for index, row in enumerate(rows, start=1):
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
                results.append(result)
                continue
            if ezid_status == "exists" and args.skip_existing_exact:
                result["action"] = "skipped_existing_exact"
                result["message"] = "exact EZID record already exists"
                results.append(result)
                print(
                    f"[{index}/{len(rows)}] {result['action']} {row['old_ark']}",
                    file=sys.stderr,
                )
                continue
            if (
                ezid_status == "exists"
                and existing_target
                and existing_target != row["target_url"]
                and not args.allow_existing_target_change
            ):
                result["action"] = "skipped"
                result["message"] = "exact EZID record exists with different _target"
                results.append(result)
                continue

        status, body = put_ezid(args, row, auth)
        result["http_status"] = str(status)
        result["message"] = body[:500]
        result["action"] = "published" if status in (200, 201) and body.startswith("success:") else "failed"

        if args.verify_after and result["action"] == "published":
            n2t_status, n2t_location = verify_n2t(row, args.timeout)
            result["n2t_status"] = n2t_status
            result["n2t_location"] = n2t_location
            if n2t_location.rstrip("/") != row["target_url"].rstrip("/"):
                result["action"] = "published_verify_mismatch"
                result["message"] = (result["message"] + " verification target mismatch").strip()

        results.append(result)
        print(
            f"[{index}/{len(rows)}] {result['action']} {row['old_ark']} -> {row['target_url']}",
            file=sys.stderr,
        )
        if args.delay > 0:
            time.sleep(args.delay)

    return results


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

    rows = build_mapping_rows(iter_geome_records(args), args.entity)
    accepted, conflicts = split_conflicts(rows)

    mapping_fields = [
        "old_ark",
        "current_bcid",
        "target_url",
        "project_id",
        "expedition_code",
        "material_sample_id",
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

    write_csv(args.output_dir / "legacy_r2_mapping.csv", accepted, mapping_fields)
    write_csv(args.output_dir / "legacy_r2_conflicts.csv", conflicts, conflict_fields)
    write_payload_preview(args.output_dir / "legacy_r2_ezid_payloads.anvl", accepted)

    if args.apply:
        results = apply_ezid(args, accepted)
    else:
        results = dry_run_results(accepted)

    write_csv(args.output_dir / "legacy_r2_ezid_results.csv", results, result_fields)

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
