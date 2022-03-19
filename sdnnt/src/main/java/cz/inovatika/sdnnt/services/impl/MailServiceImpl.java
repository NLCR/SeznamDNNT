package cz.inovatika.sdnnt.services.impl;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import cz.inovatika.sdnnt.InitServlet;
import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.MailService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.mail.*;
import org.json.JSONObject;

import java.io.*;
import java.rmi.ServerException;
import java.util.*;
import java.util.logging.Logger;

public class MailServiceImpl implements MailService  {

    public static final Logger LOGGER = Logger.getLogger(MailServiceImpl.class.getName());




    @Override
    public void sendResetPasswordRequest(User user, Pair<String, String> recepient, String token) throws IOException, EmailException {
        if (recepient != null) {
            HashMap<String, String> scopes = new HashMap<String, String>();
            scopes.put("user", recepient.getRight());
            scopes.put("token", token);
            if (user != null) {
                scopes.put("username", user.getUsername());
            }

            String path = InitServlet.CONFIG_DIR + File.separator + Options.getInstance().getString("textsDir")+File.separator+"mail_reset_link";
            LOGGER.info("Sending email: Reseting password request to "+recepient.getLeft());
            sendPeparedMail(recepient, path, Options.getInstance().getJSONObject("resetlink"), scopes);
        } else throw new EmailException("No recepient");

    }

    @Override
    public void sendResetPasswordMail(User user, Pair<String, String> recepient, String generatedPswd) throws IOException, EmailException {
        if (recepient != null) {
            HashMap<String, String> scopes = new HashMap<String, String>();
            scopes.put("user", recepient.getRight());
            scopes.put("password", generatedPswd);
            if (user != null) {
                scopes.put("username", user.getUsername());
            }

            String path = InitServlet.CONFIG_DIR + File.separator + Options.getInstance().getString("textsDir")+File.separator+"mail_reset_password";
            LOGGER.info("Sending email: Reseted password to "+recepient.getLeft());
            sendPeparedMail(recepient, path, Options.getInstance().getJSONObject("passwordreset"), scopes);
        } else throw new EmailException("No recepient");

    }

    @Override
    public void sendNotificationEmail(Pair<String, String> recepient, List<Map<String, String>> data) throws IOException, EmailException {
        if (recepient != null) {
            String path = InitServlet.CONFIG_DIR + File.separator + Options.getInstance().getString("textsDir")+File.separator+"mail_notification";

            Map scope = new HashMap();
            scope.put("user", recepient.getRight());
            scope.put("notifications", data);

            sendPeparedMail(recepient, path, Options.getInstance().getJSONObject("notificationemail"), scope);
        } else throw new EmailException("No recepient");
    }

    private void sendPeparedMail(Pair<String, String> recipient, String path, JSONObject configuration, Object scope) throws IOException, EmailException {
        if (recipient != null) {
            String content = IOUtils.toString(new FileReader(new File(path)));
            String subject = configuration.has("subject") ? configuration.getString("subject")  : "Subject";

            StringWriter stringWriter = new StringWriter();
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(new StringReader(content), "registration");
            mustache.execute(stringWriter, scope);

            JSONObject mail = Options.getInstance().getJSONObject("mail");
            if (mail != null) {
                String fromEmail = mail.getString("from.user");
                String fromName = mail.has("from.name") ? mail.getString("from.name") : fromEmail;
                Pair<String, String> from = Pair.of(fromEmail, fromName);

                if (configuration.has("content.html") && configuration.getBoolean("content.html")) {
                    sendHTMLEmail(from, Arrays.asList(recipient),subject , stringWriter.toString());
                } else {
                    sendMail(from, Arrays.asList(recipient),subject , stringWriter.toString());
                }
            } else throw new EmailException("mail configuration is missing ");

        }else  throw new EmailException("No recipient");


    }

    @Override
    public void sendRegistrationMail(User user, Pair<String, String> recipient, String generatedPswd, String requestToken) throws IOException, EmailException {
        if (recipient != null) {
            HashMap<String, String> scopes = new HashMap<String, String>();
            scopes.put("user", recipient.getRight());
            scopes.put("password", generatedPswd);
            scopes.put("token", requestToken);
            if (user != null) {
                scopes.put("username", user.getUsername());
            }
            String path = InitServlet.CONFIG_DIR + File.separator + Options.getInstance().getString("textsDir")+File.separator+"mail_registration";
            LOGGER.info("Sending email: Generated password for new created user");
            sendPeparedMail(recipient, path, Options.getInstance().getJSONObject("registration"),scopes );
        } else throw new EmailException("No recipient");
    }

    @Override
    public void sendHTMLEmail(Pair<String, String> from, List<Pair<String, String>> recipients, String subject, String text) throws ServerException, EmailException {
        HtmlEmail email = new HtmlEmail();
        sendEmailInternal(from, recipients, subject, text, email);
    }

    @Override
    public void sendMail(Pair<String, String> from, List<Pair<String, String>> recipients, String subject, String text) throws ServerException, EmailException {
        SimpleEmail email = new SimpleEmail();
        sendEmailInternal(from, recipients, subject, text, email);
    }

    private void sendEmailInternal(Pair<String, String> from, List<Pair<String, String>> recipients, String subject, String text, Email email) throws EmailException {
        JSONObject mail = Options.getInstance().getJSONObject("mail");
        email.setHostName(mail.getString("smtp.host"));
        email.setSmtpPort(mail.getInt("smtp.port"));
        email.setCharset("utf-8");
        if (mail.has("smtp.user") && mail.has("smtp.pass")) {
            email.setAuthenticator(new DefaultAuthenticator(mail.getString("smtp.user"), mail.getString("smtp.pass")));
        }
        if (mail.has("smtp.starttls.enable")) {
            email.setSSLOnConnect(mail.getBoolean("smtp.starttls.enable"));
        }
        for (Pair<String, String> recip : recipients) {
            LOGGER.info("Adding recepient "+recip.getLeft());
            email.addTo(recip.getLeft(), recip.getRight());
        }

        long start = System.currentTimeMillis();

        email.setFrom(from.getLeft(), from.getRight());
        email.setSubject(subject);
        email.setMsg(text);
        email.send();

        long end = System.currentTimeMillis();

        LOGGER.info(String.format("Send email took %d", (end - start)));
    }
}
