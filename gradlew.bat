@echo off
setlocal
set "ROOT_DIR=%~dp0"
set "PROPERTIES_FILE=%ROOT_DIR%gradle\wrapper\gradle-wrapper.properties"

if not exist "%PROPERTIES_FILE%" (
  echo ERROR: gradle\wrapper\gradle-wrapper.properties tidak ditemukan.
  exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in ("%PROPERTIES_FILE%") do (
  if "%%A"=="distributionUrl" set "DIST_URL=%%B"
)

if not defined DIST_URL (
  echo ERROR: distributionUrl tidak ditemukan.
  exit /b 1
)

set "DIST_URL=%DIST_URL:\:=%"
for %%I in ("%DIST_URL%") do set "ZIP_NAME=%%~nxI"
set "DIST_NAME=%ZIP_NAME:.zip=%"
set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
if defined GRADLE_USER_HOME_OVERRIDE set "GRADLE_USER_HOME=%GRADLE_USER_HOME_OVERRIDE%"
set "INSTALL_DIR=%GRADLE_USER_HOME%\manual-distributions\%DIST_NAME%"
set "GRADLE_BIN=%INSTALL_DIR%\%DIST_NAME%\bin\gradle.bat"

if not exist "%GRADLE_BIN%" (
  if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
  echo Mengunduh distribusi Gradle: %DIST_URL%
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%INSTALL_DIR%\%ZIP_NAME%'"
  if errorlevel 1 exit /b 1
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%INSTALL_DIR%\%ZIP_NAME%' -DestinationPath '%INSTALL_DIR%' -Force"
  if errorlevel 1 exit /b 1
)

if not exist "%GRADLE_BIN%" (
  echo ERROR: Gradle tidak ditemukan setelah ekstraksi.
  exit /b 1
)

call "%GRADLE_BIN%" %*
exit /b %ERRORLEVEL%
