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

