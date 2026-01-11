# Simulador de Clientes Java

Ubicación: `proyecto/backend-financiero/src/SimuladorClientes.java`

Este programa es independiente del backend y sirve para simular múltiples clientes realizando operaciones concurrentes (registro, login, depósitos, retiros y transferencias) contra el backend REST.

---

## Compilación y ejecución del simulador (por separado)

### 1. Compilar solo el simulador

```bash
cd proyecto/backend-financiero
javac -d bin -cp "lib/*" src/SimuladorClientes.java
```

### 2. Ejecutar el simulador

```bash
cd proyecto/backend-financiero
java -cp "bin:lib/*" SimuladorClientes <n> <h> <p> <t> <txMax>
```

- `n`: número de clientes a simular
- `h`: número de hilos (concurrentes)
- `p`: monto inicial de cada cliente
- `t`: transacciones por minuto por cliente
- `txMax`: transacciones totales por cliente

**Ejemplo:**
```bash
java -cp "bin:lib/*" SimuladorClientes 5 8 100 10 8
```

---

## Notas importantes
- El simulador no requiere compilar todo el backend, solo el archivo `SimuladorClientes.java`.
- El backend (`WebServer`) debe estar corriendo antes de ejecutar el simulador.
- El simulador crea usuarios con CURP `SIMU0`, `SIMU1`, ... y realiza operaciones reales sobre la API REST.
- Puedes limpiar los datos de prueba con el script `db/cleanup_simulador.sql`.

---

## Compilar y ejecutar SOLO el backend (sin el simulador)

```bash
cd proyecto/backend-financiero
javac -d bin -cp "lib/*" src/WebServer.java
java -cp "bin:lib/*" WebServer 8080
```

---

## Resumen
- **Compila y ejecuta el backend y el simulador por separado.**
- No es necesario compilar todo el backend para probar el simulador.
- Consulta este archivo si tienes dudas sobre cómo lanzar el simulador para pruebas o demo.
