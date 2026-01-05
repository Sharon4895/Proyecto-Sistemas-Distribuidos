# Backend Financiero

Ubicación: `proyecto/backend-financiero`

## Resumen

Servidor HTTP Java (clase `WebServer`) que expone endpoints REST básicos y usa MySQL como almacenamiento.

## Requisitos

- Java JDK 11+ (o 8+ en muchos entornos).
- MySQL (o MariaDB) en ejecución.
- Las dependencias Java están en `lib/` (`mysql-connector-j`, `gson`) y las clases compiladas en `bin/`.

## Preparar la base de datos

1) Crear la base de datos y tablas (Windows / Linux):

```bash
# abre cliente MySQL y ejecuta:
mysql -u root -p
CREATE DATABASE financiero_db;
EXIT;
```

2) Importar el esquema (desde la raíz del proyecto):

```bash
cd proyecto/backend-financiero
mysql -u root -p financiero_db < db/schema.sql
```

Si tu usuario/contraseña son diferentes, sustitúyelos en los comandos o edita `src/WebServer.java`.

## Instalación y ejecución

Windows (PowerShell):

```powershell
cd proyecto\backend-financiero
# compilar (si hiciste cambios)
javac -d bin -cp "lib/*" src\WebServer.java

# ejecutar (separa classpath con ';')
java -cp "bin;lib/*" WebServer 8080
```

Linux / macOS (bash):

```bash
cd proyecto/backend-financiero
# compilar (si hiciste cambios)
javac -d bin -cp "lib/*" src/WebServer.java

# ejecutar (separa classpath con ':')
java -cp "bin:lib/*" WebServer 8080
```

El argumento `8080` es opcional; si no se pasa, se usa por defecto el puerto `8080`.

## Configuración (consejos)

- Credenciales y URL de la base de datos están hardcodeadas en `src/WebServer.java` en las constantes `DB_URL`, `DB_USER`, `DB_PASS`. Edita esos valores si tu MySQL usa otras credenciales.
- Para mayor seguridad, puedo ayudarte a modificar `WebServer` para leer credenciales desde variables de entorno o un archivo de configuración.
- Asegúrate de que el puerto de MySQL (3306) permita conexiones desde la máquina donde ejecutas el backend.

## Notas y solución de problemas

- Si obtienes `CommunicationsException` o error de acceso, verifica el usuario/contraseña y que el servicio MySQL esté en ejecución.
- Si el puerto 8080 ya está ocupado, lanza el servidor con otro puerto: `java -cp "bin;lib/*" WebServer 9090` (Windows) o `java -cp "bin:lib/*" WebServer 9090` (Linux).
- Revisa `lib/` para asegurarte de que `mysql-connector-j` y `gson` estén presentes; si faltan, cópialos ahí o añádelos al classpath.

¿Quieres que modifique `src/WebServer.java` para leer las credenciales desde variables de entorno? Puedo hacerlo y añadir scripts de arranque para Windows/Linux.
