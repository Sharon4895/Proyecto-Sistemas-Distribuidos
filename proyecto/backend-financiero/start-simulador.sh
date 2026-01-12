#!/bin/bash
# Script para iniciar el simulador de clientes
# Uso: ./start-simulador.sh [config.simulador.properties] [logfile]

JAR_NAME=SimuladorClientes.jar
CONFIG_FILE=${1:-config.simulador.properties}
LOG_FILE=${2:-simulador.log}

java -jar "$JAR_NAME" "$CONFIG_FILE" > "$LOG_FILE" 2>&1 &
echo "Simulador de clientes iniciado. Logs en $LOG_FILE"
