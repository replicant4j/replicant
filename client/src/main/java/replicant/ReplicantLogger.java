package replicant;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Log abstraction for framework.
 */
final class ReplicantLogger {
    private static final Logger c_logger = "console".equals(ReplicantConfig.loggerType())
            ? new ConsoleLogger()
            : "proxy".equals(ReplicantConfig.loggerType()) ? new ProxyLogger() : new NoopLogger();

    private ReplicantLogger() {}

    /**
     * Log a message with an optional exception.
     */
    static void log(@NonNull final String message, @Nullable final Throwable throwable) {
        c_logger.log(message, throwable);
    }

    @NonNull
    static Logger getLogger() {
        return c_logger;
    }

    /**
     * Abstraction used to provide logging for Replicant system.
     * This abstraction is used to support compile time constants during GWT and/or closure
     * compiler phases and thus allow elimination of code during production variants of the runtime.
     */
    interface Logger {
        void log(@NonNull String message, @Nullable Throwable throwable);
    }

    /**
     * The noop log provider implementation.
     */
    private static final class NoopLogger implements Logger {
        @Override
        public void log(@NonNull final String message, @Nullable final Throwable throwable) {}
    }

    /**
     * The console log provider implementation.
     */
    private static final class ConsoleLogger extends AbstractConsoleLogger {
        @GwtIncompatible
        @Override
        public void log(@NonNull final String message, @Nullable final Throwable throwable) {
            System.out.println(message);
            if (null != throwable) {
                throwable.printStackTrace(System.out);
            }
        }
    }

    @JsType(isNative = true, name = "window.console", namespace = JsPackage.GLOBAL)
    private static final class NativeJsLoggerUtil {
        @JsMethod
        public static native void log(Object message);
    }

    /**
     * The console log provider implementation providing javascript based console logging.
     */
    private abstract static class AbstractConsoleLogger implements Logger {
        @Override
        public void log(@NonNull final String message, @Nullable final Throwable throwable) {
            NativeJsLoggerUtil.log(message);
            if (null != throwable) {
                NativeJsLoggerUtil.log(throwable);
            }
        }
    }

    /**
     * The log provider implementation that forwards to another logger if present.
     */
    static final class ProxyLogger implements Logger {
        @Nullable
        private Logger _logger;

        @Nullable
        Logger getLogger() {
            return _logger;
        }

        void setLogger(@Nullable final Logger logger) {
            _logger = logger;
        }

        @Override
        public void log(@NonNull final String message, @Nullable final Throwable throwable) {
            if (null != _logger) {
                _logger.log(message, throwable);
            }
        }
    }
}
