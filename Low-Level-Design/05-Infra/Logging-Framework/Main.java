import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// =====================================================================
// 1. LOG LEVEL — severity is EXPLICIT, not the enum ordinal
// =====================================================================
enum LogLevel {
    DEBUG(10),
    INFO(20),
    WARN(30),
    ERROR(40);

    private final int severity;

    LogLevel(int severity) {
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }

    // A record is emitted only if its severity >= configured threshold.
    public boolean isAtLeast(LogLevel threshold) {
        return this.severity >= threshold.getSeverity();
    }
}

// =====================================================================
// 2. LOG RECORD — the immutable event travelling through the pipeline
// =====================================================================
class LogRecord {
    private final LocalDateTime timestamp;
    private final LogLevel level;
    private final String loggerName;
    private final String threadName;
    private final String message;
    private final Throwable error;

    public LogRecord(LogLevel level, String loggerName, String message, Throwable error) {
        this.timestamp = LocalDateTime.now();
        this.level = level;
        this.loggerName = loggerName;
        this.threadName = Thread.currentThread().getName();
        this.message = message;
        this.error = error;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getError() {
        return error;
    }
}

// =====================================================================
// 3. FORMATTER — how a record becomes text (swappable: plain / JSON)
// =====================================================================
interface LogFormatter {
    String format(LogRecord record);
}

class SimpleFormatter implements LogFormatter {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append(record.getTimestamp().format(TIME))
          .append(" [").append(record.getThreadName()).append("]")
          .append(" ").append(String.format("%-5s", record.getLevel().name()))
          .append(" ").append(record.getLoggerName())
          .append(" - ").append(record.getMessage());
        if (record.getError() != null) {
            sb.append(" | cause=").append(record.getError());
        }
        return sb.toString();
    }
}

class JsonFormatter implements LogFormatter {
    @Override
    public String format(LogRecord record) {
        return "{\"ts\":\"" + record.getTimestamp() + "\","
             + "\"level\":\"" + record.getLevel() + "\","
             + "\"logger\":\"" + record.getLoggerName() + "\","
             + "\"msg\":\"" + record.getMessage() + "\"}";
    }
}

// =====================================================================
// 4. APPENDER — where the formatted text goes (sink)
// =====================================================================
interface Appender {
    void append(LogRecord record);

    void close();
}

abstract class AbstractAppender implements Appender {
    private final LogFormatter formatter;
    private final LogLevel threshold; // per-sink filter, independent of the logger

    protected AbstractAppender(LogFormatter formatter, LogLevel threshold) {
        this.formatter = formatter;
        this.threshold = threshold;
    }

    @Override
    public final void append(LogRecord record) {
        if (!record.getLevel().isAtLeast(threshold)) {
            return;
        }
        write(formatter.format(record), record.getLevel());
    }

    protected abstract void write(String line, LogLevel level);

    @Override
    public void close() {
        // no-op by default
    }
}

class ConsoleAppender extends AbstractAppender {
    public ConsoleAppender(LogFormatter formatter, LogLevel threshold) {
        super(formatter, threshold);
    }

    @Override
    protected void write(String line, LogLevel level) {
        if (level == LogLevel.ERROR) {
            System.err.println(line);
        } else {
            System.out.println(line);
        }
    }
}

// File I/O is simulated with a StringBuilder so the sketch stays runnable
// anywhere. In production this wraps a BufferedWriter + rotation policy.
class FileAppender extends AbstractAppender {
    private final String fileName;
    private final StringBuilder buffer = new StringBuilder();
    private int lineCount = 0;

    public FileAppender(String fileName, LogFormatter formatter, LogLevel threshold) {
        super(formatter, threshold);
        this.fileName = fileName;
    }

    @Override
    protected synchronized void write(String line, LogLevel level) {
        buffer.append(line).append(System.lineSeparator());
        lineCount++;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLineCount() {
        return lineCount;
    }

    public synchronized String dump() {
        return buffer.toString();
    }

    @Override
    public void close() {
        System.out.println("[FileAppender] flushed " + lineCount + " line(s) to " + fileName);
    }
}

// =====================================================================
// 5. LOGGER — fan-out to a list of appenders after a level check
// =====================================================================
class Logger {
    private final String name;
    private volatile LogLevel level;
    private final List<Appender> appenders = new ArrayList<>();

    public Logger(String name, LogLevel level) {
        this.name = name;
        this.level = level;
    }

    public Logger addAppender(Appender appender) {
        appenders.add(appender);
        return this;
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    // Cheap guard so callers can skip expensive message construction.
    public boolean isEnabled(LogLevel candidate) {
        return candidate.isAtLeast(level);
    }

    public void log(LogLevel candidate, String message, Throwable error) {
        if (!isEnabled(candidate)) {
            return;
        }
        LogRecord record = new LogRecord(candidate, name, message, error);
        for (Appender appender : appenders) {
            appender.append(record);
        }
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }

    public void info(String message) {
        log(LogLevel.INFO, message, null);
    }

    public void warn(String message) {
        log(LogLevel.WARN, message, null);
    }

    public void error(String message) {
        log(LogLevel.ERROR, message, null);
    }

    public void error(String message, Throwable error) {
        log(LogLevel.ERROR, message, error);
    }

    public void close() {
        for (Appender appender : appenders) {
            appender.close();
        }
    }
}

// =====================================================================
// 6. LOGGER FACTORY — one logger instance per name (registry, not a
//    global mutable singleton). Loggers are still injected into classes.
// =====================================================================
class LoggerFactory {
    private static final Map<String, Logger> REGISTRY = new ConcurrentHashMap<>();
    private static LogLevel rootLevel = LogLevel.INFO;
    private static final List<Appender> DEFAULT_APPENDERS = new ArrayList<>();

    private LoggerFactory() {
    }

    public static void configure(LogLevel level, Appender... appenders) {
        rootLevel = level;
        DEFAULT_APPENDERS.clear();
        for (Appender appender : appenders) {
            DEFAULT_APPENDERS.add(appender);
        }
    }

    public static Logger getLogger(String name) {
        return REGISTRY.computeIfAbsent(name, key -> {
            Logger logger = new Logger(key, rootLevel);
            for (Appender appender : DEFAULT_APPENDERS) {
                logger.addAppender(appender);
            }
            return logger;
        });
    }

    public static Logger getLogger(Class<?> type) {
        return getLogger(type.getSimpleName());
    }
}

// =====================================================================
// 7. ALTERNATIVE: CHAIN OF RESPONSIBILITY (the "classic" interview answer)
//    Each handler owns ONE level and forwards everything else down the
//    chain. Kept here to contrast with the appender-list design above.
// =====================================================================
abstract class LogHandler {
    private final LogLevel handledLevel;
    private LogHandler next;

    protected LogHandler(LogLevel handledLevel) {
        this.handledLevel = handledLevel;
    }

    public LogHandler setNext(LogHandler next) {
        this.next = next;
        return next;
    }

    public void handle(LogLevel level, String message) {
        if (level == handledLevel) {
            write(message);
            return; // stop: exactly one handler owns each level
        }
        if (next != null) {
            next.handle(level, message);
        }
    }

    protected abstract void write(String message);
}

class DebugHandler extends LogHandler {
    public DebugHandler() {
        super(LogLevel.DEBUG);
    }

    @Override
    protected void write(String message) {
        System.out.println("[CoR][DEBUG] " + message);
    }
}

class InfoHandler extends LogHandler {
    public InfoHandler() {
        super(LogLevel.INFO);
    }

    @Override
    protected void write(String message) {
        System.out.println("[CoR][INFO ] " + message);
    }
}

class ErrorHandler extends LogHandler {
    public ErrorHandler() {
        super(LogLevel.ERROR);
    }

    @Override
    protected void write(String message) {
        System.out.println("[CoR][ERROR] " + message);
    }
}

// =====================================================================
// 8. CLIENT — a class that receives its logger by injection
// =====================================================================
class PaymentService {
    private final Logger logger;

    public PaymentService(Logger logger) { // dependency injection => testable
        this.logger = logger;
    }

    public void charge(String orderId, double amount) {
        logger.debug("entering charge() order=" + orderId);
        logger.info("charging order=" + orderId + " amount=" + amount);
        if (amount > 1000) {
            logger.warn("unusually large amount for order=" + orderId);
        }
        if (amount < 0) {
            logger.error("negative amount for order=" + orderId,
                    new IllegalArgumentException("amount=" + amount));
        }
    }
}

public class Main {
    public static void main(String[] args) {
        LogFormatter simple = new SimpleFormatter();

        ConsoleAppender console = new ConsoleAppender(simple, LogLevel.DEBUG);
        FileAppender file = new FileAppender("app.log", simple, LogLevel.WARN); // sink-level filter
        FileAppender audit = new FileAppender("audit.json", new JsonFormatter(), LogLevel.ERROR);

        LoggerFactory.configure(LogLevel.INFO, console, file, audit);
        Logger logger = LoggerFactory.getLogger(PaymentService.class);

        System.out.println("=== Logger threshold = INFO ===");
        PaymentService service = new PaymentService(logger);
        service.charge("ORD-1", 250.0);    // DEBUG dropped by logger threshold
        service.charge("ORD-2", 5000.0);   // WARN reaches console + app.log
        service.charge("ORD-3", -10.0);    // ERROR reaches console + app.log + audit

        System.out.println();
        System.out.println("=== Lower threshold to DEBUG (same logger instance) ===");
        logger.setLevel(LogLevel.DEBUG);
        service.charge("ORD-4", 99.0);     // DEBUG now visible on console only

        System.out.println();
        System.out.println("=== Same logger name returns the same instance ===");
        System.out.println("cached == original ? "
                + (LoggerFactory.getLogger("PaymentService") == logger));

        System.out.println();
        System.out.println("=== Simulated file contents: " + file.getFileName() + " ===");
        System.out.print(file.dump());

        System.out.println("=== Simulated file contents: " + audit.getFileName() + " ===");
        System.out.print(audit.dump());

        System.out.println();
        System.out.println("=== Chain of Responsibility variant ===");
        LogHandler chain = new DebugHandler();
        chain.setNext(new InfoHandler()).setNext(new ErrorHandler());
        chain.handle(LogLevel.DEBUG, "cache warm-up finished");
        chain.handle(LogLevel.INFO, "server started on :8080");
        chain.handle(LogLevel.ERROR, "db connection refused");
        chain.handle(LogLevel.WARN, "no handler owns WARN -> silently dropped");

        System.out.println();
        logger.close();
    }
}
