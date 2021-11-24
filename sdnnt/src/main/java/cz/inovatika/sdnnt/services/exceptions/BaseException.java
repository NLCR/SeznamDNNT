package cz.inovatika.sdnnt.services.exceptions;


public abstract  class BaseException extends Exception{

    private String key;
    private String message;

    public BaseException(String key, String message) {
        this.key = key;
        this.message = message;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
