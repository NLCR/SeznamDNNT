package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.indexer.models.User;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.mail.EmailException;

import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import java.io.IOException;
import java.rmi.ServerException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Basic mail service which should cover all possible scenarios
 */
public interface MailService {


    public void sendResetPasswordRequest(User user, Pair<String, String> recipient, String requestToken) throws IOException, EmailException;
    public void sendRegistrationMail(User user, Pair<String, String> recipient, String generatedPswd, String requestToken) throws IOException, EmailException;
    public void sendResetPasswordMail(User user, Pair<String, String> recpipient, String generatedPswd) throws  IOException, EmailException;
    public void sendNotificationEmail(Pair<String, String> recpipient, List<Map<String,String>> data) throws  IOException, EmailException;



    public void sendHTMLEmail(Pair<String, String> from, List<Pair<String,String>> recipients, String subject, String text) throws ServerException, EmailException;
    public  void sendMail(Pair<String, String> from, List<Pair<String, String>> recipients, String subject, String text) throws ServerException, EmailException;



    public class SMTPAuthenticator extends javax.mail.Authenticator {

        private String name;
        private String pass;

        public SMTPAuthenticator(String name, String pass) {
            super();
            this.name = name;
            this.pass = pass;
        }

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(name, pass);
        }
    }
}
