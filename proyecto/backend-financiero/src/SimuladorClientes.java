import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimuladorClientes {
    // Variable global para saber cuántos clientes existen (usada en transferencias)
    public static int numClientesGlobal = 0;

    // Constantes de configuración
    private static final String BASE_URL = "http://localhost:8080/api";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    
    // Cliente HTTP compartido (Thread-safe y optimizado en Java 11+)
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(TIMEOUT)
            .build();

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 5) {
            System.out.println("Uso: java SimuladorClientes <n> <h> <p> <t> <txMax>");
            return;
        }

        int numClientes = Integer.parseInt(args[0]);
        numClientesGlobal = numClientes;
        int hilos = Integer.parseInt(args[1]);
        int montoInicial = Integer.parseInt(args[2]);
        int txPorMinuto = Integer.parseInt(args[3]);
        int txMaxPorCliente = Integer.parseInt(args[4]);

        // Validación para evitar división por cero en el sleep
        if (txPorMinuto <= 0) txPorMinuto = 1;

        System.out.printf("Iniciando simulación: %d clientes, %d hilos, %d tx/min%n", numClientes, hilos, txPorMinuto);

        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch latch = new CountDownLatch(numClientes);
        AtomicInteger erroresGlobales = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numClientes; i++) {
            final int clienteId = i;
            // Variable final efectiva para usar dentro del lambda
            final int finalTxPorMinuto = txPorMinuto;
            
            pool.submit(() -> {
                try {
                    simularCliente(clienteId, montoInicial, finalTxPorMinuto, txMaxPorCliente);
                } catch (Exception e) {
                    erroresGlobales.incrementAndGet();
                    System.err.printf("[Cliente %d] Error fatal: %s%n", clienteId, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        pool.shutdown();
        
        // Esperamos un tiempo razonable basado en la carga estimada + un margen
        boolean terminado = latch.await(30, TimeUnit.MINUTES);
        
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("--------------------------------------------------");
        if (!terminado) {
            System.err.println("Advertencia: Se agotó el tiempo de espera (timeout).");
            pool.shutdownNow();
        }
        System.out.printf("Simulación finalizada en %.2f segundos.%n", duration / 1000.0);
        System.out.printf("Errores fatales detectados: %d%n", erroresGlobales.get());
    }

    static void simularCliente(int id, int montoInicial, int txPorMin, int txMax) {
        String curp = "SIMU" + id;
        String password = "1234";
        String nombre = "Cliente" + id;

        try {
            // 1. Registro
            String jsonRegistro = String.format("{\"curp\":\"%s\",\"password\":\"%s\",\"name\":\"%s\"}", curp, password, nombre);
            sendRequest(BASE_URL + "/auth/register", jsonRegistro, null);

            // 2. Login
            String jsonLogin = String.format("{\"curp\":\"%s\",\"password\":\"%s\"}", curp, password);
            String responseLogin = sendRequest(BASE_URL + "/auth/login", jsonLogin, null);
            
            String token = extractToken(responseLogin);
            if (token == null) throw new RuntimeException("No se pudo obtener el token");

            System.out.printf("[Cliente %d] Login exitoso.%n", id);

            // 3. Depósito Inicial
            String jsonDeposito = String.format("{\"type\":\"DEPOSIT\",\"amount\":%d}", montoInicial);
            sendRequest(BASE_URL + "/account/operate", jsonDeposito, token);

            // 4. Ciclo de transacciones
            long sleepTime = 60000 / txPorMin;
            
            for (int i = 0; i < txMax; i++) {
                try {
                    Thread.sleep(sleepTime);
                    int tipoTx = ThreadLocalRandom.current().nextInt(3); // 0: DEPOSIT, 1: WITHDRAW, 2: TRANSFER
                    int monto = ThreadLocalRandom.current().nextInt(1, 101);
                    if (tipoTx == 0) {
                        String body = String.format("{\"type\":\"DEPOSIT\",\"amount\":%d}", monto);
                        sendRequest(BASE_URL + "/account/operate", body, token);
                        System.out.printf("[Cliente %d] Tx %d/%d: DEP $%d\n", id, i + 1, txMax, monto);
                    } else if (tipoTx == 1) {
                        String body = String.format("{\"type\":\"WITHDRAW\",\"amount\":%d}", monto);
                        sendRequest(BASE_URL + "/account/operate", body, token);
                        System.out.printf("[Cliente %d] Tx %d/%d: RET $%d\n", id, i + 1, txMax, monto);
                    } else {
                        // Transferencia a otro cliente
                        int destinoId;
                        do {
                            destinoId = ThreadLocalRandom.current().nextInt(0, numClientesGlobal);
                        } while (destinoId == id);
                        String destinoCurp = "SIMU" + destinoId;
                        String body = String.format("{\"targetCurp\":\"%s\",\"amount\":%d,\"description\":\"Transferencia simulada\"}", destinoCurp, monto);
                        System.out.printf("[Cliente %d] Intentando transferencia de $%d a Cliente %d...\n", id, monto, destinoId);
                        String resp = sendRequest(BASE_URL + "/account/transfer", body, token);
                        System.out.printf("[Cliente %d] Tx %d/%d: TRANSFER $%d -> Cliente %d OK. Respuesta: %s\n", id, i + 1, txMax, monto, destinoId, resp);
                    }
                } catch (RuntimeException re) {
                    System.err.printf("[Cliente %d] Error en tx %d/%d: %s\n", id, i + 1, txMax, re.getMessage());
                    // Continúa con la siguiente transacción
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    System.err.printf("[Cliente %d] Hilo interrumpido.\n", id);
                    break;
                } catch (Exception e) {
                    System.err.printf("[Cliente %d] Error inesperado en tx %d/%d: %s\n", id, i + 1, txMax, e.getMessage());
                    // Continúa con la siguiente transacción
                }
            }
            
            System.out.printf("[Cliente %d] Finalizó todas sus operaciones.%n", id);

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Imprimimos el error pero dejamos que el hilo termine limpiamente
            System.err.printf("[Cliente %d] Falló en operación: %s%n", id, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // Método genérico para enviar peticiones POST
    static String sendRequest(String url, String jsonBody, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + response.statusCode() + " Body: " + response.body());
        }
        
        return response.body();
    }

    // Extracción de token usando Regex (más robusto que indexOf)
    static String extractToken(String jsonResponse) {
        // Busca "token":"(cualquier cosa que no sea comillas)"
        Pattern pattern = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(jsonResponse);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}