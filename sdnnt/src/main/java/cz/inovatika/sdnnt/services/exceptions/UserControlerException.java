package cz.inovatika.sdnnt.services.exceptions;

/**
 * Exception
 */
public class UserControlerException extends Exception{

    public UserControlerException() { }

    public UserControlerException(String message) {
        super(message);
    }

    public UserControlerException(Throwable cause) {
        super(cause);
    }
}
