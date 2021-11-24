package cz.inovatika.sdnnt.services.exceptions;

/**
 * Konflikt verzi, prave ukladana zadasto byla zmenena nekym jinym
 */
public class ConflictException extends BaseException {

    public ConflictException(String key, String message) {
        super(key, message);
    }
}
