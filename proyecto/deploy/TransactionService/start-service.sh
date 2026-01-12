#!/bin/bash
# Script para iniciar TransactionService
JAR_NAME=TransactionService.jar
LOG_FILE=transaction.log

java -jar "$JAR_NAME" > "$LOG_FILE" 2>&1 &
echo "TransactionService iniciado. Logs en $LOG_FILE"
