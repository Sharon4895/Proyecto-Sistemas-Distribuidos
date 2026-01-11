# FinancieroWeb (Frontend Angular)

Ubicación: `proyecto/financiero-web`

## Requisitos
- Node.js v16+ y npm
- Angular CLI 17
- Acceso a un backend Java corriendo en `http://localhost:8080` (o ajusta la URL en los servicios)

## Instalación de Node.js y Angular CLI

### Windows
1. Descarga e instala Node.js desde https://nodejs.org (elige la versión LTS recomendada).
2. Abre PowerShell y ejecuta:
	```powershell
	npm install -g @angular/cli@17
	```

### Linux
1. Instala Node.js:
	```bash
	sudo apt update
	sudo apt install nodejs npm
	# (Opcional) Instala n para gestionar versiones:
	sudo npm install -g n
	sudo n lts
	```
2. Instala Angular CLI:
	```bash
	npm install -g @angular/cli@17
	```

## Instalación de dependencias y levantamiento

### Windows (PowerShell):
```powershell
cd proyecto\financiero-web
npm install
npm start
# Abre http://localhost:4200 en tu navegador
```

### Linux (bash):
```bash
cd proyecto/financiero-web
npm install
npm start
# Abre http://localhost:4200 en tu navegador
```

## Configuración del backend
Por defecto, el frontend se comunica con el backend en `http://localhost:8080` usando proxy (`proxy.conf.json`). Si tu backend está en otra dirección, edita:

- `src/app/core/services/auth.service.ts`
- `src/app/core/services/account.service.ts`
- `src/app/core/services/admin.service.ts`

Busca la línea con `http://localhost:8080` y reemplázala por la URL de tu backend.

## Configuración de entornos

- Para desarrollo, la API apunta a `http://localhost:8080/api` (ver `src/environments/environment.ts`).
- Para producción, edita `src/environments/environment.prod.ts` y coloca la URL real del backend desplegado.

## Despliegue en producción

```bash
npm run build -- --configuration production
# Los archivos estarán en dist/financiero-web
```

- Sube el contenido de `dist/financiero-web` a tu servidor web o bucket S3.
- Asegúrate de que el backend esté accesible desde la URL configurada en `environment.prod.ts`.

## Notas
- El login muestra un mensaje claro si las credenciales son incorrectas.
- El sistema incluye navegación fácil con botones de regreso en las vistas de admin y cliente.
- El frontend es responsivo y soporta rutas protegidas por roles.
- Los servicios de Angular (`auth.service.ts`, `account.service.ts`, `admin.service.ts`) usan la URL del entorno.
- Para integración con AWS, asegúrate de que los endpoints estén correctamente configurados.

---

## Checklist de requisitos cumplidos

**Backend:**
- [x] Microservicios REST en Java
- [x] AuthService (JWT)
- [x] AccountService (saldo, depósito, retiro, transferencia)
- [x] TransactionService (eventos Pub/Sub, integración lista)
- [x] AuditService (logs de transacción, integración lista)
- [x] AdminService (dashboard, usuarios, logs)
- [x] PubSubSimulator (simulación local, hooks AWS)
- [x] Configuración para desarrollo y producción
- [x] Frontend Angular (usuario y admin)
- [x] Simulador de clientes Java (opcional, para demo)
- [x] Monitor Lanterna (opcional, para demo)

**Frontend:**
- [x] Login y registro conectados al backend
- [x] Dashboard de usuario y admin
- [x] Transferencias entre usuarios (por CURP)
- [x] Historial de transacciones
- [x] Rutas protegidas por rol (guards)
- [x] Integración con JWT y backend
- [x] Configuración de URL del backend por entorno
- [x] Experiencia de usuario robusta y responsiva

---

## Instrucciones de despliegue local

**Backend:**
1. Instala Java JDK 11+ y MySQL.
2. Coloca los JAR necesarios en `lib/`.
3. Crea la base de datos con `db/schema.sql`.
4. Compila y ejecuta:
   ```bash
   javac -d bin -cp "lib/*" src/WebServer.java
   java -cp "bin:lib/*" WebServer 8080
   ```

**Frontend:**
1. Instala Node.js y Angular CLI.
2. Instala dependencias y ejecuta:
   ```bash
   cd proyecto/financiero-web
   npm install
   npm start
   ```
3. Accede a `http://localhost:4200`.

---

## Instrucciones de despliegue en la nube

**Backend:**
- Configura `src/config.properties` con los datos de tu VM/AWS.
- Usa una base de datos gestionada (RDS) y servicios de colas (SNS/SQS) si es necesario.
- Asegúrate de que el backend sea accesible desde la URL pública.

**Frontend:**
- Edita `src/environments/environment.prod.ts` y coloca la URL real del backend.
- Compila para producción:
  ```bash
  npm run build -- --configuration production
  ```
- Sube el contenido de `dist/financiero-web` a tu servidor web o bucket S3.

---

## Recomendaciones para estudiantes sin presupuesto

- Puedes simular Pub/Sub localmente sin AWS.
- Usa MySQL local y despliega el backend en una VM gratuita (Google Cloud, AWS Free Tier, etc.).
- Cambia la URL del backend en el archivo de entorno del frontend para apuntar a tu VM.


