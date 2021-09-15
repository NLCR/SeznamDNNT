package cz.inovatika.sdnnt.services.exceptions;

/**
 * Invalid login/pwd token
 */
public class UserControlerInvalidPwdTokenException extends Exception {

    public UserControlerInvalidPwdTokenException() {
    }

    public UserControlerInvalidPwdTokenException(String message) {
        super(message);
    }

    public UserControlerInvalidPwdTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
