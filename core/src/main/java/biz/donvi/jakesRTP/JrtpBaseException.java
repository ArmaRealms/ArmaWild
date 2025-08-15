package biz.donvi.jakesRTP;

/**
 * An exception for organizational purposes.
 */
public class JrtpBaseException extends Exception {

    public JrtpBaseException() {
        super();
    }

    public JrtpBaseException(final String message) {
        super(message);
    }

    public JrtpBaseException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public JrtpBaseException(final Throwable cause) {
        super(cause);
    }

    static class PluginDisabledException extends JrtpBaseException {
    }

    public static class NotPermittedException extends JrtpBaseException {
        public NotPermittedException(final String message) {
            super(message);
        }
    }

    static class ConfigurationException extends JrtpBaseException {
        public ConfigurationException(final String message) {
            super(message);
        }
    }
}
