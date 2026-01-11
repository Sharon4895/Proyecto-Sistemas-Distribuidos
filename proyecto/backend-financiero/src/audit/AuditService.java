
package audit;

import pubsub.PubSubSimulator;

/**
 * Servicio de auditoría listo para despliegue distribuido.
 * Si requiere acceso a BD, parametrizar por variables de entorno como los otros servicios.
 * Ejemplo de ejecución:
 *   java -cp ... audit.AuditService
 */
public class AuditService {
    public static void main(String[] args) {
        int port = 8084;
        try {
            com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(port), 0);
            PubSubSimulator pubsub = new PubSubSimulator();
            AuditService service = new AuditService(pubsub);
            server.createContext("/audit", exchange -> {
                if ("POST".equals(exchange.getRequestMethod())) {
                    try (java.io.InputStreamReader isr = new java.io.InputStreamReader(exchange.getRequestBody(), java.nio.charset.StandardCharsets.UTF_8)) {
                        java.util.Map<String, Object> body = new com.google.gson.Gson().fromJson(isr, java.util.Map.class);
                        String event = (String) body.get("event");
                        service.logTransaction(event);
                        String response = "{\"success\":true}";
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, response.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
                        exchange.getResponseBody().write(response.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
                exchange.close();
            });
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            System.out.println("AuditService escuchando en el puerto " + port);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private final PubSubSimulator pubsub;

    public AuditService(PubSubSimulator pubsub) {
        this.pubsub = pubsub;
        // Suscribirse al tópico de auditoría
        pubsub.subscribe("audit", this::consumeAuditEvent);
    }

    // Registrar un evento de auditoría
    public void logTransaction(String event) {
        pubsub.publish("audit", event);
    }

    // Consumir evento de auditoría
    private void consumeAuditEvent(String event) {
        // Aquí se puede guardar en BD, archivo, o enviar a AWS
        System.out.println("[AUDIT] Evento recibido: " + event);
        // TODO: Guardar en base de datos o enviar a AWS
    }
}
