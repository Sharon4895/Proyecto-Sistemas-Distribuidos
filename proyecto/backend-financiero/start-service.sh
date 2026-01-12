#!/bin/bash
# Script para iniciar un microservicio Java
# Uso: ./start-service.sh <NombreDelJar> [config.properties] [logfile]

JAR_NAME=${1:-AccountService.jar}
CONFIG_FILE=${2:-config.properties}
LOG_FILE=${3:-service.log}

java -jar "$JAR_NAME" --spring.config.location="$CONFIG_FILE" > "$LOG_FILE" 2>&1 &
echo "Servicio $JAR_NAME iniciado. Logs en $LOG_FILE"
