#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${EZID_USER:-}" || -z "${EZID_PASS:-}" ]]; then
  echo "Set EZID_USER and EZID_PASS before running this script." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${REPO_DIR}"
python3 scripts/legacy_r2_ark_repair.py --apply --skip-existing-exact "$@"
