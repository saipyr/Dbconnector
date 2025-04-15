@echo off
echo Starting DB Connector application...

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Java is not installed. Please install Java and try again.
    exit /b 1
)

REM Check if the JAR file exists
if not exist "target\db-connector-0.0.1-SNAPSHOT.jar" (
    echo JAR file not found. Building the application...
    
    REM Check if Maven is installed
    where mvn >nul 2>nul
    if %ERRORLEVEL% neq 0 (
        echo Maven is not installed. Please install Maven and try again.
        exit /b 1
    )
    
    REM Build the application
    call mvn clean package
    
    if %ERRORLEVEL% neq 0 (
        echo Build failed. Please check the logs for errors.
        exit /b 1
    )
)

REM Create logs directory if it doesn't exist
if not exist "logs" mkdir logs

REM Start the application
start /B javaw -jar target\db-connector-0.0.1-SNAPSHOT.jar > logs\app.log 2>&1

REM Save the PID to a file (using wmic to get the PID)
for /f "tokens=2 delims==" %%a in ('wmic process where "commandline like '%%db-connector-0.0.1-SNAPSHOT.jar%%'" get processid /format:list') do (
    echo %%a > .pid
    echo DB Connector application started with PID: %%a
)

echo You can access the application at http://localhost:8080
echo Logs are available at logs\app.log