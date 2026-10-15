#!/usr/bin/env bash
set -euo pipefail
if [[ $# -ne 1 ]]; then echo "Usage: $0 backups/file.dump" >&2; exit 2; fi
cd "$(dirname "$0")/.."
file="$1"
[[ -f "$file" ]] || { echo "File not found: $file" >&2; exit 2; }
if [[ -f "$file.sha256" ]]; then sha256sum -c "$file.sha256"; fi
read -r -p "Restore replaces database objects. Type RESTORE: " answer
[[ "$answer" == "RESTORE" ]] || { echo "Cancelled"; exit 1; }
docker compose exec -T db pg_restore -U "${POSTGRES_USER:-student_hub_app}" -d "${POSTGRES_DB:-student_hub}" --clean --if-exists < "$file"
