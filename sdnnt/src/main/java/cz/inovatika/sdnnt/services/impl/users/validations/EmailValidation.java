package cz.inovatika.sdnnt.services.impl.users.validations;

import cz.inovatika.sdnnt.services.impl.users.UserValidation;
import org.json.JSONObject;

import java.util.regex.Pattern;

public class EmailValidation extends RegularExpressionValidation {

    //Pattern.compile("[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", Pattern.CASE_INSENSITIVE);

    public EmailValidation(JSONObject jsonObject, String... flds) {
        super(jsonObject, Pattern.compile("[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", Pattern.CASE_INSENSITIVE), flds);
    }

}
