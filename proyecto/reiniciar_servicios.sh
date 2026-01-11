#!/bin/bash
# Script para compilar, detener y arrancar todos los microservicios y el WebServer
# Directorio base del backend
BACKEND_DIR="$(dirname "$0")/backend-financiero"
cd "$BACKEND_DIR" || exit 1

# Compilar fuentes
javac -d bin -cp "lib/*" src/*.java src/account/*.java src/admin/*.java src/audit/*.java src/auth/*.java src/models/*.java src/pubsub/*.java src/transaction/*.java src/utils/*.java

# Detener servicios existentes
for svc in AuthService AccountService TransactionService AdminService AuditService WebServer; do
  pkill -f "$svc" 2>/dev/null
  sleep 1
done


# Crear carpeta logs si no existe
mkdir -p logs

# Arrancar servicios en background
nohup java -cp "bin:lib/*" auth.AuthService > logs/auth.log 2>&1 &
sleep 1
nohup java -cp "bin:lib/*" account.AccountService > logs/account.log 2>&1 &
sleep 1
nohup java -cp "bin:lib/*" transaction.TransactionService > logs/transaction.log 2>&1 &
sleep 1
nohup java -cp "bin:lib/*" admin.AdminService > logs/admin.log 2>&1 &
sleep 1
nohup java -cp "bin:lib/*" audit.AuditService > logs/audit.log 2>&1 &
sleep 1
nohup java -cp "bin:lib/*" WebServer > logs/webserver.log 2>&1 &

echo "Todos los servicios han sido reiniciados. Revisa la carpeta logs/ para detalles."
