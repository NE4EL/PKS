#!/usr/bin/env python3
"""Формирует ZIP-архив проекта и проверяет его состав."""
from pathlib import Path
import hashlib
import zipfile

root = Path(__file__).resolve().parents[1]
out = root.parent / "student-hub-suite-rebuilt.zip"
exclude_dirs = {"target", "build", ".gradle", ".idea", ".git"}
exclude_files = {".env", "local.properties"}

files = []
for path in root.rglob("*"):
    if not path.is_file(): continue
    relative = path.relative_to(root)
    if any(part in exclude_dirs for part in relative.parts): continue
    if path.name in exclude_files: continue
    files.append(path)

with zipfile.ZipFile(out, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
    for path in sorted(files):
        archive.write(path, (Path(root.name) / path.relative_to(root)).as_posix())

with zipfile.ZipFile(out) as archive:
    names = set(archive.namelist())
    required = {
        f"{root.name}/backend/pom.xml",
        f"{root.name}/javafx-client/pom.xml",
        f"{root.name}/android-client/app/build.gradle",
        f"{root.name}/backend/docker-compose.yml",
        f"{root.name}/backend/.env.example",
    }
    missing = sorted(required - names)
    if missing: raise SystemExit("ZIP is incomplete: " + ", ".join(missing))
    bad = archive.testzip()
    if bad: raise SystemExit("Corrupt ZIP entry: " + bad)

digest = hashlib.sha256(out.read_bytes()).hexdigest()
sha = out.with_suffix(out.suffix + ".sha256")
sha.write_text(f"{digest}  {out.name}\n", encoding="utf-8")
print(f"Created {out} ({len(files)} files), SHA-256 {digest}")
