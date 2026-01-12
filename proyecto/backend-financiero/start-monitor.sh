#!/bin/bash
# Script para iniciar el monitor Lanterna
# Uso: ./start-monitor.sh [config.lanterna.properties] [logfile]

JAR_NAME=LanternaMonitor.jar
CONFIG_FILE=${1:-config.lanterna.properties}
LOG_FILE=${2:-monitor.log}

java -jar "$JAR_NAME" "$CONFIG_FILE" > "$LOG_FILE" 2>&1 &
echo "Monitor Lanterna iniciado. Logs en $LOG_FILE"
