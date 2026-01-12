#!/bin/bash
# Script para iniciar AuditService
JAR_NAME=AuditService.jar
LOG_FILE=audit.log

java -jar "$JAR_NAME" > "$LOG_FILE" 2>&1 &
echo "AuditService iniciado. Logs en $LOG_FILE"
