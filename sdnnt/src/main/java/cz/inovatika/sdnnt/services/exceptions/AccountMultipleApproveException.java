package cz.inovatika.sdnnt.services.exceptions;

public class AccountMultipleApproveException extends BaseException{
    public AccountMultipleApproveException(String key, String message) {
        super(key, message);
    }
}
