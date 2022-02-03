package cz.inovatika.sdnnt.services.impl.users.validations;

import cz.inovatika.sdnnt.services.impl.users.UserValidation;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EmptyFieldsValidation  extends UserValidation {

    private List<String> fields;

    public EmptyFieldsValidation(JSONObject jsonObject, String ... fields) {
        super(jsonObject);
        this.fields = new ArrayList<>(Arrays.asList(fields));
    }


    @Override
    public UserValidationResult validate() {
        List<String> errorFields = new ArrayList<>();
        for (String fieldId :  this.fields) {
            if (this.jsonObject.has(fieldId)) {
                String string = this.jsonObject.getString(fieldId);
                if (string == null || string.trim().equals("")) {
                    errorFields.add(fieldId);
                }
            } else {
                errorFields.add(fieldId);
            }
        }
        return errorFields.isEmpty() ? new UserValidationResult(true) : new UserValidationResult(errorFields, false);
    }
}
