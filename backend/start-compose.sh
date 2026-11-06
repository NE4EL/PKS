#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
[ -f .env ] || python3 init-env.py
docker compose up -d --build --wait
