package biz.donvi.jakesRTP;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Objects;

/**
 * An exception for organizational purposes.
 */
public class JrtpBaseException extends Exception {
    private Component componentMessage;

    public JrtpBaseException(final Component message) {
        super(PlainTextComponentSerializer.plainText().serialize(message));
        componentMessage = message;
    }

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

    /**
     * Keep configured formatting for players, while getMessage() remains plain text for logs.
     */
    public static Component userMessage(final Throwable error) {
        if (error instanceof final JrtpBaseException rtpError && rtpError.componentMessage != null)
            return rtpError.componentMessage;
        return Component.text(Objects.toString(error.getMessage(), error.getClass().getSimpleName()));
    }

    static class PluginDisabledException extends JrtpBaseException {
    }

    public static class NotPermittedException extends JrtpBaseException {
        public NotPermittedException(final Component message) {
            super(message);
        }

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
