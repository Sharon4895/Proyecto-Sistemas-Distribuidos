#!/bin/bash
# Script para iniciar SimuladorClientes
JAR_NAME=SimuladorClientes.jar
LOG_FILE=simulador.log

java -jar "$JAR_NAME" > "$LOG_FILE" 2>&1 &
echo "SimuladorClientes iniciado. Logs en $LOG_FILE"
