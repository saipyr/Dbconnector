#!/bin/bash

echo "=== Stopping DB Connector application ==="

# Find the Java process running the application
PID=$(ps aux | grep "java -jar" | grep "Dbconnector" | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    echo "No running DB Connector application found."
    exit 0
else
    echo "Found DB Connector application with PID: $PID"
    echo "Stopping application..."
    kill $PID
    
    # Wait for the process to terminate
    for i in {1..10}; do
        if ps -p $PID > /dev/null; then
            echo "Waiting for application to terminate... ($i/10)"
            sleep 1
        else
            echo "Application stopped successfully."
            exit 0
        fi
    done
    
    # Force kill if still running
    if ps -p $PID > /dev/null; then
        echo "Application did not terminate gracefully. Forcing termination..."
        kill -9 $PID
        echo "Application terminated."
    fi
fi