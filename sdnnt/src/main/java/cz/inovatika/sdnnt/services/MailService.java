package cz.inovatika.sdnnt.services;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.mail.EmailException;

import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.swing.text.html.Option;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.ServerException;
import java.util.List;

public interface MailService {

    public Session getSession(String name, String pswd);

    public void sendResetPasswordRequest(Pair<String,String> recipient, String requestToken) throws IOException, EmailException;

    public void sendRegistrationMail(Pair<String,String> recipient, String generatedPswd) throws IOException, EmailException;

    public void sendResetPasswordMail(Pair<String, String> recpipient, String generatedPswd) throws  IOException, EmailException;



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
