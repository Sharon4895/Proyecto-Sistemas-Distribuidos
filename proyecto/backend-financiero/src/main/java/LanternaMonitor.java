import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class LanternaMonitor {
    public static void main(String[] args) throws Exception {
        int interval = 2; // Actualización más rápida (2s) para ver efectos
        if (args.length > 0) {
            interval = Integer.parseInt(args[0]);
        }
        final int finalInterval = interval;

        // Leer servicios y URLs desde config.lanterna.properties
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("config.lanterna.properties")) {
            props.load(fis);
        } catch (Exception e) {
            System.err.println("No se pudo leer config.lanterna.properties: " + e.getMessage());
            return;
        }
        List<String> servicios = new ArrayList<>();
        List<String> urls = new ArrayList<>();
        for (String key : props.stringPropertyNames()) {
            if (key.endsWith(".url")) {
                String nombre = key.substring(0, key.length() - 4); // quitar ".url"
                servicios.add(nombre);
                urls.add(props.getProperty(key));
            }
        }

        // 1. Configuración de Lanterna
        DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();
        Screen screen = terminalFactory.createScreen();
        screen.startScreen();
        
        MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
        BasicWindow window = new BasicWindow("Monitor de Servicios");

        // 2. Layout
        Panel panel = new Panel();
        panel.setLayoutManager(new GridLayout(4)); // 4 columnas: Servicio, URL, Estado, CPU
        // Encabezados
        panel.addComponent(new Label("Servicio").setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.CENTER)));
        panel.addComponent(new Label("URL").setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER)));
        panel.addComponent(new Label("Estado").setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER)));
        panel.addComponent(new Label("CPU %").setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER)));

        // Crear referencias a los Labels para actualizarlos luego
        List<Label[]> filas = new ArrayList<>();
        for (int i = 0; i < servicios.size(); i++) {
            Label[] fila = new Label[4];
            fila[0] = new Label(servicios.get(i));
            fila[1] = new Label(urls.get(i));
            fila[2] = new Label("SCANNING");
            fila[3] = new Label("-");
            for (Label l : fila) panel.addComponent(l);
            filas.add(fila);
        }
        
        // Botón de salida para cerrar limpiamente
        panel.addComponent(new Label(" ")); // Espaciador
        panel.addComponent(new Button("Salir", window::close));
        
        window.setComponent(panel);

        // 3. Hilo de actualización (CORRECCIÓN: Iniciado ANTES del bucle GUI)
        Thread updateThread = new Thread(() -> {
            while (true) {
                try {
                    for (int i = 0; i < servicios.size(); i++) {
                        String url = urls.get(i);
                        String estado = comprobarHTTP(url) ? "UP" : "DOWN";
                        String cpu = obtenerCPU(url);
                        Label[] fila = filas.get(i);
                        fila[2].setText(estado);
                        fila[3].setText(cpu);
                    }
                    // Forzamos actualización visual si la GUI está inactiva esperando input
                    try {
                        if (gui.getActiveWindow() != null) {
                            gui.updateScreen();
                        }
                    } catch (Exception ex) {
                        // Ignorar error si la ventana se cerró
                    }
                    TimeUnit.SECONDS.sleep(finalInterval);
                } catch (InterruptedException e) {
                    break; // Salir del hilo si es interrumpido
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        // Configurar como Daemon para que muera si el main muere
        updateThread.setDaemon(true); 
        updateThread.start(); 

        // 4. Bloqueo del Hilo Principal (Loop de la GUI)
        gui.addWindowAndWait(window);
    }

    // Comprueba si la URL responde HTTP 2xx/3xx
    private static boolean comprobarHTTP(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            return code >= 200 && code < 400;
        } catch (Exception e) {
            return false;
        }
    }

    // Intenta obtener el uso de CPU de la instancia vía endpoint /cpu
    private static String obtenerCPU(String urlStr) {
        try {
            URL url = new URL(urlStr + (urlStr.endsWith("/") ? "cpu" : "/cpu"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            if (code >= 200 && code < 400) {
                Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                String cpu = s.hasNext() ? s.next().trim() : "-";
                s.close();
                return cpu;
            }
            return "-";
        } catch (Exception e) {
            return "-";
        }
    }
}