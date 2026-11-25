// Uso (con la aplicación en marcha):
//   java --add-modules jdk.attach deploy\MetricasJmx.java <PID>     (PID en C:\PiuraVigila-deploypp\piuravigila.pid)
import com.sun.tools.attach.VirtualMachine;
import java.util.List;
import java.util.Map;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/** Lee por JMX las métricas que publica Actuator en el proceso desplegado (lo mismo que muestra JConsole). */
public class MetricasJmx {
    public static void main(String[] args) throws Exception {
        VirtualMachine vm = VirtualMachine.attach(args[0]);
        String url = vm.startLocalManagementAgent();
        vm.detach();
        try (JMXConnector c = JMXConnectorFactory.connect(new JMXServiceURL(url))) {
            MBeanServerConnection s = c.getMBeanServerConnection();
            System.out.println("MBeans de Actuator:");
            for (ObjectName n : s.queryNames(new ObjectName("org.springframework.boot:type=Endpoint,*"), null)) {
                System.out.println("  " + n);
            }
            ObjectName metrics = new ObjectName("org.springframework.boot:type=Endpoint,name=Metrics");
            Object nombres = s.invoke(metrics, "listNames", new Object[0], new String[0]);
            List<?> lista = (List<?>) ((Map<?, ?>) nombres).get("names");
            System.out.println("\nMétricas disponibles: " + lista.size());
            String[] clave = {"jvm.memory.used", "jvm.memory.max", "process.cpu.usage", "system.cpu.usage", "jvm.threads.live",
                    "http.server.requests", "hikaricp.connections.active", "hikaricp.connections.max", "process.uptime", "disk.free"};
            System.out.println("\nMétricas clave (valor actual):");
            for (String m : clave) {
                if (!lista.contains(m)) { System.out.println("  " + m + ": (no disponible)"); continue; }
                Map<?, ?> r = (Map<?, ?>) s.invoke(metrics, "metric", new Object[]{m, null},
                        new String[]{String.class.getName(), List.class.getName()});
                System.out.println("  " + m + " [" + r.get("baseUnit") + "]: " + r.get("measurements"));
            }
        }
    }
}
