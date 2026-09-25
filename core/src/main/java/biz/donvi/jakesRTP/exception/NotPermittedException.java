package biz.donvi.jakesRTP.exception;

import net.kyori.adventure.text.Component;

public class NotPermittedException extends JrtpBaseException {
    public NotPermittedException(final Component message) {
        super(message);
    }

    public NotPermittedException(final String message) {
        super(message);
    }
}
