package cz.inovatika.sdnnt.services.impl.users;

import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.impl.users.validations.EmailValidation;
import cz.inovatika.sdnnt.services.impl.users.validations.EmptyFieldsValidation;
import cz.inovatika.sdnnt.services.impl.users.validations.RegularExpressionValidation;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class UserValidationTest {

    @Test
    public void testEmptyValidation() {
        AtomicBoolean errorDetect = new AtomicBoolean(false);

        User noName = new User();
        noName.setTyp(User.Typ.fyzickaOsoba.name());
        noName.setRole("user");
        noName.setUsername("username");
        noName.setJmeno(null);
        noName.setPrijmeni("Prijmeni");
        noName.setEmail("my.emaik@gmail.com");

        UsersUtils.userValidation(noName.toJSONObject(), (erroFields, validationId) -> {
            Assert.assertTrue(validationId.equals(EmptyFieldsValidation.class.getName()));
            Assert.assertTrue(erroFields.size() == 1);
            Assert.assertTrue(erroFields.get(0).equals(User.JMENO_KEY));
            errorDetect.set(true);
        });

        Assert.assertTrue(errorDetect.get());

        noName = new User();
        noName.setTyp(User.Typ.fyzickaOsoba.name());
        noName.setRole("user");
        noName.setUsername("username");
        noName.setJmeno("");
        noName.setPrijmeni("Prijmeni");
        noName.setEmail("my.emaik@gmail.com");

        errorDetect.set(false);
        UsersUtils.userValidation(noName.toJSONObject(), (erroFields, validationId) -> {
            Assert.assertTrue(validationId.equals(EmptyFieldsValidation.class.getName()));
            Assert.assertTrue(erroFields.size() == 1);
            Assert.assertTrue(erroFields.get(0).equals(User.JMENO_KEY));
            errorDetect.set(true);
        });
        Assert.assertTrue(errorDetect.get());


        User noSurname = new User();
        noSurname.setTyp(User.Typ.fyzickaOsoba.name());
        noSurname.setRole("user");
        noSurname.setUsername("username");
        noSurname.setJmeno("Pavel");
        noSurname.setPrijmeni(null);
        noSurname.setEmail("my.emaik@gmail.com");

        errorDetect.set(false);
        UsersUtils.userValidation(noSurname.toJSONObject(), (erroFields, validationId) -> {
            Assert.assertTrue(validationId.equals(EmptyFieldsValidation.class.getName()));
            Assert.assertTrue(erroFields.size() == 1);
            Assert.assertTrue(erroFields.get(0).equals(User.PRIJMENI_KEY));
            errorDetect.set(true);
        });
        Assert.assertTrue(errorDetect.get());


        User noFirsnameNoSurname = new User();
        noFirsnameNoSurname.setTyp(User.Typ.fyzickaOsoba.name());
        noFirsnameNoSurname.setRole("user");
        noFirsnameNoSurname.setUsername("username");
        noFirsnameNoSurname.setJmeno(null);
        noFirsnameNoSurname.setPrijmeni(null);
        noFirsnameNoSurname.setEmail("my.emaik@gmail.com");

        errorDetect.set(false);
        UsersUtils.userValidation(noFirsnameNoSurname.toJSONObject(), (erroFields, validationId) -> {
            Assert.assertTrue(validationId.equals(EmptyFieldsValidation.class.getName()));
            Assert.assertTrue(erroFields.size() == 2);
            Assert.assertTrue(erroFields.contains(User.PRIJMENI_KEY));
            Assert.assertTrue(erroFields.contains(User.JMENO_KEY));
            errorDetect.set(true);
        });
        Assert.assertTrue(errorDetect.get());
    }


    @Test
    public void testEmailValidation() {
        AtomicBoolean errorDetect = new AtomicBoolean(false);

        User emailValidation = new User();
        emailValidation.setTyp(User.Typ.fyzickaOsoba.name());
        emailValidation.setRole("user");
        emailValidation.setUsername("username");
        emailValidation.setJmeno("jmeno");
        emailValidation.setPrijmeni("prijmeni");
        emailValidation.setEmail("pavel.stastny at gmail.com");

        UsersUtils.userValidation(emailValidation.toJSONObject(), (erroFields, validationId) -> {
            Assert.assertTrue(validationId.equals(EmailValidation.class.getName()));
            Assert.assertTrue(erroFields.contains(User.EMAIL_KEY));
            errorDetect.set(true);
        });

        Assert.assertTrue(errorDetect.get());
    }

    @Test
    public void testPSCValidation() {
        AtomicBoolean errorDetect1 = new AtomicBoolean(false);

        User pscValidation = new User();
        pscValidation.setTyp(User.Typ.fyzickaOsoba.name());
        pscValidation.setRole("user");
        pscValidation.setUsername("username");
        pscValidation.setJmeno("jmeno");
        pscValidation.setPrijmeni("prijmeni");
        pscValidation.setEmail("pavel.stastny@gmail.com");
        pscValidation.setPsc("4444");

        UsersUtils.userValidation(pscValidation.toJSONObject(), (erroFields, validationId) -> {
            Assert.assertTrue(validationId.equals(RegularExpressionValidation.class.getName()));
            Assert.assertTrue(erroFields.contains(User.PSC_KEY));
            errorDetect1.set(true);
        });

        Assert.assertTrue(errorDetect1.get());

        AtomicBoolean errorDetect2 = new AtomicBoolean(false);
        pscValidation.setPsc("69602");
        UsersUtils.userValidation(pscValidation.toJSONObject(), (erroFields, validationId) -> {
            Assert.assertTrue(validationId.equals(RegularExpressionValidation.class.getName()));
            Assert.assertTrue(erroFields.contains(User.PSC_KEY));
            errorDetect2.set(true);
        });

        Assert.assertFalse(errorDetect2.get());
    }

    @Test
    public void testPhoneNumber() {
        AtomicBoolean errorDetect1 = new AtomicBoolean(false);

        User phoneValidation = new User();
        phoneValidation.setTyp(User.Typ.fyzickaOsoba.name());
        phoneValidation.setRole("user");
        phoneValidation.setUsername("username");
        phoneValidation.setJmeno("jmeno");
        phoneValidation.setPrijmeni("prijmeni");
        phoneValidation.setEmail("pavel.stastny@gmail.com");
        phoneValidation.setTelefon("60 916 946");

        UsersUtils.userValidation(phoneValidation.toJSONObject(), (erroFields, validationId) -> {
            Assert.assertTrue(validationId.equals(RegularExpressionValidation.class.getName()));
            Assert.assertTrue(erroFields.contains(User.TELEFON_KEY));
            errorDetect1.set(true);
        });

        Assert.assertTrue(errorDetect1.get());

        AtomicBoolean errorDetect2 = new AtomicBoolean(false);
        phoneValidation.setTelefon("605 916 946");
        UsersUtils.userValidation(phoneValidation.toJSONObject(), (erroFields, validationId) -> {
            Assert.assertTrue(validationId.equals(RegularExpressionValidation.class.getName()));
            Assert.assertTrue(erroFields.contains(User.TELEFON_KEY));
            errorDetect2.set(true);
        });

        Assert.assertFalse(errorDetect2.get());

        AtomicBoolean errorDetect3 = new AtomicBoolean(false);
        phoneValidation.setTelefon("0420 605 916 946");
        UsersUtils.userValidation(phoneValidation.toJSONObject(), (erroFields, validationId) -> {
            Assert.assertTrue(validationId.equals(RegularExpressionValidation.class.getName()));
            Assert.assertTrue(erroFields.contains(User.TELEFON_KEY));
            errorDetect3.set(true);
        });

        Assert.assertFalse(errorDetect3.get());

        AtomicBoolean errorDetect4 = new AtomicBoolean(false);
        phoneValidation.setTelefon("+420 605 916 946");
        UsersUtils.userValidation(phoneValidation.toJSONObject(), (erroFields, validationId) -> {
            Assert.assertTrue(validationId.equals(RegularExpressionValidation.class.getName()));
            Assert.assertTrue(erroFields.contains(User.PSC_KEY));
            errorDetect4.set(true);
        });

        Assert.assertFalse(errorDetect4.get());

        AtomicBoolean errorDetect5 = new AtomicBoolean(false);
        phoneValidation.setTelefon("+42 605 916 946");
        UsersUtils.userValidation(phoneValidation.toJSONObject(), (erroFields, validationId) -> {
            Assert.assertTrue(validationId.equals(RegularExpressionValidation.class.getName()));
            Assert.assertTrue(erroFields.contains(User.TELEFON_KEY));
            errorDetect5.set(true);
        });

        Assert.assertTrue(errorDetect5.get());

    }


    @Test
    public void testICOValidation() {
        AtomicBoolean errorDetect1 = new AtomicBoolean(false);

        User icoValidation = new User();
        icoValidation.setTyp(User.Typ.fyzickaOsoba.name());
        icoValidation.setRole("user");
        icoValidation.setUsername("username");
        icoValidation.setJmeno("jmeno");
        icoValidation.setPrijmeni("prijmeni");
        icoValidation.setEmail("pavel.stastny@gmail.com");
        icoValidation.setIco("2615406");

        UsersUtils.userValidation(icoValidation.toJSONObject(), (erroFields, validationId) -> {
            Assert.assertTrue(validationId.equals(RegularExpressionValidation.class.getName()));
            Assert.assertTrue(erroFields.contains(User.ICO_KEY));
            errorDetect1.set(true);
        });

        Assert.assertTrue(errorDetect1.get());

        AtomicBoolean errorDetect2 = new AtomicBoolean(false);
        icoValidation.setIco("26165406");
        UsersUtils.userValidation(icoValidation.toJSONObject(), (erroFields, validationId) -> {
            Assert.assertTrue(validationId.equals(RegularExpressionValidation.class.getName()));
            Assert.assertTrue(erroFields.contains(User.ICO_KEY));
            errorDetect2.set(true);
        });

        Assert.assertFalse(errorDetect2.get());
    }

}