@if "%DEBUG%"=="" @echo off
setlocal

set APP_HOME=%~dp0
set GRADLE_VERSION=8.14.3
set GRADLE_CACHE=%USERPROFILE%\.gradle\wrapper\dists\fitplan-gradle-%GRADLE_VERSION%
set GRADLE_EXE=%GRADLE_CACHE%\gradle-%GRADLE_VERSION%\bin\gradle.bat

if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto ensureGradle

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%\bin\java.exe

if exist "%JAVA_EXE%" goto ensureGradle

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
goto fail

:ensureGradle
if exist "%GRADLE_EXE%" goto execute

echo Downloading Gradle %GRADLE_VERSION%...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop';" ^
  "$ProgressPreference='SilentlyContinue';" ^
  "$cache='%GRADLE_CACHE%';" ^
  "$zip=Join-Path $cache 'gradle-%GRADLE_VERSION%-bin.zip';" ^
  "New-Item -ItemType Directory -Force -Path $cache | Out-Null;" ^
  "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile $zip;" ^
  "Expand-Archive -LiteralPath $zip -DestinationPath $cache -Force;"
if %ERRORLEVEL% neq 0 goto fail

:execute
"%GRADLE_EXE%" %*
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
exit /b 1

:mainEnd
endlocal
