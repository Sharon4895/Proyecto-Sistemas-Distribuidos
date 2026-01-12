#!/bin/bash
# Script para iniciar AdminService
JAR_NAME=AdminService.jar
LOG_FILE=admin.log

java -jar "$JAR_NAME" > "$LOG_FILE" 2>&1 &
echo "AdminService iniciado. Logs en $LOG_FILE"
