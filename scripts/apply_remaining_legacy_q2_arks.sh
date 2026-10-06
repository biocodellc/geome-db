#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${REPO_DIR}"
python3 scripts/legacy_r2_ark_repair.py --legacy-prefix Q2 --apply --skip-existing-exact "$@"
