package cz.inovatika.sdnnt.index.exceptions;

public class MaximumIterationExceedException extends Exception {

    public MaximumIterationExceedException() {
    }

    public MaximumIterationExceedException(String message) {
        super(message);
    }

    public MaximumIterationExceedException(String message, Throwable cause) {
        super(message, cause);
    }

    public MaximumIterationExceedException(Throwable cause) {
        super(cause);
    }

    public MaximumIterationExceedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
