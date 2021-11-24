package cz.inovatika.sdnnt.services.exceptions;

/**
 * Genericka vyjimka pri praci s zadostmi
 */
public class AccountException extends BaseException {

    public AccountException(String key, String message) {
        super(key, message);
    }
}

