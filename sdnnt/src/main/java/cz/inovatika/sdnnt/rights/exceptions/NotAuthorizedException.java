package cz.inovatika.sdnnt.rights.exceptions;

public class NotAuthorizedException extends Exception{

    public NotAuthorizedException() {
    }

    public NotAuthorizedException(String message) {
        super(message);
    }

    public NotAuthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
