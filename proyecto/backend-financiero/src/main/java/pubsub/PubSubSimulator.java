package pubsub;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class PubSubSimulator {
    private final Map<String, List<Consumer<String>>> subscribers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // Publica un mensaje en un tópico
    public void publish(String topic, String message) {
        List<Consumer<String>> subs = subscribers.getOrDefault(topic, Collections.emptyList());
        for (Consumer<String> sub : subs) {
            executor.submit(() -> sub.accept(message));
        }
    }

    // Suscribe un consumidor a un tópico
    public void subscribe(String topic, Consumer<String> handler) {
        subscribers.computeIfAbsent(topic, k -> new ArrayList<>()).add(handler);
    }

    // Simula una cola (para desarrollo local)
    public void shutdown() {
        executor.shutdown();
    }

    // Hook para integración con AWS SNS/SQS
    public void publishToCloud(String topic, String message) {
        // TODO: Implementar integración real con AWS SNS/SQS
    }
}
