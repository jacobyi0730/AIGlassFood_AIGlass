@echo off
setlocal

set "SERVER_ROOT=%~dp0"

if not exist "%SERVER_ROOT%package.json" (
  echo [FoodServer] package.json not found.
  exit /b 1
)

if not exist "%SERVER_ROOT%.env" (
  echo [FoodServer] .env file not found.
  echo [FoodServer] Copy .env.example to .env and set NVIDIA values first.
  exit /b 1
)

if not exist "%SERVER_ROOT%node_modules" (
  echo [FoodServer] node_modules not found. Running npm install first...
  call npm install
  if errorlevel 1 (
    echo [FoodServer] npm install failed.
    exit /b 1
  )
)

cd /d "%SERVER_ROOT%"
echo [FoodServer] Starting local server on 0.0.0.0...
call npm start

endlocal
