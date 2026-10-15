#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p backups
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
file="backups/student_hub_${stamp}.dump"
# Custom format supports selective restore and compression.
docker compose exec -T db pg_dump -U "${POSTGRES_USER:-student_hub_app}" -d "${POSTGRES_DB:-student_hub}" -Fc > "$file"
sha256sum "$file" > "$file.sha256"
printf 'Backup: %s\n' "$file"
