package cz.inovatika.sdnnt.services.impl.users;

import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.impl.users.validations.EmailValidation;
import cz.inovatika.sdnnt.services.impl.users.validations.EmptyFieldsValidation;
import cz.inovatika.sdnnt.services.impl.users.validations.RegularExpressionValidation;
import cz.inovatika.sdnnt.services.impl.users.validations.UserValidationResult;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import static cz.inovatika.sdnnt.utils.ServletsSupport.errorJson;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

public class UsersUtils {

    private UsersUtils() {}

    public static User findOneUser(SolrClient solr, String q) throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery(q)
                .setRows(1);

        QueryResponse usersResult = solr.query("users", query);

        long numFound = usersResult.getResults().getNumFound();
        if (numFound >0 ) {
            SolrDocument document = usersResult.getResults().get(0);
            return User.fromSolrDocument(document);
        } else {
            return null;
        }
    }


    public static User toTOObject(User user) {
        if (user != null) {
            User toObject = new User();
            toObject.setUsername(user.getUsername());
            toObject.setJmeno(user.getJmeno());
            toObject.setPrijmeni(user.getPrijmeni());
            toObject.setApikey(user.getApikey());
            toObject.setResetPwdToken(user.getResetPwdToken());
            toObject.setResetPwdExpiration(  user.getResetPwdExpiration());
            toObject.setEmail( user.getEmail()) ;
            toObject.setNotifikaceInterval(user.getNotifikaceInterval());
            toObject.setNositel(user.getNositel());
            toObject.setRole(user.getRole());
            toObject.setInstitution( user.getInstitution());
            toObject.setIco(user.getIco());
            toObject.setPoznamka(user.getPoznamka());
            toObject.setTyp(user.getTyp());
            toObject.setMesto(user.getMesto());
            toObject.setAdresa(user.getAdresa());
            toObject.setUlice(user.getUlice());
            toObject.setCislo(user.getCislo());
            toObject.setTelefon(user.getTelefon());
            toObject.setPsc(user.getPsc());
            toObject.setNazevSpolecnosti(user.getNazevSpolecnosti());

            return toObject;
        } else {
            return null;
        }
    }

    @FunctionalInterface
    public static interface ValidationConsumer {
        public void validationError(List<String> errorFields, String validationId);
    }

    public static void userValidation(JSONObject savingUser, ValidationConsumer validationConsumer) {
        EmptyFieldsValidation emptyFields = new EmptyFieldsValidation(savingUser,
                User.EMAIL_KEY,
                User.JMENO_KEY,
                User.PRIJMENI_KEY,
                User.USERNAME_KEY
        );

        UserValidationResult emptyFieldsValidation = emptyFields.validate();
        if (emptyFieldsValidation != null && !emptyFieldsValidation.getResult()) {
            List<String> errorFields = emptyFieldsValidation.getErrorFields();
            validationConsumer.validationError(errorFields, EmptyFieldsValidation.class.getName());
        }

        EmailValidation emailValidation = new EmailValidation(savingUser, User.EMAIL_KEY);
        UserValidationResult emailValidationErrors = emailValidation.validate();
        if (emailValidationErrors != null && !emailValidationErrors.getResult()) {
            if (!emailValidationErrors.getErrorFields().isEmpty()) {
                validationConsumer.validationError(emailValidationErrors.getErrorFields(), EmailValidation.class.getName());
            }
        }

        RegularExpressionValidation icoValidation = new RegularExpressionValidation(savingUser, "^\\d{2}\\s*\\d{2}\\s*\\d{2}\\s*\\d{2}$", User.ICO_KEY);
        UserValidationResult icoValidationResults = icoValidation.validate();
        if (icoValidationResults != null && !icoValidationResults.getResult()) {
            if (!icoValidationResults.getErrorFields().isEmpty()) {
                validationConsumer.validationError(icoValidationResults.getErrorFields(), RegularExpressionValidation.class.getName());
            }
        }

        RegularExpressionValidation phoneValiation = new RegularExpressionValidation(savingUser, "^\\+?(\\d{3,4})?(\\s*)(\\d{3}\\s*\\d{3}\\s*\\d{3})$", User.TELEFON_KEY);
        UserValidationResult phoneValiationResults = phoneValiation.validate();
        if (phoneValiationResults != null && !phoneValiationResults.getResult()) {
            if (!phoneValiationResults.getErrorFields().isEmpty()) {
                validationConsumer.validationError(phoneValiationResults.getErrorFields(), RegularExpressionValidation.class.getName());
            }
        }
        //^\+?(\d{3})?(\s*)(\d{2})$
        RegularExpressionValidation pscNumberValidation = new RegularExpressionValidation(savingUser, "^^\\+?(\\d{3})?(\\s*)(\\d{2})$", User.PSC_KEY);
        UserValidationResult pcsNumberValidationResult = pscNumberValidation.validate();
        if (pcsNumberValidationResult != null && !pcsNumberValidationResult.getResult()) {
            if (!pcsNumberValidationResult.getErrorFields().isEmpty()) {
                validationConsumer.validationError(pcsNumberValidationResult.getErrorFields(), RegularExpressionValidation.class.getName());
            }
        }

    }
}
