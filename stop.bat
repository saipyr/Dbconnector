@echo off
echo Stopping DB Connector application...

REM Check if PID file exists
if not exist ".pid" (
    echo PID file not found. The application may not be running.
    exit /b 1
)

REM Read the PID from the file
set /p PID=<.pid

REM Check if the process is running
wmic process where processid="%PID%" get processid >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Process with PID %PID% is not running.
    del .pid
    exit /b 1
)

REM Kill the process
taskkill /PID %PID% /F

REM Remove the PID file
del .pid

REM Backup any unsaved connections if needed
if exist "connections_temp.json" (
    echo Backing up unsaved connections...
    if not exist "connections_backup" mkdir connections_backup
    for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (
        set datestamp=%%c-%%a-%%b
    )
    for /f "tokens=1-2 delims=: " %%a in ('time /t') do (
        set timestamp=%%a%%b
    )
    copy /Y connections_temp.json "connections_backup\connections_%datestamp%_%timestamp%.json"
    echo Connections backed up to connections_backup\connections_%datestamp%_%timestamp%.json
)

echo Application stopped.