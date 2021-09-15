package cz.inovatika.sdnnt.services.exceptions;

/**
 * Expired pswd/login token
 */
public class UserControlerExpiredTokenException extends Exception{

    public UserControlerExpiredTokenException() {
    }

    public UserControlerExpiredTokenException(String message) {
        super(message);
    }

    public UserControlerExpiredTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
