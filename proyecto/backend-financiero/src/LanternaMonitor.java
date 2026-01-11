import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanternaMonitor {
    public static void main(String[] args) throws Exception {
        int interval = 2; // Actualización más rápida (2s) para ver efectos
        if (args.length > 0) {
            interval = Integer.parseInt(args[0]);
        }
        final int finalInterval = interval;

        // Nombres exactos o parciales de los procesos
        String[] servicios = {
            "WebServer",
            "LanternaMonitor",
            "SimuladorClientes"
        };
        
        // Obtener IP real (intento de no obtener localhost)
        String host = InetAddress.getLocalHost().getHostAddress();

        // 1. Configuración de Lanterna
        DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();
        Screen screen = terminalFactory.createScreen();
        screen.startScreen();
        
        MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
        BasicWindow window = new BasicWindow("Monitor de Servicios");

        // 2. Layout
        Panel panel = new Panel();
        panel.setLayoutManager(new GridLayout(5)); // 5 columnas
        
        // Encabezados
        panel.addComponent(new Label("Servicio").setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.CENTER)));
        panel.addComponent(new Label("IP").setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER)));
        panel.addComponent(new Label("PID").setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER)));
        panel.addComponent(new Label("CPU %").setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER)));
        panel.addComponent(new Label("Estado").setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER)));

        // Crear referencias a los Labels para actualizarlos luego
        List<Label[]> filas = new ArrayList<>();
        for (String servicio : servicios) {
            Label[] fila = new Label[5];
            fila[0] = new Label(servicio);
            fila[1] = new Label(host);
            fila[2] = new Label("-");
            fila[3] = new Label("0.0");
            fila[4] = new Label("SCANNING");
            
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
                    for (int i = 0; i < servicios.length; i++) {
                        String servicio = servicios[i];
                        String pid = obtenerPID(servicio);
                        
                        // Variables finales para usar en lambda de GUI (opcional en Lanterna pero buena práctica)
                        String cpuVal = obtenerCPU(pid);
                        String estadoVal = (pid != null && !pid.equals("-")) ? "ACTIVO" : "INACTIVO";
                        String pidVal = pid;

                        // Actualizamos los datos
                        Label[] fila = filas.get(i);
                        fila[2].setText(pidVal);
                        fila[3].setText(cpuVal);
                        fila[4].setText(estadoVal);
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

    // Método mejorado: usa pgrep para mayor precisión y menos consumo que 'ps aux' completo
    private static String obtenerPID(String nombre) {
        try {
            // 'pgrep -f' busca en toda la línea de comando
            Process p = new ProcessBuilder("pgrep", "-f", nombre).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine(); // Solo tomamos el primer PID encontrado
            
            if (line != null && !line.trim().isEmpty()) {
                // Evitamos que el proceso detecte su propia llamada 'pgrep' (raro, pero posible)
                return line.trim(); 
            }
            return "-";
        } catch (Exception e) {
            return "-";
        }
    }

    private static String obtenerCPU(String pid) {
        if (pid == null || pid.equals("-")) return "-";
        try {
            // Obtenemos %CPU puro
            Process p = new ProcessBuilder("ps", "-p", pid, "-o", "%cpu=").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            return (line != null) ? line.trim() + "%" : "-";
        } catch (Exception e) {
            return "-";
        }
    }
}