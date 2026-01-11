package audit;

import pubsub.PubSubSimulator;

public class AuditService {
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
