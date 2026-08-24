# ---- Build stage ----
# maven:3.9-eclipse-temurin-17 (Debian bookworm, maintained) replaces deprecated
# openjdk:17-slim + apt maven. Tesseract NOT needed here (runtime-only dependency).
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw -B dependency:go-offline

COPY src ./src

RUN ./mvnw -B clean package -DskipTests

# ---- Runtime stage ----
# eclipse-temurin:17-jre-noble = Ubuntu 24.04. Deliberate choice: Ubuntu 24.04 ships
# tesseract 5.x whose traineddata lives at /usr/share/tesseract-ocr/5/tessdata/ —
# this MATCHES TESSERACT_DATAPATH in docker-compose.yml (bullseye/openjdk:17-slim
# would give tesseract 4.x at .../4.00/tessdata and break the native tess4j engine).
# NOTE: Temurin publishes no Debian bookworm variant; noble is the closest match.
FROM eclipse-temurin:17-jre-noble

WORKDIR /app

# In-image default; docker-compose.yml overrides with the same value.
ENV TESSERACT_DATAPATH=/usr/share/tesseract-ocr/5/tessdata/

# tesseract + ind language   -> primary pytesseract engine + native tess4j fallback
# python3 + pip             -> runs opt/app/ocr/tesseract_ocr.py bridge
# libglib2.0-0t64           -> cv2 headless still links libgthread (glib); t64 = noble pkg name
# fonts-dejavu-core         -> PIL text rendering for smoke-test images
RUN apt-get update && apt-get install -y --no-install-recommends \
      tesseract-ocr \
      tesseract-ocr-ind \
      python3 \
      python3-pip \
      libglib2.0-0t64 \
      libgl1 \
      fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/* \
    && pip3 install --no-cache-dir --break-system-packages \
      --retries 10 --timeout 180 \
      opencv-python-headless numpy pytesseract onnxruntime pyclipper shapely pyyaml rapidocr_onnxruntime

COPY --from=builder /app/target/*.jar app.jar

# Python OCR bridge scripts (OCR_SCRIPT_PATH=/app/opt/app/ocr/tesseract_ocr.py)
COPY opt /app/opt

# Dev keystore: AES secret for API-key encrypt/decrypt must be stable across
# container rebuilds or previously issued api_keys become undecryptable.
COPY keystore.jks /app/keystore.jks

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
