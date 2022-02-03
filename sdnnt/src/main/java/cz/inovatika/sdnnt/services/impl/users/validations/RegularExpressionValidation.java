package cz.inovatika.sdnnt.services.impl.users.validations;

import cz.inovatika.sdnnt.services.impl.users.UserValidation;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class RegularExpressionValidation extends UserValidation {

    //static  final Pattern EMAIL_REGEX = null; //Pattern.compile("[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", Pattern.CASE_INSENSITIVE);
    private List<String> fields;

    private Pattern regularExpression = null; //Pattern.compile("[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", Pattern.CASE_INSENSITIVE);

    public RegularExpressionValidation(JSONObject jsonObject, String regularExpression, String ... flds) {
        super(jsonObject);
        this.fields = new ArrayList<>(Arrays.asList(flds));
        this.regularExpression = Pattern.compile(regularExpression);
    }

    public RegularExpressionValidation(JSONObject jsonObject, Pattern regularExpression,  String ... flds) {
        super(jsonObject);
        this.fields =  new ArrayList<>(Arrays.asList(flds));
        this.regularExpression = regularExpression;
    }

    public boolean isValid(String value) {
        return regularExpression.matcher(value).matches();
    }

    @Override
    public UserValidationResult validate() {
        List<String> invalidFields = new ArrayList<>();
        for (String fieldId :  this.fields) {
            if (this.jsonObject.has(fieldId)) {
                String string = this.jsonObject.getString(fieldId);
                if (string != null && !isValid(string)) {
                    invalidFields.add(string);
                    //return new UserValidationResult(fieldId, false);
                }
            }
        }
        return  invalidFields.isEmpty() ? new UserValidationResult(true) : new UserValidationResult(fields, false);
        //return new UserValidationResult(false);
    }
}
