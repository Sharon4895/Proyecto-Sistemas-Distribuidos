#!/bin/bash
# Script para iniciar LanternaMonitor
JAR_NAME=LanternaMonitor.jar
LOG_FILE=lanterna.log

java -jar "$JAR_NAME" > "$LOG_FILE" 2>&1 &
echo "LanternaMonitor iniciado. Logs en $LOG_FILE"
