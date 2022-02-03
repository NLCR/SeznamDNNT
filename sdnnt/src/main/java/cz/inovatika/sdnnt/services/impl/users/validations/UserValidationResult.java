package cz.inovatika.sdnnt.services.impl.users.validations;

import java.util.List;

public class UserValidationResult {

    private List<String> errorFields;
    private boolean result;

    public UserValidationResult(List<String> errorFields, boolean result) {
        this.errorFields = errorFields;
        this.result = result;
    }

    public UserValidationResult(boolean result) {
        this.result = result;
    }


    public List<String> getErrorFields() {
        return errorFields;
    }

    public boolean getResult() {
        return result;
    }
}
