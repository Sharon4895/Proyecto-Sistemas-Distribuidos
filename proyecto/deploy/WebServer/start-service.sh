#!/bin/bash
# Script para iniciar WebServer
JAR_NAME=WebServer.jar
LOG_FILE=webserver.log

java -jar "$JAR_NAME" > "$LOG_FILE" 2>&1 &
echo "WebServer iniciado. Logs en $LOG_FILE"
