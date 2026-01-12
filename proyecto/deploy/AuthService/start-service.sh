#!/bin/bash
# Script para iniciar AuthService
JAR_NAME=AuthService.jar
LOG_FILE=auth.log

java -jar "$JAR_NAME" > "$LOG_FILE" 2>&1 &
echo "AuthService iniciado. Logs en $LOG_FILE"
