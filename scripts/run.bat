@echo off
REM run.bat — double-click untuk jalankan ocr-tool di Windows
REM Edit bagian CONFIG di bawah sesuai environment kamu, lalu double-click file ini.
REM Log Spring Boot akan tampil di window ini. Jangan close window selama app running.
REM Tekan Ctrl+C untuk stop.

setlocal EnableDelayedExpansion

REM ============================================================
REM CONFIG — edit sesuai kebutuhan (user friendly)
REM Kosongkan (=) untuk pakai default dari application.properties
REM ============================================================
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
REM Jika JAVA_HOME di atas tidak ada, script akan coba cari java di PATH

set "SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/ocr_tool?createDatabaseIfNotExist=true"
set "SPRING_DATASOURCE_USERNAME=ocr"
set "SPRING_DATASOURCE_PASSWORD=ocr"

set "SPRING_JPA_HIBERNATE_DDL_AUTO=update"

set "TESSERACT_DATAPATH=C:\tessdata"
REM Isi C:\tessdata dengan ind.traineddata + eng.traineddata
REM Download: https://github.com/tesseract-ocr/tessdata_fast/raw/main/ind.traineddata

set "TESSERACT_CMD="
REM Kosongkan untuk auto-detect. Atau isi manual jika tesseract.exe tidak di PATH:
REM set "TESSERACT_CMD=C:\Program Files\Tesseract-OCR\tesseract.exe"

REM --- Python OCR bridge ---
set "OCR_PYTHON_PATH=python"
REM windows: python/py; linux: python3
set "OCR_SCRIPT_PATH=opt/app/ocr/tesseract_ocr.py"

REM --- Preprocessing (blur/gelap/kontras rendah) ---
set "OCR_PREPROCESSING_ENABLED=true"
set "OCR_UPSCALE_THRESHOLD=1000"
set "OCR_BLUR_THRESHOLD=100"

REM --- Ensemble fallback (pyTesseract -> native Tesseract -> PaddleOCR) ---
set "OCR_ENSEMBLE_FALLBACK=true"
set "OCR_CONF_THRESHOLD=60"

REM --- PaddleOCR fallback (butuh pip install paddlepaddle paddleocr) ---
set "OCR_PADDLE_ENABLED=false"
set "OCR_PADDLE_SCRIPT=opt/app/ocr/paddle_ocr.py"

REM --- Super-resolution (blur ekstrem) ---
set "OCR_SR_ENABLED=false"
set "ESRGAN_MODEL="

REM --- Koreksi miring / perspective / orientasi ---
set "OCR_PERSPECTIVE_ENABLED=true"
set "OCR_OSD_ENABLED=true"
set "OCR_DESKEW_MAX_ANGLE=45"

REM --- LLM Vision fallback (opsional, berbayar) ---
set "OCR_LLM_ENABLED=false"
set "OCR_LLM_API_KEY="
set "OCR_LLM_BASE_URL=https://api.openai.com/v1"
set "OCR_LLM_MODEL=gpt-4o-mini"

set "KEYSTORE_PASSWORD=changeit"
set "KEYSTORE_ALIAS=ocr"

set "SPRING_MAIL_HOST=smtp.gmail.com"
set "SPRING_MAIL_PORT=587"
set "SPRING_MAIL_USERNAME=dev@example.com"
set "SPRING_MAIL_PASSWORD=dev"

set "SERVER_PORT=8080"
REM ============================================================
REM END CONFIG — jangan edit di bawah ini kecuali perlu
REM ============================================================

REM --- Set working dir ke repo root (parent dari scripts) ---
pushd "%~dp0.."

REM --- Load .env jika ada (override CONFIG) ---
if exist ".env" (
    echo  [INFO] Load .env dari %CD%\.env
    for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do (
        if not "%%a"=="" (
            for /f "tokens=*" %%c in ("%%a") do for /f "tokens=*" %%d in ("%%b") do set "%%c=%%d"
        )
    )
    REM Strip quotes jika value pakai " atau ' (mis: KEY="value")
    for %%k in (SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD TESSERACT_DATAPATH TESSERACT_CMD OCR_PYTHON_PATH OCR_SCRIPT_PATH OCR_PREPROCESSING_ENABLED OCR_UPSCALE_THRESHOLD OCR_BLUR_THRESHOLD OCR_ENSEMBLE_FALLBACK OCR_CONF_THRESHOLD OCR_PADDLE_ENABLED OCR_PADDLE_SCRIPT OCR_SR_ENABLED ESRGAN_MODEL OCR_PERSPECTIVE_ENABLED OCR_OSD_ENABLED OCR_DESKEW_MAX_ANGLE OCR_LLM_ENABLED OCR_LLM_API_KEY OCR_LLM_BASE_URL OCR_LLM_MODEL KEYSTORE_PASSWORD KEYSTORE_ALIAS SPRING_MAIL_HOST SPRING_MAIL_USERNAME SPRING_MAIL_PASSWORD) do (
        if defined %%k (
            set "%%k=!%%k:"=!"
            set "%%k=!%%k:'=!"
        )
    )
)

title OCR Tool - Spring Boot

echo.
echo  ========================================
echo   OCR Tool - Starting...
echo  ========================================
echo  Repo: %CD%
echo.

REM --- Cari JAVA ---
if exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
    echo  [OK] JAVA_HOME: %JAVA_HOME%
    "%JAVA_EXE%" -version 2>&1 | findstr /i "version"
) else (
    where java >nul 2>&1
    if !errorlevel! equ 0 (
        set "JAVA_EXE=java"
        echo  [INFO] JAVA_HOME tidak ditemukan, pakai java di PATH
        java -version 2>&1 | findstr /i "version"
    ) else (
        echo  [ERROR] Java tidak ditemukan!
        echo          Install JDK 17: https://adoptium.net/temurin/releases/?version=17
        echo          Atau set JAVA_HOME di bagian CONFIG atas file ini.
        echo.
        pause
        exit /b 1
    )
)
echo.

REM --- Cari JAR ---
set "JAR="
for %%f in ("target\*.jar") do set "JAR=%%f"
if not defined JAR (
    for %%f in (".\*.jar") do set "JAR=%%f"
)
if not defined JAR (
    for %%f in ("%~dp0..\target\*.jar") do set "JAR=%%f"
)
if not defined JAR (
    echo  [ERROR] File *.jar tidak ditemukan!
    echo          Jalankan dulu: mvnw.cmd clean package -DskipTests
    echo          Atau download dari GitHub Releases.
    echo.
    pause
    exit /b 1
)
echo  [OK] JAR: %JAR%
echo  [OK] DB: %SPRING_DATASOURCE_URL% (%SPRING_DATASOURCE_USERNAME%)
echo  [OK] TESSERACT: %TESSERACT_DATAPATH%
if defined TESSERACT_CMD echo  [OK] TESSERACT_CMD: %TESSERACT_CMD%
echo  [OK] PORT: %SERVER_PORT%
echo.

REM --- Cek tesseract binary ---
where tesseract >nul 2>&1
if %errorlevel% neq 0 (
    if not defined TESSERACT_CMD (
        echo  [WARN] tesseract.exe tidak ditemukan di PATH!
        echo         Install: https://github.com/UB-Mannheim/tesseract/wiki
        echo         Atau: choco install tesseract  /  scoop install tesseract
        echo         Atau set TESSERACT_CMD di CONFIG atas.
        echo         OCR via Python akan gagal sampai tesseract terinstall.
        echo.
    ) else (
        if not exist "%TESSERACT_CMD%" (
            echo  [WARN] TESSERACT_CMD tidak ditemukan: %TESSERACT_CMD%
            echo.
        ) else (
            echo  [OK] tesseract binary: %TESSERACT_CMD%
            echo.
        )
    )
) else (
    echo  [OK] tesseract binary di PATH
    tesseract --version 2>&1 | findstr /i "tesseract"
    echo.
)

REM --- Cek MySQL (opsional, warning saja) ---
echo  [INFO] Pastikan MySQL running di %SPRING_DATASOURCE_URL%
echo         Jika DB belum ada, app akan auto-create (createDatabaseIfNotExist=true)
echo.

REM --- Set env untuk Spring Boot (hanya untuk proses ini) ---
set "SPRING_DATASOURCE_URL=%SPRING_DATASOURCE_URL%"
set "SPRING_DATASOURCE_USERNAME=%SPRING_DATASOURCE_USERNAME%"
set "SPRING_DATASOURCE_PASSWORD=%SPRING_DATASOURCE_PASSWORD%"
set "SPRING_JPA_HIBERNATE_DDL_AUTO=%SPRING_JPA_HIBERNATE_DDL_AUTO%"
set "TESSERACT_DATAPATH=%TESSERACT_DATAPATH%"
if defined TESSERACT_CMD set "TESSERACT_CMD=%TESSERACT_CMD%"
set "OCR_PYTHON_PATH=%OCR_PYTHON_PATH%"
set "OCR_SCRIPT_PATH=%OCR_SCRIPT_PATH%"
set "OCR_PREPROCESSING_ENABLED=%OCR_PREPROCESSING_ENABLED%"
set "OCR_UPSCALE_THRESHOLD=%OCR_UPSCALE_THRESHOLD%"
set "OCR_BLUR_THRESHOLD=%OCR_BLUR_THRESHOLD%"
set "OCR_ENSEMBLE_FALLBACK=%OCR_ENSEMBLE_FALLBACK%"
set "OCR_CONF_THRESHOLD=%OCR_CONF_THRESHOLD%"
set "OCR_PADDLE_ENABLED=%OCR_PADDLE_ENABLED%"
set "OCR_PADDLE_SCRIPT=%OCR_PADDLE_SCRIPT%"
if defined OCR_SR_ENABLED set "OCR_SR_ENABLED=%OCR_SR_ENABLED%"
if defined ESRGAN_MODEL set "ESRGAN_MODEL=%ESRGAN_MODEL%"
set "OCR_PERSPECTIVE_ENABLED=%OCR_PERSPECTIVE_ENABLED%"
set "OCR_OSD_ENABLED=%OCR_OSD_ENABLED%"
set "OCR_DESKEW_MAX_ANGLE=%OCR_DESKEW_MAX_ANGLE%"
if defined OCR_LLM_ENABLED set "OCR_LLM_ENABLED=%OCR_LLM_ENABLED%"
if defined OCR_LLM_API_KEY set "OCR_LLM_API_KEY=%OCR_LLM_API_KEY%"
if defined OCR_LLM_BASE_URL set "OCR_LLM_BASE_URL=%OCR_LLM_BASE_URL%"
if defined OCR_LLM_MODEL set "OCR_LLM_MODEL=%OCR_LLM_MODEL%"
set "KEYSTORE_PASSWORD=%KEYSTORE_PASSWORD%"
set "KEYSTORE_ALIAS=%KEYSTORE_ALIAS%"
set "SPRING_MAIL_HOST=%SPRING_MAIL_HOST%"
set "SPRING_MAIL_PORT=%SPRING_MAIL_PORT%"
set "SPRING_MAIL_USERNAME=%SPRING_MAIL_USERNAME%"
set "SPRING_MAIL_PASSWORD=%SPRING_MAIL_PASSWORD%"

echo  ========================================
echo   Starting Spring Boot... (Ctrl+C untuk stop)
echo   Buka http://localhost:%SERVER_PORT% setelah "Started OcrToolApplication"
echo  ========================================
echo.

REM --- Jalankan (log tampil di window ini) ---
"%JAVA_EXE%" -jar "%JAR%"

echo.
echo  ----------------------------------------
echo   Aplikasi berhenti (exit code %errorlevel%)
echo  ----------------------------------------
pause
