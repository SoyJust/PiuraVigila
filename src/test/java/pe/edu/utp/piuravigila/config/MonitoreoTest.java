package pe.edu.utp.piuravigila.config;

import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.spi.FilterReply;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Semana 16 · logs estructurados y métricas (TC-MON). */
class MonitoreoTest {

    private static String recurso(String nombre) throws Exception {
        try (InputStream in = MonitoreoTest.class.getClassLoader().getResourceAsStream(nombre)) {
            assertNotNull(in, nombre + " debe existir");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test @DisplayName("TC-MON-01 · tres archivos de log: texto, JSON (ECS) y solo WARN/ERROR, con rotación")
    void archivos_de_log() throws Exception {
        String xml = recurso("logback-spring.xml");
        assertTrue(xml.contains("<file>${DIR}/piuravigila.log</file>"));
        assertTrue(xml.contains("<file>${DIR}/piuravigila.json</file>"));
        assertTrue(xml.contains("StructuredLogEncoder") && xml.contains("<format>ecs</format>"), "JSON estructurado");
        assertTrue(xml.contains("<file>${DIR}/errores.log</file>") && xml.contains("ThresholdFilter"));
        assertEquals(3, xml.split("SizeAndTimeBasedRollingPolicy", -1).length - 1, "los tres archivos rotan");
    }

    @Test @DisplayName("TC-MON-02 · errores.log acepta WARN y ERROR y descarta INFO")
    void errores_solo_warn_y_error() throws Exception {
        LoggerContext ctx = new LoggerContext();
        ctx.putProperty("DIR", System.getProperty("java.io.tmpdir") + "/piuravigila-test-logs");
        String xml = recurso("logback-spring.xml")
                .replaceAll("(?s)<include[^>]*/>", "")
                .replaceAll("(?s)<springProperty[^>]*/>", "")
                .replaceAll("(?s)<appender name=\"JSON\".*?</appender>", "")
                .replace("<appender-ref ref=\"JSON\"/>", "")
                .replace("<appender-ref ref=\"CONSOLE\"/>", "")
                .replace("${FILE_LOG_PATTERN}", "%d %level %logger %msg%n");
        JoranConfigurator conf = new JoranConfigurator();
        conf.setContext(ctx);
        conf.doConfigure(new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        Logger raiz = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
        Appender<?> errores = raiz.getAppender("ERRORES");
        assertInstanceOf(RollingFileAppender.class, errores);
        @SuppressWarnings("unchecked")
        Appender<ch.qos.logback.classic.spi.ILoggingEvent> ap = (Appender<ch.qos.logback.classic.spi.ILoggingEvent>) errores;
        assertEquals(FilterReply.DENY, ap.getFilterChainDecision(evento(raiz, Level.INFO)));
        assertNotEquals(FilterReply.DENY, ap.getFilterChainDecision(evento(raiz, Level.WARN)));
        assertNotEquals(FilterReply.DENY, ap.getFilterChainDecision(evento(raiz, Level.ERROR)));
        assertEquals(Level.INFO, ctx.getLogger("pe.edu.utp.piuravigila").getLevel());
        ctx.stop();
    }

    private static LoggingEvent evento(Logger l, Level nivel) {
        return new LoggingEvent(Logger.FQCN, l, nivel, "prueba", null, null);
    }

    @Test @DisplayName("TC-MON-03 · métricas por JMX (JConsole/VisualVM) y por HTTP solo health e info")
    void metricas_por_jmx() throws Exception {
        Properties p = new Properties();
        p.load(new java.io.StringReader(recurso("application.properties")));
        assertEquals("true", p.getProperty("spring.jmx.enabled"));
        assertTrue(p.getProperty("management.endpoints.jmx.exposure.include").contains("metrics"));
        assertEquals("health,info", p.getProperty("management.endpoints.web.exposure.include"));
    }
}
