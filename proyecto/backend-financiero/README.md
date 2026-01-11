# Backend Financiero

Ubicación: `proyecto/backend-financiero`

## Resumen
Servidor HTTP Java (clase `WebServer`) que expone endpoints REST y usa MySQL como almacenamiento.

## Requisitos

- Java JDK 11+ (o 8+ en muchos entornos)
- MySQL (o MariaDB) en ejecución (puerto 3306 por defecto)
- Las dependencias Java (`mysql-connector-j`, `gson`) deben estar en la carpeta `lib/`

## Instalación de dependencias

1. Descarga los archivos JAR necesarios y colócalos en `lib/`:
	 - [mysql-connector-j](https://dev.mysql.com/downloads/connector/j/)
	 - [gson](https://search.maven.org/artifact/com.google.code.gson/gson)

2. Instala MySQL:
	 - **Linux:**
		 ```bash
		 sudo apt update
		 sudo apt install mysql-server
		 sudo systemctl start mysql
		 ```
	 - **Windows:**
		 Descarga el instalador desde https://dev.mysql.com/downloads/installer/ y sigue el asistente.

3. (Opcional) Cambia las credenciales de la base de datos en `src/WebServer.java` si no usas `root`/`root`.

## Preparar la base de datos

1. Crea la base y tablas:
	 ```bash
	 cd proyecto/backend-financiero
	 mysql -u root -p < db/schema.sql
	 ```

2. (Opcional) Para crear un usuario admin:
	 - Genera el hash SHA-256 de la contraseña:
		 ```bash
		 echo -n "admin123" | sha256sum
		 # Copia solo el hash (sin espacios)
		 ```
	 - Inserta el usuario admin en MySQL:
		 ```sql
		 INSERT INTO users (curp, password, name, role) VALUES ('ADMINCURP000000000', '<hash>', 'Administrador', 'ADMIN');
		 INSERT INTO accounts (user_id, balance) SELECT id, 0.00 FROM users WHERE curp = 'ADMINCURP000000000';
		 ```

## Compilación y ejecución

### Windows (PowerShell):
```powershell
cd proyecto\backend-financiero
javac -d bin -cp "lib/*" src\WebServer.java
java -cp "bin;lib/*" WebServer 8080
```

### Linux / macOS (bash):
```bash
cd proyecto/backend-financiero
javac -d bin -cp "lib/*" src/WebServer.java
java -cp "bin:lib/*" WebServer 8080
```

El argumento `8080` es opcional; si no se pasa, se usa por defecto el puerto `8080`.

## Notas y solución de problemas

- Si obtienes `CommunicationsException` o error de acceso, verifica el usuario/contraseña y que el servicio MySQL esté en ejecución.
- Si el puerto 8080 ya está ocupado, lanza el servidor con otro puerto: `java -cp "bin;lib/*" WebServer 9090` (Windows) o `java -cp "bin:lib/*" WebServer 9090` (Linux).
- Revisa `lib/` para asegurarte de que `mysql-connector-j` y `gson` estén presentes.
- Si tienes problemas de permisos en MySQL, asegúrate de que el usuario tenga privilegios sobre la base `financiero_db`.

## Configuración y despliegue

### 1. Configuración de entorno
- Edita `src/config.properties` para definir los parámetros de base de datos, JWT y Pub/Sub.
- Para desarrollo local, usa los valores por defecto.
- Para producción, configura los valores de AWS y la URL de la base de datos en la nube.

### 2. Compilación y ejecución
```bash
cd backend-financiero
# Compilar
javac -cp "lib/*" -d bin src/**/*.java
# Ejecutar
java -cp "bin:lib/*" src/WebServer 8080
```

### 3. Variables importantes
- `db.url`, `db.user`, `db.pass`: Conexión a la base de datos
- `jwt.secret`: Clave secreta para JWT
- `pubsub.mode`: local o aws

### 4. Despliegue en producción (AWS)
- Configura los valores de AWS en `src/config.properties`.
- Asegúrate de que las dependencias estén en `lib/`.
- Usa una base de datos gestionada (RDS) y servicios de colas (SNS/SQS).

### 5. Notas
- El sistema está modularizado en servicios: AuthService, AccountService, TransactionService, AdminService, AuditService, PubSubSimulator.
- Para simular Pub/Sub localmente no necesitas AWS.
- Para producción, implementa los métodos de integración en PubSubSimulator y AuditService.

---

# Checklist de requisitos (resumido)
- [x] Microservicios REST en Java
- [x] AuthService (JWT)
- [x] AccountService (saldo, depósito, retiro, transferencia)
- [x] TransactionService (eventos Pub/Sub, integración lista)
- [x] AuditService (logs de transacción, integración lista)
- [x] AdminService (dashboard, usuarios, logs)
- [x] PubSubSimulator (simulación local, hooks AWS)
- [x] Configuración para desarrollo y producción
- [x] Frontend Angular (usuario y admin)
- [ ] Simulador de clientes Java (opcional, para demo)
- [ ] Monitoreo Lanterna (opcional, para demo)

# Despliegue recomendado para estudiantes

## Opción 1: Local (más fácil y suficiente para la demo)
1. **Base de datos:**
   - Instala MySQL localmente y ejecuta el script `db/schema.sql`.
2. **Backend:**
   - Compila todo el código Java:
     ```bash
     javac -d bin -cp "lib/*" src/**/*.java
     ```
   - Ejecuta el servidor:
     ```bash
     java -cp "bin:lib/*" WebServer 8080
     ```
3. **Frontend:**
   - En otra terminal:
     ```bash
     cd ../financiero-web
     npm install
     npm start
     ```
   - Accede a `http://localhost:4200`.

## Opción 2: Nube gratuita (solo si quieres mostrarlo online)
- Puedes usar una máquina virtual gratuita en Google Cloud, AWS o Azure (free tier) para levantar el backend y la base de datos.
- Sube el frontend a un bucket S3, Firebase Hosting, o Netlify (tienen planes gratuitos).
- Cambia la URL del backend en `environment.prod.ts`.
- **No es obligatorio para la calificación, pero suma puntos en la demo.**

# Recomendaciones
- Haz pruebas locales completas antes de la demo.
- Si tienes poco tiempo, prioriza la opción local.
- Documenta bien los pasos en el README y ten a la mano los comandos.
- Si el profe pide el simulador de clientes o Lanterna, implementa un programa Java aparte que consuma los endpoints REST.

# Entrega
- Sube todo el código a un repositorio privado de GitHub o Google Drive.
- Incluye capturas de pantalla y el README actualizado.
- Ten listo el enlace de la web de usuario y admin para la demo.

---

# Estructura de carpetas
- `src/` Código fuente Java
- `lib/` Dependencias externas
- `db/` Scripts de base de datos
- `bin/` Archivos compilados

