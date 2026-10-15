#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import re
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main"
JAVA = MAIN / "java"
RES = MAIN / "res"
checks: list[tuple[str, bool, str]] = []


def check(name: str, ok: bool, details: str = "") -> None:
    checks.append((name, bool(ok), details))


# XML parse
xml_errors = []
for path in list(MAIN.rglob("*.xml")) + list((ROOT / "app/src/debug").rglob("*.xml")):
    try:
        ET.parse(path)
    except Exception as exc:
        xml_errors.append(f"{path.relative_to(ROOT)}: {exc}")
check("Все Android XML корректно разбираются", not xml_errors, "; ".join(xml_errors))

# Activity, указанные в AndroidManifest
manifest = (MAIN / "AndroidManifest.xml").read_text(encoding="utf-8")
activities = re.findall(r'<activity\s+android:name="([^"]+)"', manifest)
base_package = JAVA / "ru/mirea/studenthub/android"
missing_activities = []
for activity in activities:
    if not activity.startswith("."):
        continue
    cls = base_package.joinpath(*activity[1:].split(".")).with_suffix(".java")
    if not cls.exists():
        missing_activities.append(activity)
check("Все Activity из AndroidManifest существуют", not missing_activities, ", ".join(missing_activities))

# Проверка exported-компонентов
exported_true = re.findall(r'<activity\s+android:name="([^"]+)"\s+android:exported="true"', manifest)
check("Наружу экспортирована только LoginActivity", exported_true == [".ui.LoginActivity"], str(exported_true))

# Security manifest
check("Backup отключён", 'android:allowBackup="false"' in manifest and 'android:fullBackupContent="false"' in manifest)
check("Release cleartext HTTP отключён", 'android:usesCleartextTraffic="false"' in manifest)
permissions = re.findall(r'<uses-permission\s+android:name="([^"]+)"', manifest)
check("Нет broad storage permissions", all("STORAGE" not in p and "MANAGE_EXTERNAL_STORAGE" not in p for p in permissions), str(permissions))

# Ограничения сети в debug-сборке
network = (ROOT / "app/src/debug/res/xml/network_security_config.xml").read_text(encoding="utf-8")
check("Debug HTTP ограничен локальными адресами", all(x in network for x in ["10.0.2.2", "127.0.0.1", "localhost", 'cleartextTrafficPermitted="false"']))

# Model + API compatibility
models = (base_package / "model/ApiModels.java").read_text(encoding="utf-8")
api = (base_package / "net/StudentHubApi.java").read_text(encoding="utf-8")
check("RiskLevel содержит NOT_APPLICABLE", "NOT_APPLICABLE" in models)
check("Есть endpoint первичной смены пароля", 'auth/change-initial-password' in api)
check("Есть полный student CRUD + restore + permanent delete", all(x in api for x in ['students/{id}', 'students/{id}/restore', 'students/{id}/permanent']))
check("Есть notes/groups/users/audit/analytics/CSV", all(x in api for x in ['/{studentId}/notes', 'groups', 'admin/users', 'admin/audit', 'analytics/summary', 'students/import/preview', 'students/import/apply', 'students/export']))

student_edit = (base_package / "ui/StudentEditActivity.java").read_text(encoding="utf-8")
check("Android использует 100-балльную систему", '100' in student_edit and 'grade.compareTo(new BigDecimal("100"))' in student_edit)
check("Телефон проверяется форматом +7-999-999-99-99", 'PhoneFormatter.isComplete' in student_edit)

ui_text = (base_package / "util/UiText.java").read_text(encoding="utf-8")
check("Роли локализованы на русский", all(x in ui_text for x in ['Администратор', 'Преподаватель', 'Наблюдатель']))
check("Риск NOT_APPLICABLE отображается как 'Не применяется'", 'NOT_APPLICABLE -> "Не применяется"' in ui_text)

# Проверки авторизации
auth_interceptor = (base_package / "net/AuthInterceptor.java").read_text(encoding="utf-8")
refresh_auth = (base_package / "net/RefreshAuthenticator.java").read_text(encoding="utf-8")
check("Bearer добавляется к защищённым /auth endpoints", all(x in auth_interceptor for x in ['PUBLIC_AUTH_PATHS', '/api/auth/login', '/api/auth/refresh', '/api/auth/logout', 'Authorization', 'Bearer ']) and '/api/auth/change-password' not in re.search(r'PUBLIC_AUTH_PATHS[\s\S]*?\)\);', auth_interceptor).group(0))
check("Refresh retry ограничен", 'responseCount(response) >= 2' in refresh_auth or 'responseCount(response) >=' in refresh_auth)

session = (base_package / "security/SessionManager.java").read_text(encoding="utf-8")
store = (base_package / "security/EncryptedTokenStore.java").read_text(encoding="utf-8")
check("Access JWT хранится только в SessionManager memory", 'accessToken' in session and 'EncryptedTokenStore' in session)
check("Refresh token использует Android Keystore AES/GCM", all(x in store for x in ['AndroidKeyStore', 'AES/GCM/NoPadding', 'KeyGenParameterSpec']))

# No sensitive logging / DB access
all_java = "\n".join(p.read_text(encoding="utf-8") for p in JAVA.rglob("*.java"))
check("Нет JDBC/PostgreSQL credentials в Android", not re.search(r'jdbc:postgresql|POSTGRES_PASSWORD|spring\.datasource', all_java, re.I))
check("Нет HTTP BODY logger", 'HttpLoggingInterceptor' not in all_java)

# R references
ids = set()
layouts = {p.stem for p in (RES / "layout").glob("*.xml")}
menus = {p.stem for p in (RES / "menu").glob("*.xml")}
drawables = {p.stem for p in (RES / "drawable").glob("*.xml")}
for p in list((RES / "layout").glob("*.xml")) + list((RES / "menu").glob("*.xml")):
    ids.update(re.findall(r'@\+id/([A-Za-z0-9_]+)', p.read_text(encoding="utf-8")))
missing_refs = []
for p in JAVA.rglob("*.java"):
    text = p.read_text(encoding="utf-8")
    # Ignore android.R.* references.
    text = re.sub(r'android\.R\.(id|layout|menu|drawable)\.[A-Za-z0-9_]+', '', text)
    for kind, name in re.findall(r'(?<!android\.)R\.(id|layout|menu|drawable)\.([A-Za-z0-9_]+)', text):
        bucket = {"id": ids, "layout": layouts, "menu": menus, "drawable": drawables}[kind]
        if name not in bucket:
            missing_refs.append(f"{p.relative_to(ROOT)} -> R.{kind}.{name}")
check("Java R.* ссылки соответствуют ресурсам", not missing_refs, "; ".join(missing_refs[:20]))

# No unfinished placeholders
unfinished = []
for p in ROOT.rglob("*"):
    if p.is_file() and p.suffix in {".java", ".xml", ".gradle"}:
        text = p.read_text(encoding="utf-8", errors="ignore")
        if re.search(r'\bTODO\b|\bFIXME\b|UnsupportedOperationException', text):
            unfinished.append(str(p.relative_to(ROOT)))
check("Нет TODO/FIXME/заглушек", not unfinished, ", ".join(unfinished))

# Compatibility APIs
legacy_hits = []
for p in JAVA.rglob("*.java"):
    text = p.read_text(encoding="utf-8")
    if '.isBlank()' in text or 'Set.of(' in text or re.search(r'(?<!Collectors)\.toList\(\)', text):
        legacy_hits.append(str(p.relative_to(ROOT)))
check("Нет случайной зависимости от неподдержанных Java collection/string APIs", not legacy_hits, ", ".join(legacy_hits))

# Gradle bootstrap
check("Есть Windows Gradle bootstrap", (ROOT / "gradlew.bat").exists() and (ROOT / "scripts/gradle-bootstrap.ps1").exists())
check("Есть Gradle 8.13 wrapper metadata", 'gradle-8.13-bin.zip' in (ROOT / "gradle/wrapper/gradle-wrapper.properties").read_text(encoding="utf-8"))

# Компиляция Java-классов с уровнем языка Java 17
with tempfile.TemporaryDirectory(prefix="studenthub-pure-") as temp:
    files = [
        base_package / "model/ApiModels.java",
        base_package / "util/PhoneFormatter.java",
        base_package / "util/PasswordValidator.java",
        base_package / "util/UiText.java",
    ]
    proc = subprocess.run(
        ["javac", "--release", "17", "-d", temp, *map(str, files)],
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )
    check("Чистая Java-модель/util компилируется с --release 17", proc.returncode == 0, proc.stdout[-1500:])

passed = sum(1 for _, ok, _ in checks if ok)
failed = len(checks) - passed
print(f"PASS={passed} FAIL={failed} TOTAL={len(checks)}")
for name, ok, details in checks:
    print(f"[{'PASS' if ok else 'FAIL'}] {name}" + (f" :: {details}" if details and not ok else ""))

sys.exit(1 if failed else 0)
