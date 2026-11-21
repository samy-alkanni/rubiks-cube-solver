@echo off
setlocal
cd /d %~dp0

echo Running JavaFX app via Maven Wrapper...
echo (This will download Maven + dependencies on first run.)
echo.
call "%~dp0mvnw.cmd" javafx:run

if errorlevel 1 (
  echo.
  echo FAILED. Make sure you have JDK 21+ installed.
  pause
)
