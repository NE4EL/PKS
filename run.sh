#!/usr/bin/env sh
# Запуск JavaFX-клиента КР2 (Linux/macOS): ./run.sh
# Нужны: JDK 21, запущенная БД (docker compose up -d --wait)
# и файл src/main/resources/database.properties (см. README).
set -e
cd "$(dirname "$0")"

if [ ! -f src/main/resources/database.properties ]; then
    echo "Нет файла src/main/resources/database.properties."
    echo "Скопируйте database.properties.example в database.properties и укажите параметры БД."
    exit 1
fi

./mvnw -q javafx:run
