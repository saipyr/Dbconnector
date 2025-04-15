#!/bin/bash

echo "=== DB Connector Startup Script ==="
echo "Cleaning and building the application..."

# Navigate to the project directory
cd /Users/bhagidaran-0369/Desktop/Trae/Dbconnector

# Clean and package the application
mvn clean package -DskipTests

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "Build successful! Starting the application..."
    
    # Find the generated JAR file
    JAR_FILE=$(find target -name "*.jar" -not -name "*sources.jar" -not -name "*javadoc.jar" | head -1)
    
    if [ -z "$JAR_FILE" ]; then
        echo "Error: Could not find the JAR file. Build may have failed."
        exit 1
    fi
    
    echo "Running: $JAR_FILE"
    
    # Run the application with a different port (8081)
    java -jar $JAR_FILE --server.port=8081
else
    echo "Build failed. Please check the logs above for errors."
    exit 1
fi