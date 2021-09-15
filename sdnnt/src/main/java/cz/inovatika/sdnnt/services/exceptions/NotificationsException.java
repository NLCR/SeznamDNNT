package cz.inovatika.sdnnt.services.exceptions;

public class NotificationsException extends Exception {

    public NotificationsException() {
    }

    public NotificationsException(String message) {
        super(message);
    }

    public NotificationsException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotificationsException(Throwable cause) {
        super(cause);
    }
}
