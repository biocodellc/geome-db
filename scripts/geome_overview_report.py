#!/usr/bin/env python3
"""
Generate presentation-ready overview tables for GEOME.

The script is intentionally read-only. It queries PostgreSQL through the local
`psql` executable, then writes CSV files and a compact HTML index under
scripts/output/ by default.
"""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import html
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Set, Tuple


try:
    csv.field_size_limit(sys.maxsize)
except OverflowError:
    csv.field_size_limit(2**31 - 1)

SAFE_IDENTIFIER = re.compile(r"^[A-Za-z][A-Za-z0-9_]*$")

METRIC_CANDIDATES: Sequence[Tuple[str, str, Sequence[str]]] = (
    ("events", "Events", ("Event",)),
    ("samples", "Samples", ("Sample",)),
    ("tissues", "Tissues / extractions", ("Tissue", "Extraction")),
    ("event_photos", "Event photos", ("Event_Photo", "EventPhoto")),
    ("sample_photos", "Sample photos", ("Sample_Photo", "SamplePhoto")),
    ("diagnostics", "Diagnostics", ("Diagnostics",)),
    ("fasta", "FASTA", ("fastaSequence", "FastaSequence", "Fasta")),
    ("fastq", "FASTQ", ("fastqMetadata", "FastqMetadata", "Fastq")),
)

CORE_METRICS = ("events", "samples", "tissues", "event_photos", "sample_photos")
FIELD_METRICS = ("events", "samples", "tissues")

PROJECT_METADATA_FIELDS: Sequence[Tuple[str, str, str, str]] = (
    ("project_code", "Project code", "text", "Short project identifier"),
    ("project_title", "Project title", "text", "Project display title"),
    ("description", "Description", "text", "Project description"),
    ("principal_investigator", "Principal investigator", "text", "PI name"),
    ("principal_investigator_affiliation", "PI affiliation", "text", "PI affiliation"),
    ("project_contact", "Project contact", "text", "Project contact name"),
    ("project_contact_email", "Project contact email", "text", "Project contact email present"),
    ("publication_guid", "Publication GUID", "text", "Publication identifier"),
    ("project_data_guid", "Project data GUID", "text", "Dataset identifier"),
    ("recommended_citation", "Recommended citation", "text", "Citation text"),
    ("license", "License", "text", "Project license"),
    ("localcontexts_id", "Local Contexts ID", "text", "Local Contexts Hub project ID"),
    ("permit_guid", "Permit GUID", "text", "Permit identifier"),
    ("public", "Public", "boolean_true", "Projects marked public"),
    ("discoverable", "Discoverable", "boolean_true", "Projects marked discoverable"),
)

FIELD_USAGE_COLUMNS = (
    "rank",
    "entity",
    "field_name",
    "field_uri",
    "filled_count",
    "total_records",
    "coverage_pct",
)

ADDITIONAL_TEAM_FIELD_REPORTS: Sequence[Tuple[str, str, str]] = (
    (
        "smithsonian_bggi_top_fields.csv",
        "Smithsonian BGGI Top Fields",
        "Smithsonian Barcoding and Global Genome Initiative",
    ),
    (
        "amphibiaweb_disease_portal_top_fields.csv",
        "AmphibiaWeb Disease Portal Top Fields",
        "AmphibiaWeb's Disease Portal",
    ),
)

TABLE_DESCRIPTIONS = {
    "overview_metrics.csv": "High-level GEOME counts for projects, records, users, public teams, and Local Contexts adoption.",
    "top_fields.csv": "The top field-data columns across all GEOME projects ranked by percent coverage.",
    "biocode_top_fields.csv": "The top field-data columns in the Biocode team ranked by percent coverage.",
    "smithsonian_bggi_top_fields.csv": "The top field-data columns in the Smithsonian Barcoding and Global Genome Initiative team ranked by percent coverage.",
    "amphibiaweb_disease_portal_top_fields.csv": "The top field-data columns in the AmphibiaWeb Disease Portal team ranked by percent coverage.",
    "project_metadata_coverage.csv": "Project-level metadata fields ranked by how often they are filled across all GEOME projects.",
    "local_contexts_projects.csv": "Public or discoverable GEOME projects that contain a Local Contexts project ID.",
    "local_contexts_record_counts.csv": "Counts and coverage percentages for records covered by project-level Local Contexts IDs.",
    "team_growth_summary.csv": "Public-team record additions in the last complete year as a percent of additions in the previous complete year.",
    "record_accumulation_by_month.csv": "Monthly all-GEOME record additions and cumulative record totals over time.",
}

TABLE_VISUALIZATIONS = {
    "overview_metrics.csv": {"kind": "metric_cards", "label": "metric", "value": "value"},
    "top_fields.csv": {"kind": "bar", "x": "field_name", "y": "coverage_pct", "group": "entity"},
    "biocode_top_fields.csv": {"kind": "bar", "x": "field_name", "y": "coverage_pct", "group": "entity"},
    "smithsonian_bggi_top_fields.csv": {"kind": "bar", "x": "field_name", "y": "coverage_pct", "group": "entity"},
    "amphibiaweb_disease_portal_top_fields.csv": {"kind": "bar", "x": "field_name", "y": "coverage_pct", "group": "entity"},
    "project_metadata_coverage.csv": {"kind": "bar", "x": "field", "y": "coverage_pct"},
    "local_contexts_projects.csv": {"kind": "table"},
    "local_contexts_record_counts.csv": {"kind": "bar", "x": "entity", "y": "coverage_pct"},
    "team_growth_summary.csv": {"kind": "bar", "x": "team_name", "y": "pct_growth_last_year"},
    "record_accumulation_by_month.csv": {"kind": "line", "x": "month", "y": "cumulative_records"},
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate GEOME overview HTML and CSV report tables."
    )
    parser.add_argument(
        "--db-props-file",
        default="src/main/environment/local/biocode-fims-database.properties",
        help="Java properties file containing bcidUrl, bcidUser, and bcidPassword.",
    )
    parser.add_argument(
        "--dsn",
        help="PostgreSQL connection string. Overrides --db-props-file when supplied.",
    )
    parser.add_argument("--network-id", type=int, default=1, help="GEOME network id.")
    parser.add_argument("--team-name", default="Biocode", help="Team/config name to summarize.")
    parser.add_argument("--team-id", type=int, help="Project configuration id to use for team tables.")
    parser.add_argument("--field-limit", type=int, default=50, help="Top field rows to render.")
    parser.add_argument("--months", type=int, default=24, help="Deprecated; ignored by the current yearly team growth summary.")
    parser.add_argument(
        "--out-dir",
        help="Output directory. Defaults to scripts/output/geome-overview-YYYYMMDD.",
    )
    return parser.parse_args()


def default_out_dir() -> Path:
    stamp = dt.date.today().strftime("%Y%m%d")
    return Path("scripts") / "output" / ("geome-overview-" + stamp)


def clean(value: object) -> str:
    if value is None:
        return ""
    text = str(value).strip()
    if text.lower() == "null":
        return ""
    return text


def nonempty(value: object) -> bool:
    return bool(clean(value))


def int_value(value: object, default: int = 0) -> int:
    text = clean(value)
    if not text:
        return default
    try:
        return int(float(text))
    except ValueError:
        return default


def bool_value(value: object) -> bool:
    return clean(value).lower() in ("t", "true", "1", "yes", "y")


def log_step(message: str) -> None:
    print(message, file=sys.stderr, flush=True)


def pct(part: int, total: int) -> str:
    if total <= 0:
        return "0.0"
    return "{:.1f}".format((float(part) / float(total)) * 100.0)


def sql_literal(value: object) -> str:
    return "'" + str(value).replace("'", "''") + "'"


def sql_in(values: Iterable[object]) -> str:
    vals = list(values)
    if not vals:
        return "(NULL)"
    return "(" + ", ".join(sql_literal(v) for v in vals) + ")"


def safe_entity_table(network_id: int, entity: str) -> str:
    if not SAFE_IDENTIFIER.match(entity):
        raise ValueError("Unsafe entity alias: " + entity)
    return "network_{}.{}".format(network_id, entity)


def parse_key_value_file(file_path: Path) -> Dict[str, str]:
    values: Dict[str, str] = {}
    if not file_path.exists():
        return values

    for raw in file_path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        if key:
            values[key] = value
    return values


def parse_jdbc_props(file_path: Path) -> Dict[str, str]:
    values = parse_key_value_file(file_path)
    jdbc_url = clean(values.get("bcidUrl"))
    match = re.match(r"^jdbc:postgresql://([^/:?]+)(?::(\d+))?/([^?]+)", jdbc_url)
    if match:
        values["DB_HOST"] = match.group(1)
        values["DB_PORT"] = match.group(2) or "5432"
        values["DB_NAME"] = match.group(3)
    if values.get("bcidUser"):
        values["DB_USER"] = values["bcidUser"]
    if values.get("bcidPassword"):
        values["DB_PASSWORD"] = values["bcidPassword"]
    return values


class PsqlClient:
    def __init__(self, args: argparse.Namespace):
        self.psql = shutil.which("psql")
        if self.psql is None:
            raise RuntimeError("psql was not found on PATH")

        self.dsn = clean(args.dsn)
        self.props = parse_jdbc_props(Path(args.db_props_file)) if not self.dsn else {}
        self.redactions = [clean(self.props.get("DB_PASSWORD")), self.dsn]

    def query(self, sql: str) -> List[Dict[str, str]]:
        cmd = [
            self.psql,
            "-X",
            "-q",
            "--csv",
            "-P",
            "footer=off",
            "-v",
            "ON_ERROR_STOP=1",
        ]

        env = os.environ.copy()
        env.setdefault("PGCONNECT_TIMEOUT", "20")

        if self.dsn:
            cmd.append(self.dsn)
        else:
            host = clean(self.props.get("DB_HOST"))
            port = clean(self.props.get("DB_PORT"))
            dbname = clean(self.props.get("DB_NAME"))
            user = clean(self.props.get("DB_USER"))
            password = clean(self.props.get("DB_PASSWORD"))

            if host:
                cmd.extend(["-h", host])
            if port:
                cmd.extend(["-p", port])
            if dbname:
                cmd.extend(["-d", dbname])
            if user:
                cmd.extend(["-U", user])
            if password:
                env["PGPASSWORD"] = password

        cmd.extend(["-c", sql])

        proc = subprocess.run(cmd, text=True, capture_output=True, env=env)
        if proc.returncode != 0:
            stderr = self._redact(proc.stderr.strip())
            raise RuntimeError("psql query failed:\n" + stderr)

        stdout = proc.stdout.strip()
        if not stdout:
            return []

        return list(csv.DictReader(stdout.splitlines()))

    def one_int(self, sql: str, column: str = "value") -> int:
        rows = self.query(sql)
        if not rows:
            return 0
        return int_value(rows[0].get(column))

    def _redact(self, text: str) -> str:
        redacted = text
        for secret in self.redactions:
            if secret:
                redacted = redacted.replace(secret, "<redacted>")
        return redacted


def load_network_config(client: PsqlClient, network_id: int) -> Dict[str, object]:
    rows = client.query(
        "SELECT config::text AS config FROM networks WHERE id = {}".format(network_id)
    )
    if not rows:
        raise RuntimeError("Network {} was not found".format(network_id))
    return json.loads(rows[0]["config"])


def load_existing_tables(client: PsqlClient, network_id: int) -> Set[str]:
    rows = client.query(
        """
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = {schema}
          AND table_type = 'BASE TABLE'
        """.format(schema=sql_literal("network_" + str(network_id)))
    )
    return {clean(r.get("table_name")).lower() for r in rows}


def parse_network_fields(
    config: Dict[str, object],
    existing_tables: Set[str],
) -> Tuple[List[str], Dict[str, List[Dict[str, str]]], Dict[Tuple[str, str], Dict[str, str]]]:
    available_entities: List[str] = []
    fields_by_entity: Dict[str, List[Dict[str, str]]] = {}
    labels: Dict[Tuple[str, str], Dict[str, str]] = {}

    entities = config.get("entities") or []
    if not isinstance(entities, list):
        return available_entities, fields_by_entity, labels

    for entity in entities:
        if not isinstance(entity, dict):
            continue
        alias = clean(entity.get("conceptAlias"))
        if not alias or not SAFE_IDENTIFIER.match(alias):
            continue
        if alias.lower() not in existing_tables:
            continue

        available_entities.append(alias)
        fields_by_entity[alias] = []

        attributes = entity.get("attributes") or []
        if not isinstance(attributes, list):
            continue

        for attr in attributes:
            if not isinstance(attr, dict):
                continue
            uri = clean(attr.get("uri"))
            if not uri:
                continue
            meta = {
                "entity": alias,
                "uri": uri,
                "column": clean(attr.get("column")),
                "definition": clean(attr.get("definition")),
                "group": clean(attr.get("group")),
                "defined_by": clean(attr.get("definedBy")),
            }
            fields_by_entity[alias].append(meta)
            labels[(alias.lower(), uri)] = meta

    return available_entities, fields_by_entity, labels


def build_top_field_exclusions(
    config: Dict[str, object],
    available_entities: Sequence[str],
) -> Dict[str, Set[str]]:
    entity_set = {entity.lower() for entity in available_entities}
    entity_defs: Dict[str, Dict[str, object]] = {}

    for entity in config.get("entities") or []:
        if not isinstance(entity, dict):
            continue
        alias = clean(entity.get("conceptAlias"))
        if alias and alias.lower() in entity_set:
            entity_defs[alias.lower()] = entity

    def field_keys(entity: Dict[str, object], key: str) -> Set[str]:
        keys = {key, key.lower()}
        for attr in entity.get("attributes") or []:
            if not isinstance(attr, dict):
                continue
            column = clean(attr.get("column"))
            uri = clean(attr.get("uri"))
            if key and key in (column, uri):
                for value in (column, uri):
                    if value:
                        keys.add(value)
                        keys.add(value.lower())
        return keys

    exclusions: Dict[str, Set[str]] = {}
    for alias_lower, entity in entity_defs.items():
        keys = {"img128", "img512"}

        unique_key = clean(entity.get("uniqueKey"))
        if unique_key:
            keys.update(field_keys(entity, unique_key))

        parent_alias = clean(entity.get("parentEntity")).lower()
        parent = entity_defs.get(parent_alias)
        if parent:
            parent_unique_key = clean(parent.get("uniqueKey"))
            if parent_unique_key:
                keys.update(field_keys(entity, parent_unique_key))

        exclusions[alias_lower] = keys

    return exclusions


def display_field_name(
    labels: Dict[Tuple[str, str], Dict[str, str]],
    entity: str,
    field_uri: str,
) -> str:
    meta = labels.get((entity.lower(), field_uri))
    if meta and meta.get("column"):
        return meta["column"]
    if field_uri.startswith("urn:"):
        return field_uri[4:]
    return field_uri


def resolve_metric_entities(available_entities: Sequence[str]) -> Dict[str, Optional[str]]:
    available_by_lower = {e.lower(): e for e in available_entities}
    resolved: Dict[str, Optional[str]] = {}
    for metric, _display, candidates in METRIC_CANDIDATES:
        match = None
        for candidate in candidates:
            match = available_by_lower.get(candidate.lower())
            if match:
                break
        resolved[metric] = match
    return resolved


def unique_entities(metrics: Dict[str, Optional[str]], metric_keys: Sequence[str]) -> List[str]:
    entities: List[str] = []
    seen: Set[str] = set()
    for metric in metric_keys:
        entity = metrics.get(metric)
        if entity and entity.lower() not in seen:
            entities.append(entity)
            seen.add(entity.lower())
    return entities


def all_metric_entities(metrics: Dict[str, Optional[str]]) -> List[str]:
    return unique_entities(metrics, [m[0] for m in METRIC_CANDIDATES])


def public_team_ids(project_rows: Sequence[Dict[str, str]]) -> Set[int]:
    return {
        int_value(project.get("team_id"))
        for project in project_rows
        if int_value(project.get("team_id")) and bool_value(project.get("public"))
    }


def project_filter(network_id: int, scope: str, team_id: Optional[int] = None) -> str:
    base = "p.network_id = {}".format(network_id)
    if scope == "team":
        if team_id is None:
            raise ValueError("team_id is required for team scope")
        return base + " AND p.config_id = {}".format(team_id)
    return base


def count_entity(
    client: PsqlClient,
    network_id: int,
    entity: Optional[str],
    filter_sql: str,
    extra_condition: Optional[str] = None,
) -> int:
    if not entity:
        return 0
    where = filter_sql
    if extra_condition:
        where += " AND (" + extra_condition + ")"
    sql = """
        SELECT COUNT(*)::bigint AS value
        FROM {table} r
        JOIN expeditions e ON e.id = r.expedition_id
        JOIN projects p ON p.id = e.project_id
        WHERE {where}
    """.format(table=safe_entity_table(network_id, entity), where=where)
    return client.one_int(sql)


def load_field_usage(
    client: PsqlClient,
    network_id: int,
    entities: Sequence[str],
    filter_sql: str,
    labels: Dict[Tuple[str, str], Dict[str, str]],
    excluded_fields_by_entity: Dict[str, Set[str]],
    limit: Optional[int] = None,
) -> List[Dict[str, object]]:
    rows: List[Dict[str, object]] = []

    for entity in entities:
        total_records = count_entity(client, network_id, entity, filter_sql)
        if total_records <= 0:
            continue

        sql = """
            SELECT kv.key AS field_uri, COUNT(*)::bigint AS filled_count
            FROM {table} r
            JOIN expeditions e ON e.id = r.expedition_id
            JOIN projects p ON p.id = e.project_id
            CROSS JOIN LATERAL jsonb_each_text(r.data) kv
            WHERE {where}
              AND kv.value IS NOT NULL
              AND btrim(kv.value) <> ''
            GROUP BY kv.key
            ORDER BY filled_count DESC, kv.key
        """.format(table=safe_entity_table(network_id, entity), where=filter_sql)

        for row in client.query(sql):
            field_uri = clean(row.get("field_uri"))
            excluded = excluded_fields_by_entity.get(entity.lower(), set())
            if field_uri in excluded or field_uri.lower() in excluded:
                continue
            filled = int_value(row.get("filled_count"))
            rows.append(
                {
                    "entity": entity,
                    "field_name": display_field_name(labels, entity, field_uri),
                    "field_uri": field_uri,
                    "filled_count": filled,
                    "total_records": total_records,
                    "coverage_pct": pct(filled, total_records),
                }
            )

    rows.sort(
        key=lambda r: (
            -float(clean(r["coverage_pct"]) or "0"),
            -int_value(r["filled_count"]),
            clean(r["entity"]).lower(),
            clean(r["field_uri"]).lower(),
        )
    )
    if limit is not None:
        rows = rows[:limit]

    ranked: List[Dict[str, object]] = []
    for idx, row in enumerate(rows, 1):
        ranked.append({"rank": idx, **row})
    return ranked


def load_project_rows(client: PsqlClient, network_id: int) -> List[Dict[str, str]]:
    return client.query(
        """
        SELECT
          p.id AS project_id,
          p.project_code,
          p.project_title,
          p.description,
          p.created::text AS created,
          p.modified::text AS modified,
          p.latest_data_modification::text AS latest_data_modification,
          p.public::text AS public,
          p.discoverable::text AS discoverable,
          p.config_id AS team_id,
          COALESCE(to_jsonb(pc)->>'name', '') AS team_name,
          p.principal_investigator,
          p.principal_investigator_affiliation,
          p.project_contact,
          p.project_contact_email,
          p.publication_guid,
          p.project_data_guid,
          p.recommended_citation,
          p.license,
          p.localcontexts_id,
          p.permit_guid
        FROM projects p
        LEFT JOIN project_configurations pc ON pc.id = p.config_id
        WHERE p.network_id = {network_id}
        ORDER BY lower(p.project_title)
        """.format(network_id=network_id)
    )


def resolve_team(
    client: PsqlClient,
    network_id: int,
    team_name: str,
    team_id: Optional[int],
) -> Tuple[Optional[Dict[str, str]], List[str]]:
    warnings: List[str] = []

    if team_id is not None:
        rows = client.query(
            """
            SELECT id AS team_id, COALESCE(to_jsonb(pc)->>'name', '') AS team_name
            FROM project_configurations pc
            WHERE pc.network_id = {network_id}
              AND pc.id = {team_id}
            """.format(network_id=network_id, team_id=team_id)
        )
        if not rows:
            raise RuntimeError("No project configuration found for --team-id {}".format(team_id))
        return rows[0], warnings

    escaped = sql_literal(team_name)
    exact = client.query(
        """
        SELECT id AS team_id, COALESCE(to_jsonb(pc)->>'name', '') AS team_name
        FROM project_configurations pc
        WHERE pc.network_id = {network_id}
          AND lower(COALESCE(to_jsonb(pc)->>'name', '')) = lower({team_name})
        ORDER BY id
        """.format(network_id=network_id, team_name=escaped)
    )
    if len(exact) == 1:
        return exact[0], warnings
    if len(exact) > 1:
        raise RuntimeError(
            "Multiple exact team matches for '{}': {}".format(
                team_name,
                ", ".join("{} ({})".format(r["team_name"], r["team_id"]) for r in exact),
            )
        )

    partial = client.query(
        """
        SELECT id AS team_id, COALESCE(to_jsonb(pc)->>'name', '') AS team_name
        FROM project_configurations pc
        WHERE pc.network_id = {network_id}
          AND lower(COALESCE(to_jsonb(pc)->>'name', '')) LIKE '%' || lower({team_name}) || '%'
        ORDER BY lower(COALESCE(to_jsonb(pc)->>'name', '')), id
        """.format(network_id=network_id, team_name=escaped)
    )
    if len(partial) == 1:
        return partial[0], warnings
    if len(partial) > 1:
        raise RuntimeError(
            "Ambiguous team name '{}'. Use --team-id. Candidates: {}".format(
                team_name,
                ", ".join("{} ({})".format(r["team_name"], r["team_id"]) for r in partial),
            )
        )

    warnings.append(
        "No project configuration matched team name '{}'; the team top-fields table is empty.".format(team_name)
    )
    return None, warnings


def load_team_field_usage(
    client: PsqlClient,
    network_id: int,
    requested_team_name: str,
    requested_team_id: Optional[int],
    entities: Sequence[str],
    labels: Dict[Tuple[str, str], Dict[str, str]],
    excluded_fields_by_entity: Dict[str, Set[str]],
    limit: int,
) -> Tuple[List[Dict[str, object]], str, List[str]]:
    team, warnings = resolve_team(client, network_id, requested_team_name, requested_team_id)
    if not team:
        return [], requested_team_name, warnings

    resolved_team_id = int_value(team.get("team_id"))
    resolved_team_name = clean(team.get("team_name")) or requested_team_name
    log_step("Computing top fields for {}".format(resolved_team_name))
    rows = load_field_usage(
        client,
        network_id,
        entities,
        project_filter(network_id, "team", resolved_team_id),
        labels,
        excluded_fields_by_entity,
        limit,
    )
    return rows, resolved_team_name, warnings


def local_contexts_project_rows(project_rows: Sequence[Dict[str, str]]) -> Tuple[List[Dict[str, object]], Set[int]]:
    rows: List[Dict[str, object]] = []
    lc_project_ids: Set[int] = set()

    for project in project_rows:
        if not nonempty(project.get("localcontexts_id")):
            continue
        if not (bool_value(project.get("public")) or bool_value(project.get("discoverable"))):
            continue

        project_id = int_value(project.get("project_id"))
        if not project_id:
            continue

        lc_project_ids.add(project_id)
        rows.append(
            {
                "project_id": project_id,
                "project_code": project.get("project_code", ""),
                "project_title": project.get("project_title", ""),
                "team_id": int_value(project.get("team_id")),
                "team_name": project.get("team_name", ""),
                "public": project.get("public", ""),
                "discoverable": project.get("discoverable", ""),
                "localcontexts_id": project.get("localcontexts_id", ""),
            }
        )

    rows.sort(key=lambda r: clean(r["project_title"]).lower())
    return rows, lc_project_ids


def local_contexts_record_counts(
    client: PsqlClient,
    network_id: int,
    metric_entities: Dict[str, Optional[str]],
    lc_project_ids: Set[int],
) -> List[Dict[str, object]]:
    rows: List[Dict[str, object]] = []
    project_count = len(lc_project_ids)
    total_covered = 0
    total_records = 0

    for metric, display, _candidates in METRIC_CANDIDATES:
        entity = metric_entities.get(metric)
        if not entity:
            continue

        all_total = count_entity(client, network_id, entity, project_filter(network_id, "all"))
        covered = 0
        if lc_project_ids:
            covered = count_entity(
                client,
                network_id,
                entity,
                "p.network_id = {} AND p.id IN {}".format(network_id, sql_in(sorted(lc_project_ids))),
            )

        total_covered += covered
        total_records += all_total
        rows.append(
            {
                "entity": display,
                "projects_with_local_contexts_id": project_count,
                "records_covered_by_local_contexts_id": covered,
                "coverage_pct": pct(covered, all_total),
            }
        )

    rows.append(
        {
            "entity": "All classes total",
            "projects_with_local_contexts_id": project_count,
            "records_covered_by_local_contexts_id": total_covered,
            "coverage_pct": pct(total_covered, total_records),
        }
    )

    return rows


def project_metadata_coverage(project_rows: Sequence[Dict[str, str]]) -> List[Dict[str, object]]:
    rows: List[Dict[str, object]] = []
    total = len(project_rows)
    for key, display, kind, notes in PROJECT_METADATA_FIELDS:
        if kind == "boolean_true":
            filled = sum(1 for p in project_rows if bool_value(p.get(key)))
        else:
            filled = sum(1 for p in project_rows if nonempty(p.get(key)))
        rows.append(
            {
                "field": display,
                "filled_or_true_count": filled,
                "total_projects": total,
                "coverage_pct": pct(filled, total),
                "notes": notes,
            }
        )
    rows.sort(
        key=lambda row: (
            -float(clean(row["coverage_pct"]) or "0"),
            -int_value(row["filled_or_true_count"]),
            clean(row["field"]).lower(),
        )
    )
    return rows


def union_record_parts(network_id: int, entities: Sequence[str], select_sql: str) -> str:
    parts = []
    for entity in entities:
        parts.append(
            """
            SELECT {select_sql}
            FROM {table} r
            JOIN expeditions e ON e.id = r.expedition_id
            JOIN projects p ON p.id = e.project_id
            WHERE p.network_id = {network_id}
            """.format(
                select_sql=select_sql,
                table=safe_entity_table(network_id, entity),
                network_id=network_id,
            )
        )
    return " UNION ALL ".join(parts)


def distinct_owner_count(client: PsqlClient, network_id: int, entities: Sequence[str]) -> int:
    if not entities:
        return 0
    union_sql = union_record_parts(network_id, entities, "e.user_id")
    return client.one_int(
        """
        WITH record_users AS ({union_sql})
        SELECT COUNT(DISTINCT user_id)::bigint AS value
        FROM record_users
        WHERE user_id IS NOT NULL
        """.format(union_sql=union_sql)
    )


def audit_table_exists(existing_tables: Set[str]) -> bool:
    return "audit_table" in existing_tables


def distinct_audit_user_count(
    client: PsqlClient,
    network_id: int,
    entities: Sequence[str],
    existing_tables: Set[str],
) -> int:
    if not audit_table_exists(existing_tables) or not entities:
        return 0
    table_names = ["network_{}.{}".format(network_id, e.lower()) for e in entities]
    return client.one_int(
        """
        SELECT COUNT(DISTINCT a.user_name)::bigint AS value
        FROM network_{network_id}.audit_table a
        JOIN expeditions e
          ON (a.row_data->>'expedition_id') ~ '^[0-9]+$'
         AND e.id = (a.row_data->>'expedition_id')::int
        JOIN projects p ON p.id = e.project_id
        WHERE p.network_id = {network_id}
          AND a.action = 'I'
          AND a.table_name IN {table_names}
          AND COALESCE(a.user_name, '') <> ''
        """.format(network_id=network_id, table_names=sql_in(table_names))
    )


def load_team_growth_summary(
    client: PsqlClient,
    network_id: int,
    metric_entities: Dict[str, Optional[str]],
) -> List[Dict[str, object]]:
    parts = []
    current_month = dt.date.today().replace(day=1)
    last_year_start = add_months(current_month, -12)
    previous_year_start = add_months(current_month, -24)

    for metric, _display, _candidates in METRIC_CANDIDATES:
        entity = metric_entities.get(metric)
        if not entity:
            continue
        parts.append(
            """
            SELECT
              p.config_id AS team_id,
              COALESCE(to_jsonb(pc)->>'name', '') AS team_name,
              COUNT(*) FILTER (
                WHERE r.created >= {previous_year_start}
                  AND r.created < {last_year_start}
              )::bigint AS records_previous_year,
              COUNT(*) FILTER (
                WHERE r.created >= {last_year_start}
                  AND r.created < {current_month}
              )::bigint AS records_last_year
            FROM {table} r
            JOIN expeditions e ON e.id = r.expedition_id
            JOIN projects p ON p.id = e.project_id
            LEFT JOIN project_configurations pc ON pc.id = p.config_id
            WHERE p.network_id = {network_id}
              AND p.public = TRUE
              AND r.created >= {previous_year_start}
              AND r.created < {current_month}
            GROUP BY 1, 2
            """.format(
                table=safe_entity_table(network_id, entity),
                network_id=network_id,
                previous_year_start=sql_literal(previous_year_start.isoformat()),
                last_year_start=sql_literal(last_year_start.isoformat()),
                current_month=sql_literal(current_month.isoformat()),
            )
        )

    if not parts:
        return []

    rows = client.query(
        """
        SELECT
          team_id,
          team_name,
          SUM(records_previous_year)::bigint AS records_previous_year,
          SUM(records_last_year)::bigint AS records_last_year
        FROM ({union_sql}) growth
        GROUP BY team_id, team_name
        ORDER BY records_last_year DESC, lower(team_name)
        """.format(union_sql=" UNION ALL ".join(parts))
    )

    summaries: List[Dict[str, object]] = []
    for row in rows:
        previous = int_value(row.get("records_previous_year"))
        last = int_value(row.get("records_last_year"))
        if previous == 0 or last == 0:
            continue

        summaries.append(
            {
                "team_id": int_value(row.get("team_id")),
                "team_name": clean(row.get("team_name")),
                "records_previous_year": previous,
                "records_last_year": last,
                "accumulated_records_last_two_years": previous + last,
                "pct_growth_last_year": "{:.1f}".format((float(last) / float(previous)) * 100.0),
            }
        )

    summaries.sort(
        key=lambda r: (
            -float(clean(r["pct_growth_last_year"]) or "0"),
            -int_value(r["records_last_year"]),
            clean(r["team_name"]).lower(),
        )
    )
    return summaries


def load_record_accumulation(
    client: PsqlClient,
    network_id: int,
    metric_entities: Dict[str, Optional[str]],
) -> List[Dict[str, object]]:
    parts = []
    for metric, _display, _candidates in METRIC_CANDIDATES:
        entity = metric_entities.get(metric)
        if not entity:
            continue
        parts.append(
            """
            SELECT
              date_trunc('month', r.created)::date::text AS month,
              COUNT(*)::bigint AS records_created
            FROM {table} r
            WHERE r.created IS NOT NULL
            GROUP BY 1
            """.format(table=safe_entity_table(network_id, entity))
        )

    if not parts:
        return []

    log_step("  Accumulation: all GEOME monthly counts")
    monthly_rows = client.query(
        """
        SELECT
          month,
          SUM(records_created)::bigint AS records_created
        FROM ({union_sql}) monthly
        GROUP BY month
        ORDER BY month
        """.format(union_sql=" UNION ALL ".join(parts))
    )

    output: List[Dict[str, object]] = []
    cumulative = 0
    for row in monthly_rows:
        month = clean(row.get("month"))
        if not month:
            continue
        records_created = int_value(row.get("records_created"))
        cumulative += records_created
        output.append(
            {
                "month": month,
                "records_created": records_created,
                "cumulative_records": cumulative,
            }
        )
    return output


def add_months(month: dt.date, offset: int) -> dt.date:
    index = month.year * 12 + (month.month - 1) + offset
    year = index // 12
    month_num = index % 12 + 1
    return dt.date(year, month_num, 1)


def load_expedition_count(client: PsqlClient, network_id: int, filter_sql: str) -> int:
    return client.one_int(
        """
        SELECT COUNT(*)::bigint AS value
        FROM expeditions e
        JOIN projects p ON p.id = e.project_id
        WHERE {filter_sql}
        """.format(filter_sql=filter_sql)
    )


def overview_metrics(
    client: PsqlClient,
    network_id: int,
    project_rows: Sequence[Dict[str, str]],
    metric_entities: Dict[str, Optional[str]],
    owner_user_count: int,
    audit_user_count: int,
    growth_rows: Sequence[Dict[str, object]],
) -> List[Dict[str, object]]:
    all_projects = len(project_rows)
    public_teams = len(public_team_ids(project_rows))
    local_contexts_projects = sum(
        1
        for p in project_rows
        if nonempty(p.get("localcontexts_id"))
        and (bool_value(p.get("public")) or bool_value(p.get("discoverable")))
    )
    active_growth_teams = len(growth_rows)

    rows: List[Dict[str, object]] = [
        {"metric": "Projects", "value": all_projects, "notes": "All projects in network"},
        {
            "metric": "Projects with Local Contexts ID",
            "value": local_contexts_projects,
            "notes": "Non-empty projects.localcontexts_id and public or discoverable",
        },
        {"metric": "Public teams", "value": public_teams, "notes": "Distinct project configurations with public projects"},
        {
            "metric": "Expeditions",
            "value": load_expedition_count(client, network_id, project_filter(network_id, "all")),
            "notes": "",
        },
    ]

    total_all_records = 0
    for metric, display, _candidates in METRIC_CANDIDATES:
        entity = metric_entities.get(metric)
        if not entity:
            continue
        all_count = count_entity(client, network_id, entity, project_filter(network_id, "all"))
        total_all_records += all_count
        rows.append({"metric": display, "value": all_count, "notes": entity})

    rows.extend(
        [
            {
                "metric": "Record rows",
                "value": total_all_records,
                "notes": "Sum of discovered metric entity tables",
            },
            {
                "metric": "Record-owner users",
                "value": owner_user_count,
                "notes": "Based on current record rows joined to expeditions.user_id",
            },
            {
                "metric": "Audit insert users",
                "value": audit_user_count,
                "notes": "Based on network audit insert rows when available",
            },
            {
                "metric": "Public teams in growth summary",
                "value": active_growth_teams,
                "notes": "Public-project teams with records in both last-year and previous-year windows",
            },
        ]
    )
    return rows


def write_csv(path: Path, rows: Sequence[Dict[str, object]], fieldnames: Sequence[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


def table_id(filename: str) -> str:
    return filename.rsplit(".", 1)[0].replace("-", "_")


def column_label(field: str) -> str:
    labels = {
        "pct_growth_last_year": "Pct Growth Last Year",
        "records_previous_year": "Records Previous Year",
        "records_last_year": "Records Last Year",
        "accumulated_records_last_two_years": "Accumulated Records Last Two Years",
        "coverage_pct": "Coverage Pct",
        "localcontexts_id": "Local Contexts ID",
    }
    if field in labels:
        return labels[field]
    return field.replace("_", " ").title()


def column_type(field: str) -> str:
    if field == "month":
        return "date"
    if field in ("public", "discoverable"):
        return "boolean"
    if field == "coverage_pct" or field.startswith("pct_") or field.endswith("_pct"):
        return "number"
    if field in ("rank", "project_id", "team_id", "value", "filled_count", "total_records", "cumulative_records"):
        return "integer"
    if field.endswith("_count") or field.startswith("records_") or field.startswith("accumulated_records"):
        return "integer"
    return "string"


def json_value(field: str, value: object) -> object:
    text = clean(value)
    if text == "":
        return None

    kind = column_type(field)
    if kind == "boolean":
        return bool_value(text)
    if kind == "integer":
        return int_value(text)
    if kind == "number":
        try:
            return float(text)
        except ValueError:
            return None
    return text


def write_json_report(
    out_dir: Path,
    generated_at: str,
    network_id: int,
    warnings: Sequence[str],
    tables: Sequence[Tuple[str, str, Sequence[Dict[str, object]], Sequence[str], int]],
) -> None:
    table_payloads = []

    for title, filename, rows, fieldnames, _max_rows in tables:
        table_payloads.append(
            {
                "id": table_id(filename),
                "title": title,
                "description": TABLE_DESCRIPTIONS.get(filename, "Presentation-ready GEOME report table."),
                "csv": filename,
                "row_count": len(rows),
                "columns": [
                    {"key": field, "label": column_label(field), "type": column_type(field)}
                    for field in fieldnames
                ],
                "visualization": TABLE_VISUALIZATIONS.get(filename, {"kind": "table"}),
                "rows": [
                    {field: json_value(field, row.get(field)) for field in fieldnames}
                    for row in rows
                ],
            }
        )

    payload = {
        "schema_version": 1,
        "title": "GEOME Overview Report",
        "description": "Presentation-ready GEOME overview data for rendering tables, charts, and slide-oriented summaries.",
        "generated_at": generated_at,
        "network_id": network_id,
        "warnings": list(warnings),
        "outputs": {
            "html": "index.html",
            "json": "report.json",
        },
        "tables": table_payloads,
    }

    (out_dir / "report.json").write_text(
        json.dumps(payload, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def clip(value: object, limit: int = 160) -> str:
    text = clean(value)
    if len(text) <= limit:
        return text
    return text[: limit - 1] + "..."


def html_table(rows: Sequence[Dict[str, object]], fieldnames: Sequence[str], max_rows: int) -> str:
    shown = list(rows[:max_rows])
    if not shown:
        return "<p>No rows.</p>"
    parts = ["<table><thead><tr>"]
    for field in fieldnames:
        parts.append("<th>{}</th>".format(html.escape(field.replace("_", " ").title())))
    parts.append("</tr></thead><tbody>")
    for row in shown:
        parts.append("<tr>")
        for field in fieldnames:
            parts.append("<td>{}</td>".format(html.escape(clip(row.get(field)))))
        parts.append("</tr>")
    parts.append("</tbody></table>")
    if len(rows) > max_rows:
        parts.append("<p class=\"more\">Showing {} of {} rows.</p>".format(max_rows, len(rows)))
    return "".join(parts)


def svg_accumulation_curve(rows: Sequence[Dict[str, object]]) -> str:
    series_order: List[str] = []
    months: List[str] = []
    values_by_series: Dict[str, Dict[str, int]] = {}

    for row in rows:
        series = clean(row.get("series")) or "All GEOME"
        month = clean(row.get("month"))[:10]
        cumulative = int_value(row.get("cumulative_records"))
        if not month:
            continue
        if series not in values_by_series:
            values_by_series[series] = {}
            series_order.append(series)
        values_by_series[series][month] = cumulative
        if month not in months:
            months.append(month)

    months.sort()
    if len(months) < 2 or not series_order:
        return ""

    width = 920
    height = 330
    left = 64
    right = 20
    top = 18
    bottom = 78
    plot_width = width - left - right
    plot_height = height - top - bottom
    max_y = max(max(series_values.values()) for series_values in values_by_series.values())
    max_x = max(len(months) - 1, 1)
    month_index = {month: index for index, month in enumerate(months)}

    def xy(index: int, value: int) -> Tuple[float, float]:
        x = left + (float(index) / float(max_x)) * plot_width
        y = top + plot_height - (float(value) / float(max_y)) * plot_height
        return x, y

    colors = ["#0b65c2", "#2f855a", "#c05621", "#805ad5", "#b83280", "#2c7a7b"]
    line_parts = []
    legend_parts = []
    for idx, series in enumerate(series_order):
        values = values_by_series[series]
        points = [
            (month_index[month], values[month])
            for month in months
            if month in values
        ]
        if len(points) < 2:
            continue
        color = colors[idx % len(colors)]
        polyline = " ".join("{:.1f},{:.1f}".format(*xy(index, value)) for index, value in points)
        last_x, last_y = xy(points[-1][0], points[-1][1])
        line_parts.append(
            '<polyline fill="none" stroke="{color}" stroke-width="{width}" points="{polyline}"/>'
            '<circle cx="{last_x:.1f}" cy="{last_y:.1f}" r="3.5" fill="{color}"/>'.format(
                color=color,
                width=3 if idx == 0 else 2,
                polyline=polyline,
                last_x=last_x,
                last_y=last_y,
            )
        )
        legend_x = left + (idx % 3) * 275
        legend_y = height - 46 + (idx // 3) * 20
        legend_parts.append(
            '<line x1="{x}" y1="{y}" x2="{x2}" y2="{y}" stroke="{color}" stroke-width="3"/>'
            '<text x="{text_x}" y="{text_y}" font-size="12" fill="#1f2933">{label}</text>'.format(
                x=legend_x,
                y=legend_y,
                x2=legend_x + 24,
                color=color,
                text_x=legend_x + 32,
                text_y=legend_y + 4,
                label=html.escape(series),
            )
        )

    first_month = months[0][:7]
    last_month = months[-1][:7]
    all_value = values_by_series.get("All GEOME", {}).get(months[-1], max_y)

    y_ticks = []
    for step in range(0, 5):
        value = int(round(max_y * (step / 4.0)))
        y = top + plot_height - (step / 4.0) * plot_height
        y_ticks.append(
            '<line x1="{left}" y1="{y:.1f}" x2="{right}" y2="{y:.1f}" stroke="#e5e7eb"/>'
            '<text x="{label_x}" y="{text_y:.1f}" font-size="11" text-anchor="end" fill="#52606d">{value:,}</text>'.format(
                left=left,
                right=width - right,
                y=y,
                label_x=left - 8,
                text_y=y + 4,
                value=value,
            )
        )

    return """
    <svg viewBox="0 0 {width} {height}" role="img" aria-label="Cumulative records over time" style="width:100%; max-width:{width}px; height:auto; margin-top:8px;">
      <rect x="0" y="0" width="{width}" height="{height}" fill="#ffffff"/>
      {ticks}
      <line x1="{left}" y1="{bottom_y}" x2="{right_x}" y2="{bottom_y}" stroke="#9fb3c8"/>
      <line x1="{left}" y1="{top}" x2="{left}" y2="{bottom_y}" stroke="#9fb3c8"/>
      {lines}
      {legend}
      <text x="{left}" y="{label_y}" font-size="12" fill="#52606d">{first_month}</text>
      <text x="{right_x}" y="{label_y}" font-size="12" text-anchor="end" fill="#52606d">{last_month}</text>
      <text x="{right_x}" y="{top_label_y}" font-size="12" text-anchor="end" fill="#1f2933">{all_value:,} records</text>
    </svg>
    """.format(
        width=width,
        height=height,
        ticks="\n".join(y_ticks),
        left=left,
        right_x=width - right,
        top=top,
        bottom_y=top + plot_height,
        lines="\n".join(line_parts),
        legend="\n".join(legend_parts),
        label_y=height - 14,
        top_label_y=top + 12,
        first_month=html.escape(first_month),
        last_month=html.escape(last_month),
        all_value=all_value,
    )


def write_html_report(
    out_dir: Path,
    generated_at: str,
    network_id: int,
    warnings: Sequence[str],
    tables: Sequence[Tuple[str, str, Sequence[Dict[str, object]], Sequence[str], int]],
) -> None:
    style = """
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; margin: 32px; color: #1f2933; }
    h1 { margin-bottom: 4px; }
    h2 { margin-top: 32px; border-bottom: 1px solid #d9e2ec; padding-bottom: 6px; }
    .meta, .more { color: #52606d; }
    .warning { background: #fff8e5; border: 1px solid #f7d070; padding: 10px 12px; margin: 8px 0; }
    table { border-collapse: collapse; width: 100%; font-size: 13px; margin-top: 8px; }
    th, td { border: 1px solid #d9e2ec; padding: 6px 8px; text-align: left; vertical-align: top; }
    th { background: #f0f4f8; }
    a { color: #0b65c2; }
    """
    parts = [
        "<!doctype html><html><head><meta charset=\"utf-8\"><title>GEOME Overview Report</title>",
        "<style>{}</style></head><body>".format(style),
        "<h1>GEOME Overview Report</h1>",
        "<p class=\"meta\">Generated {}. Network {}. Counts use all projects in the network.</p>".format(
            html.escape(generated_at), network_id
        ),
        "<p class=\"meta\"><a href=\"report.json\">JSON report</a></p>",
    ]
    for warning in warnings:
        parts.append("<div class=\"warning\">{}</div>".format(html.escape(warning)))

    for title, filename, rows, fieldnames, max_rows in tables:
        parts.append("<h2>{}</h2>".format(html.escape(title)))
        parts.append("<p class=\"meta\"><a href=\"{}\">CSV</a></p>".format(html.escape(filename)))
        if filename == "record_accumulation_by_month.csv":
            parts.append(svg_accumulation_curve(rows))
        parts.append(html_table(rows, fieldnames, max_rows))

    parts.append("</body></html>")
    (out_dir / "index.html").write_text("\n".join(parts), encoding="utf-8")


def main() -> int:
    args = parse_args()
    if args.network_id < 1:
        raise RuntimeError("--network-id must be positive")
    if args.field_limit < 1:
        raise RuntimeError("--field-limit must be positive")
    out_dir = Path(args.out_dir) if args.out_dir else default_out_dir()
    out_dir.mkdir(parents=True, exist_ok=True)

    client = PsqlClient(args)
    generated_at = dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat()
    warnings: List[str] = []

    log_step("Loading network configuration")
    network_config = load_network_config(client, args.network_id)
    existing_tables = load_existing_tables(client, args.network_id)
    available_entities, _fields_by_entity, labels = parse_network_fields(network_config, existing_tables)
    excluded_fields = build_top_field_exclusions(network_config, available_entities)
    metric_entities = resolve_metric_entities(available_entities)
    core_entities = unique_entities(metric_entities, CORE_METRICS)
    field_entities = unique_entities(metric_entities, FIELD_METRICS)
    metric_entity_list = all_metric_entities(metric_entities)

    if not core_entities:
        raise RuntimeError("No core Event/Sample/Tissue/Photo entity tables were found for network {}".format(args.network_id))

    log_step("Loading projects and resolving team")
    project_rows = load_project_rows(client, args.network_id)

    log_step("Computing top fields across all projects")
    top_fields = load_field_usage(
        client,
        args.network_id,
        field_entities,
        project_filter(args.network_id, "all"),
        labels,
        excluded_fields,
        args.field_limit,
    )

    biocode_top_fields, _biocode_resolved_name, team_warnings = load_team_field_usage(
        client,
        args.network_id,
        args.team_name,
        args.team_id,
        field_entities,
        labels,
        excluded_fields,
        args.field_limit,
    )
    warnings.extend(team_warnings)

    additional_team_field_tables = []
    for filename, title, requested_team_name in ADDITIONAL_TEAM_FIELD_REPORTS:
        rows, _resolved_team_name, team_warnings = load_team_field_usage(
            client,
            args.network_id,
            requested_team_name,
            None,
            field_entities,
            labels,
            excluded_fields,
            args.field_limit,
        )
        warnings.extend(team_warnings)
        additional_team_field_tables.append(
            (filename, rows, FIELD_USAGE_COLUMNS, title, args.field_limit)
        )

    log_step("Computing Local Contexts coverage")
    lc_projects, lc_project_ids = local_contexts_project_rows(project_rows)
    lc_record_counts = local_contexts_record_counts(
        client,
        args.network_id,
        metric_entities,
        lc_project_ids,
    )

    log_step("Computing project metadata coverage")
    metadata_rows = project_metadata_coverage(project_rows)
    log_step("Computing user participation counts")
    owner_user_count = distinct_owner_count(client, args.network_id, metric_entity_list)
    audit_user_count = distinct_audit_user_count(client, args.network_id, metric_entity_list, existing_tables)
    log_step("Computing public team growth")
    growth_rows = load_team_growth_summary(client, args.network_id, metric_entities)
    log_step("Computing record accumulation curves")
    accumulation_rows = load_record_accumulation(client, args.network_id, metric_entities)
    log_step("Computing overview metrics")
    overview_rows = overview_metrics(
        client,
        args.network_id,
        project_rows,
        metric_entities,
        owner_user_count,
        audit_user_count,
        growth_rows,
    )

    files = [
        (
            "overview_metrics.csv",
            overview_rows,
            ("metric", "value", "notes"),
            "Overview Metrics",
            80,
        ),
        (
            "top_fields.csv",
            top_fields,
            FIELD_USAGE_COLUMNS,
            "Top Fields In All Projects",
            args.field_limit,
        ),
        (
            "biocode_top_fields.csv",
            biocode_top_fields,
            FIELD_USAGE_COLUMNS,
            "Biocode Top Fields",
            args.field_limit,
        ),
        *additional_team_field_tables,
        (
            "project_metadata_coverage.csv",
            metadata_rows,
            ("field", "filled_or_true_count", "total_projects", "coverage_pct", "notes"),
            "Project Metadata Coverage",
            40,
        ),
        (
            "local_contexts_projects.csv",
            lc_projects,
            (
                "project_id",
                "project_code",
                "project_title",
                "team_id",
                "team_name",
                "public",
                "discoverable",
                "localcontexts_id",
            ),
            "Local Contexts Projects",
            30,
        ),
        (
            "local_contexts_record_counts.csv",
            lc_record_counts,
            (
                "entity",
                "projects_with_local_contexts_id",
                "records_covered_by_local_contexts_id",
                "coverage_pct",
            ),
            "Local Contexts Record Counts",
            20,
        ),
        (
            "team_growth_summary.csv",
            growth_rows,
            (
                "team_id",
                "team_name",
                "records_previous_year",
                "records_last_year",
                "accumulated_records_last_two_years",
                "pct_growth_last_year",
            ),
            "Team Growth Summary",
            30,
        ),
        (
            "record_accumulation_by_month.csv",
            accumulation_rows,
            ("month", "records_created", "cumulative_records"),
            "Record Accumulation Over Time",
            36,
        ),
    ]

    for obsolete in ("published_top_fields.csv", "local_contexts_fields.csv", "team_activity_by_month.csv", "users_overview.csv"):
        obsolete_path = out_dir / obsolete
        if obsolete_path.exists():
            obsolete_path.unlink()

    for filename, rows, fieldnames, _title, _max_rows in files:
        write_csv(out_dir / filename, rows, fieldnames)

    report_tables = [
        (title, filename, rows, fieldnames, max_rows)
        for filename, rows, fieldnames, title, max_rows in files
    ]

    write_json_report(
        out_dir,
        generated_at,
        args.network_id,
        warnings,
        report_tables,
    )

    write_html_report(
        out_dir,
        generated_at,
        args.network_id,
        warnings,
        report_tables,
    )

    print("Wrote GEOME overview report to {}".format(out_dir.resolve()))
    print("Open {}".format((out_dir / "index.html").resolve()))
    if warnings:
        print("Warnings:")
        for warning in warnings:
            print("- " + warning)
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("Interrupted", file=sys.stderr)
        sys.exit(130)
    except Exception as exc:
        print("error: {}".format(exc), file=sys.stderr)
        sys.exit(1)
