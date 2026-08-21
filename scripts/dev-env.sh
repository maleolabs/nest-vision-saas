#!/usr/bin/env bash
# dev-env.sh — setup development environment for ocr-tool
# Usage: source scripts/dev-env.sh  (must be sourced, not executed)
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# --- JDK 17 ---
if [ -d "$HOME/.local/opt/jdk-17" ] && [ -f "$HOME/.local/opt/jdk-17/bin/java" ]; then
    export JAVA_HOME="$HOME/.local/opt/jdk-17"
elif [ -d "/opt/phpstorm/jbr" ] && [ -f "/opt/phpstorm/jbr/bin/java" ]; then
    export JAVA_HOME="/opt/phpstorm/jbr"
    echo "NOTE: Using JBR 21 at /opt/phpstorm/jbr (Temurin 17 not found at ~/.local/opt/jdk-17)" >&2
fi
export PATH="$JAVA_HOME/bin:$PATH"

# --- Tesseract ---
export TESSERACT_DATAPATH="${TESSERACT_DATAPATH:-$HOME/.local/share/tessdata}"
if [ ! -f "$TESSERACT_DATAPATH/ind.traineddata" ]; then
    echo "WARN: ind.traineddata not found in $TESSERACT_DATAPATH — OCR for Indonesian will fail" >&2
    echo "      Run: curl -sL https://github.com/tesseract-ocr/tessdata_fast/raw/main/ind.traineddata -o \"\$TESSERACT_DATAPATH/ind.traineddata\"" >&2
fi

# --- Database ---
export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:mysql://127.0.0.1:3307/ocr_tool?createDatabaseIfNotExist=true}"
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-ocr}"
export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-ocr}"

# --- OCR ---
export OCR_SCRIPT_PATH="${OCR_SCRIPT_PATH:-$REPO_ROOT/opt/app/ocr/tesseract_ocr.py}"

# --- Keystore ---
export KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-changeit}"
export KEYSTORE_ALIAS="${KEYSTORE_ALIAS:-ocr}"

# --- Mail (dummy for dev) ---
export SPRING_MAIL_USERNAME="${SPRING_MAIL_USERNAME:-dev@example.com}"
export SPRING_MAIL_PASSWORD="${SPRING_MAIL_PASSWORD:-dev}"

# --- MySQL container ---
if command -v docker >/dev/null 2>&1; then
    if ! docker ps --format '{{.Names}}' 2>/dev/null | grep -q '^ocr-tool-mysql$'; then
        if docker ps -a --format '{{.Names}}' 2>/dev/null | grep -q '^ocr-tool-mysql$'; then
            echo "Starting existing ocr-tool-mysql container..."
            docker start ocr-tool-mysql >/dev/null
        else
            echo "Creating ocr-tool-mysql container..."
            docker run -d --name ocr-tool-mysql --restart unless-stopped \
                -e MYSQL_ROOT_PASSWORD=root \
                -e MYSQL_DATABASE=ocr_tool \
                -e MYSQL_USER=ocr \
                -e MYSQL_PASSWORD=ocr \
                -p 127.0.0.1:3307:3306 \
                mysql:8 >/dev/null
        fi
        echo "Waiting for MySQL to be ready..."
        for _i in {1..30}; do
            if docker exec ocr-tool-mysql mysqladmin ping -h localhost -uroot -proot --silent 2>/dev/null; then
                echo "MySQL ready."
                break
            fi
            sleep 1
        done
    else
        echo "MySQL container ocr-tool-mysql already running."
    fi
else
    echo "WARN: docker not found — cannot manage MySQL container" >&2
fi

echo ""
echo "Dev env ready:"
echo "  JAVA_HOME=$JAVA_HOME ($("$JAVA_HOME/bin/java" -version 2>&1 | head -1))"
echo "  TESSERACT_DATAPATH=$TESSERACT_DATAPATH"
echo "  DB: $SPRING_DATASOURCE_URL ($SPRING_DATASOURCE_USERNAME)"
echo "  OCR_SCRIPT_PATH=$OCR_SCRIPT_PATH"
echo "  KEYSTORE: alias=$KEYSTORE_ALIAS"
echo ""
echo "Next: ./mvnw verify  |  ./mvnw spring-boot:run  |  anvil pipeline build  |  anvil pipeline ci"
