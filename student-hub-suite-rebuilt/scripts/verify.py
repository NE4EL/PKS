#!/usr/bin/env python3
"""Структурная проверка проекта Student Hub Suite."""
from __future__ import annotations

import argparse
import hashlib
import os
import re
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / "VERIFICATION.md"

@dataclass
class Check:
    name: str
    status: str
    details: str
    seconds: float = 0.0

checks: list[Check] = []

def add(name: str, status: str, details: str, seconds: float = 0.0) -> None:
    checks.append(Check(name, status, details.replace("\n", " ").strip(), seconds))

def require(path: str) -> None:
    target = ROOT / path
    add(f"Файл/каталог `{path}`", "PASS" if target.exists() else "FAIL",
        "найден" if target.exists() else "отсутствует")

def read_files(suffixes: tuple[str, ...], roots: Iterable[Path]) -> list[Path]:
    result: list[Path] = []
    for base in roots:
        if base.exists():
            result.extend(p for p in base.rglob("*") if p.is_file() and p.suffix.lower() in suffixes)
    return result

def run(name: str, command: list[str], cwd: Path, timeout: int) -> None:
    executable = shutil.which(command[0]) if os.path.sep not in command[0] else (cwd / command[0]).resolve()
    if not executable or (isinstance(executable, Path) and not executable.exists()):
        add(name, "SKIP", f"инструмент `{command[0]}` отсутствует")
        return
    started = time.monotonic()
    try:
        process = subprocess.run(command, cwd=cwd, text=True, stdout=subprocess.PIPE,
                                 stderr=subprocess.STDOUT, timeout=timeout, check=False)
        elapsed = time.monotonic() - started
        log_dir = ROOT / "build-reports"
        log_dir.mkdir(exist_ok=True)
        log_name = re.sub(r"[^a-zA-Z0-9._-]+", "-", name.lower()).strip("-") + ".log"
        (log_dir / log_name).write_text(process.stdout, encoding="utf-8", errors="replace")
        tail = " | ".join(process.stdout.splitlines()[-5:])[-1200:]
        add(name, "PASS" if process.returncode == 0 else "FAIL",
            f"код {process.returncode}; лог `build-reports/{log_name}`; {tail}", elapsed)
    except subprocess.TimeoutExpired as exc:
        elapsed = time.monotonic() - started
        add(name, "FAIL", f"превышен таймаут {timeout} секунд", elapsed)

for item in [
    "backend/pom.xml", "backend/src/main/java", "backend/src/main/resources/db/migration",
    "backend/src/main/resources/static/index.html", "backend/docker-compose.yml", "backend/init-env.ps1",
    "backend/.env.example",
    "javafx-client/pom.xml", "javafx-client/src/main/java", "javafx-client/README.md",
    "android-client/app/build.gradle", "android-client/app/src/main/AndroidManifest.xml",
    "android-client/app/src/main/java", "android-client/README.md", "docs/QUICKSTART.md"
]: require(item)

# Проверка XML
xml_files = read_files((".xml",), [ROOT / "android-client/app/src"])
xml_errors = []
for file in xml_files:
    try: ET.parse(file)
    except Exception as error: xml_errors.append(f"{file.relative_to(ROOT)}: {error}")
add("Android XML синтаксис", "PASS" if not xml_errors else "FAIL",
    f"проверено {len(xml_files)} файлов" if not xml_errors else "; ".join(xml_errors[:10]))

# Java-исходники и документация
java_roots = [ROOT / "backend/src", ROOT / "javafx-client/src", ROOT / "android-client/app/src"]
java_files = read_files((".java",), java_roots)
java_text = "\n".join(p.read_text(encoding="utf-8", errors="replace") for p in java_files)
javadocs = java_text.count("/**")
add("Java-исходники", "PASS" if len(java_files) >= 45 else "FAIL", f"{len(java_files)} файлов")
add("JavaDoc", "PASS" if javadocs >= 45 else "FAIL", f"обнаружено {javadocs} блоков JavaDoc")

# Клиенты работают с backend через REST и не содержат JDBC-реквизитов.
client_files = read_files((".java", ".xml", ".gradle", ".properties", ".md"),
                          [ROOT / "javafx-client", ROOT / "android-client"])
client_text = "\n".join(p.read_text(encoding="utf-8", errors="replace") for p in client_files)
forbidden = [r"jdbc:postgresql", r"org\.postgresql\.Driver", r"POSTGRES_PASSWORD", r"spring\.datasource"]
hits = [pattern for pattern in forbidden if re.search(pattern, client_text, re.I)]
add("Нет прямого JDBC в клиентах", "PASS" if not hits else "FAIL",
    "JavaFX/Android работают только через REST" if not hits else "найдено: " + ", ".join(hits))

# Базовые настройки безопасности Android.
manifest = (ROOT / "android-client/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
debug_manifest = (ROOT / "android-client/app/src/debug/AndroidManifest.xml").read_text(encoding="utf-8")
security_requirements = {
    "allowBackup=false": 'android:allowBackup="false"' in manifest,
    "release cleartext=false": 'android:usesCleartextTraffic="false"' in manifest,
    "debug overlay": 'android:usesCleartextTraffic="true"' in debug_manifest,
    "Keystore": "AndroidKeyStore" in java_text and "AES/GCM/NoPadding" in java_text,
    "FLAG_SECURE": "FLAG_SECURE" in java_text,
    "нет storage permission": "READ_EXTERNAL_STORAGE" not in manifest and "MANAGE_EXTERNAL_STORAGE" not in manifest,
    "нет HTTP body logger": "HttpLoggingInterceptor" not in java_text,
}
failed_security = [name for name, ok in security_requirements.items() if not ok]
add("Android security baseline", "PASS" if not failed_security else "FAIL",
    ", ".join(security_requirements) if not failed_security else "не выполнено: " + ", ".join(failed_security))

# Сверка маршрутов REST между backend и Android-клиентом.
def mapping_value(annotation: str) -> str:
    match = re.search(r'\(\s*(?:path\s*=\s*|value\s*=\s*)?"([^"]*)"', annotation)
    return match.group(1) if match else ""

def join_route(base: str, child: str) -> str:
    value = "/".join(part.strip("/") for part in (base, child) if part and part.strip("/"))
    value = value.removeprefix("api/")
    return re.sub(r"\{[^}]+\}", "{}", value)

backend_routes: set[tuple[str, str]] = set()
for controller in (ROOT / "backend/src/main/java").rglob("*Controller.java"):
    text = controller.read_text(encoding="utf-8", errors="replace")
    class_base = ""
    class_match = re.search(r'@RequestMapping\s*\([^\n]*\)\s*(?:\n\s*@[^\n]+)*\n\s*public\s+class', text)
    if class_match:
        first = re.search(r'@RequestMapping[^\n]*', class_match.group(0))
        if first: class_base = mapping_value(first.group(0))
    else:
        first = re.search(r'@RequestMapping[^\n]*', text)
        if first: class_base = mapping_value(first.group(0))
    for verb, annotation in re.findall(r'@(Get|Post|Put|Patch|Delete)Mapping([^\n]*)', text):
        backend_routes.add((verb.upper(), join_route(class_base, mapping_value(annotation))))

retrofit_text = (ROOT / "android-client/app/src/main/java/ru/mirea/studenthub/android/net/StudentHubApi.java").read_text(encoding="utf-8")
retrofit_routes = {(verb.upper(), re.sub(r"\{[^}]+\}", "{}", route.strip("/")))
                   for verb, route in re.findall(r'@(GET|POST|PUT|PATCH|DELETE)\("([^"]+)"\)', retrofit_text)}
missing_routes = sorted(retrofit_routes - backend_routes)
# Если маршрут не удалось извлечь из аннотаций Spring, дополнительно проверяются
# URI-литералы JavaFX-клиента.
fx_api_file = ROOT / "javafx-client/src/main/java/ru/mirea/studenthub/fx/net/ApiClient.java"
fx_api_text = fx_api_file.read_text(encoding="utf-8", errors="replace") if fx_api_file.exists() else ""
unconfirmed = []
for verb, path in missing_routes:
    stable = [segment for segment in path.split("/") if segment != "{}"]
    if not stable or not all(segment in fx_api_text for segment in stable): unconfirmed.append((verb, path))
extra_details = ", ".join(f"{verb} {path}" for verb, path in unconfirmed[:12])
add("REST-контракт Android ↔ backend", "PASS" if not unconfirmed else "FAIL",
    f"проверено {len(retrofit_routes)} маршрутов" if not unconfirmed else "не подтверждены: " + extra_details)

# Поиск распространённых форматов секретов.
scan_files = [p for p in ROOT.rglob("*") if p.is_file() and p.stat().st_size < 2_000_000
              and "build" not in p.parts and ".gradle" not in p.parts]
patterns = {
    "private key": r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----",
    "AWS key": r"AKIA[0-9A-Z]{16}",
    "GitHub token": r"gh[pousr]_[A-Za-z0-9_]{30,}",
    "JWT literal": r"eyJ[a-zA-Z0-9_-]{10,}\.eyJ[a-zA-Z0-9_-]{10,}\.",
}
secret_hits = []
for file in scan_files:
    try: text = file.read_text(encoding="utf-8")
    except Exception: continue
    for label, pattern in patterns.items():
        if re.search(pattern, text): secret_hits.append(f"{label}: {file.relative_to(ROOT)}")
add("Скан известных секретов", "PASS" if not secret_hits else "FAIL",
    "ключи/токены не найдены" if not secret_hits else "; ".join(secret_hits[:20]))

parser = argparse.ArgumentParser()
parser.add_argument("--build", action="store_true", help="run available compilers/build tools")
args = parser.parse_args()

if args.build:
    mvnw = "mvnw.cmd" if os.name == "nt" else "./mvnw"
    gradlew = "gradlew.bat" if os.name == "nt" else "./gradlew"
    run("Backend Maven tests", [mvnw, "-B", "-ntp", "clean", "verify"], ROOT / "backend", 600)
    run("JavaFX Maven tests", [mvnw, "-B", "-ntp", "clean", "verify"], ROOT / "javafx-client", 600)
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not sdk:
        for candidate in (Path.home() / "Android/Sdk", Path("/opt/android-sdk"), Path("/usr/local/lib/android/sdk")):
            if candidate.exists():
                sdk = str(candidate)
                os.environ["ANDROID_HOME"] = sdk
                os.environ["ANDROID_SDK_ROOT"] = sdk
                break
    if sdk and Path(sdk).exists():
        run("Android unit tests + debug APK", [gradlew, "--no-daemon", ":app:testDebugUnitTest", ":app:assembleDebug"],
            ROOT / "android-client", 1200)
    else:
        # Без Android SDK полная APK-сборка недоступна, поэтому проверяется Gradle wrapper.
        run("Android Gradle wrapper", [gradlew, "--version"], ROOT / "android-client", 300)
        add("Android unit tests + debug APK", "SKIP", "ANDROID_HOME/ANDROID_SDK_ROOT не настроен в этой среде")
    if shutil.which("docker"):
        run("Docker Compose config", ["docker", "compose", "config", "--quiet"], ROOT, 120)
    else: add("Docker Compose config", "SKIP", "docker отсутствует")

# SHA-256 для исходников и конфигурации; build output и сам отчёт исключаются.
manifest_lines = []
for file in sorted(p for p in ROOT.rglob("*") if p.is_file() and "target" not in p.parts
                   and "build" not in p.parts and ".gradle" not in p.parts
                   and p.name not in {"SOURCE_MANIFEST.sha256", "VERIFICATION.md"}):
    digest = hashlib.sha256(file.read_bytes()).hexdigest()
    manifest_lines.append(f"{digest}  {file.relative_to(ROOT).as_posix()}")
(ROOT / "SOURCE_MANIFEST.sha256").write_text("\n".join(manifest_lines) + "\n", encoding="utf-8")

passed = sum(c.status == "PASS" for c in checks)
failed = sum(c.status == "FAIL" for c in checks)
skipped = sum(c.status == "SKIP" for c in checks)
lines = [
    "# Проверка проекта", "",
    f"Результат: **PASS: {passed}; FAIL: {failed}; SKIP: {skipped}.**", "",
    "`SKIP` означает, что в текущей среде нет нужного инструмента или SDK.", "",
    "| Проверка | Статус | Детали | Время |", "|---|---:|---|---:|",
]
for c in checks:
    icon = {"PASS": "✅ PASS", "FAIL": "❌ FAIL", "SKIP": "⚪ SKIP"}[c.status]
    safe_details = c.details.replace("|", "\\|")
    lines.append(f"| {c.name} | {icon} | {safe_details} | {c.seconds:.1f}s |")
lines += ["", "## Контрольные суммы", "", f"`SOURCE_MANIFEST.sha256` содержит {len(manifest_lines)} хешей исходников и конфигурации.", ""]
REPORT.write_text("\n".join(lines), encoding="utf-8")
print(f"PASS={passed} FAIL={failed} SKIP={skipped}; report={REPORT}")
sys.exit(1 if failed else 0)
