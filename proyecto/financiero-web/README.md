# FinancieroWeb

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 17.3.5.

## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

# FinancieroWeb

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 17.3.5.

## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via a platform of your choice. To use this command, you need to first add a package that implements end-to-end testing capabilities.

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.

---

## Instalación (Windows / Linux)

Requisitos: `Node.js` (v16+ recomendado) y `npm` o `yarn`.

Windows (PowerShell):

```powershell
cd proyecto\financiero-web
npm install
npm start
# abre http://localhost:4200 en tu navegador
```

Linux (bash):

```bash
cd proyecto/financiero-web
npm install
npm start
# abre http://localhost:4200 en tu navegador
```

## Configuración

Por defecto el frontend apunta a `http://localhost:8080` para el backend. Si tu backend está en otra dirección modifica las variables `apiUrl` ubicadas en:

- `src/app/core/services/auth.service.ts`
- `src/app/core/services/account.service.ts`
- `src/app/core/services/admin.service.ts`

Busca la línea que contiene `http://localhost:8080` y reemplázala por la URL de tu backend, por ejemplo:

```ts
private apiUrl = 'http://mi-servidor:8080/api/auth';
```

Para preparación de producción usa:

```bash
npm run build -- --configuration production
```

Si prefieres, puedo ayudarte a mover la URL del backend a los archivos de entornos (`src/environments`) para manejar múltiples entornos más fácilmente.
