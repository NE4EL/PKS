#!/usr/bin/env bash
set -euo pipefail

api="${STUDENT_HUB_API:-http://127.0.0.1:8080/api}"

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven 3.9+ is required (or run javafx:run from your IDE)." >&2
  exit 1
fi

echo "Student Hub JavaFX"
echo "REST API: $api"

exec mvn -Dstudenthub.api="$api" javafx:run
