#!/bin/bash
# Script para iniciar AccountService
JAR_NAME=AccountService.jar
LOG_FILE=account.log

java -jar "$JAR_NAME" > "$LOG_FILE" 2>&1 &
echo "AccountService iniciado. Logs en $LOG_FILE"
