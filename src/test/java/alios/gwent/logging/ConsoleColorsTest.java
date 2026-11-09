package alios.gwent.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.pattern.color.ANSIConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsoleColorsTest {

    @Test
    @DisplayName("Returns correct ANSI color code according to log level")
    void testColorCodes() {
        ConsoleColors colors = new ConsoleColors();
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        LoggingEvent errorEvent = new LoggingEvent("test", context.getLogger("test"), Level.ERROR, "err", null, null);
        assertEquals(ANSIConstants.BOLD + ANSIConstants.RED_FG, colors.getForegroundColorCode(errorEvent));

        LoggingEvent warnEvent = new LoggingEvent("test", context.getLogger("test"), Level.WARN, "warn", null, null);
        assertEquals(ANSIConstants.YELLOW_FG, colors.getForegroundColorCode(warnEvent));

        LoggingEvent infoEvent = new LoggingEvent("test", context.getLogger("test"), Level.INFO, "info", null, null);
        assertEquals(ANSIConstants.GREEN_FG, colors.getForegroundColorCode(infoEvent));

        LoggingEvent debugEvent = new LoggingEvent("test", context.getLogger("test"), Level.DEBUG, "debug", null, null);
        assertEquals(ANSIConstants.DEFAULT_FG, colors.getForegroundColorCode(debugEvent));
    }
}
