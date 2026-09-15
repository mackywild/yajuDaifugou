@echo off
if "%DAIFUGO_PASSWORD%"=="" (
  echo ERROR: DAIFUGO_PASSWORD を設定してください。
  echo 例: set DAIFUGO_PASSWORD=change-me
  exit /b 1
)
call gradlew.bat bootRun
