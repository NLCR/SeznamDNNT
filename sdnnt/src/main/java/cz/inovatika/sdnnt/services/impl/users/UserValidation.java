package cz.inovatika.sdnnt.services.impl.users;

import cz.inovatika.sdnnt.services.impl.users.validations.UserValidationResult;
import org.json.JSONObject;

import java.util.List;

public abstract class UserValidation {

    protected JSONObject jsonObject;

    public UserValidation(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public abstract UserValidationResult validate();
}
