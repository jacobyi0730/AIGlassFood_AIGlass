@echo off
setlocal

set "SERVER_ROOT=%~dp0"
set "SERVER_PORT=8000"

if not exist "%SERVER_ROOT%package.json" (
  echo [FoodServer] package.json not found.
  exit /b 1
)

if not exist "%SERVER_ROOT%.env" (
  echo [FoodServer] .env file not found.
  echo [FoodServer] Copy .env.example to .env and set NVIDIA values first.
  exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in ("%SERVER_ROOT%.env") do (
  if /I "%%A"=="SERVER_PORT" (
    set "SERVER_PORT=%%B"
  )
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
echo [FoodServer] Clearing existing process on port %SERVER_PORT%...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$port = %SERVER_PORT%;" ^
  "$connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue;" ^
  "if ($connections) {" ^
  "  $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique;" ^
  "  foreach ($pidValue in $pids) {" ^
  "    try {" ^
  "      $process = Get-Process -Id $pidValue -ErrorAction Stop;" ^
  "      Write-Host ('[FoodServer] Stopping PID ' + $pidValue + ' (' + $process.ProcessName + ') on port ' + $port + '...');" ^
  "      Stop-Process -Id $pidValue -Force -ErrorAction Stop;" ^
  "    } catch {" ^
  "      Write-Host ('[FoodServer] Failed to stop PID ' + $pidValue + ': ' + $_.Exception.Message);" ^
  "      exit 1;" ^
  "    }" ^
  "  }" ^
  "} else {" ^
  "  Write-Host ('[FoodServer] No existing process found on port ' + $port + '.');" ^
  "}"
if errorlevel 1 (
  echo [FoodServer] Could not clear the existing server process.
  exit /b 1
)

echo [FoodServer] Starting local server on 0.0.0.0:%SERVER_PORT%...
call npm start

endlocal
