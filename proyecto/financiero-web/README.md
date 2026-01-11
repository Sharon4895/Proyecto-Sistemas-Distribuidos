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


