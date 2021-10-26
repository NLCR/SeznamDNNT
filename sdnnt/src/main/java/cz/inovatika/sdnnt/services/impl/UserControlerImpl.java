package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.indexer.models.Zadost;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.rights.exceptions.NotAuthorizedException;
import cz.inovatika.sdnnt.services.MailService;
import cz.inovatika.sdnnt.services.UserControler;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerExpiredTokenException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerInvalidPwdTokenException;
import cz.inovatika.sdnnt.utils.GeneratePSWDUtility;
import cz.inovatika.sdnnt.utils.SolrUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static cz.inovatika.sdnnt.utils.ServletsSupport.errorJson;

public class UserControlerImpl implements UserControler {

    private HttpServletRequest request;
    private MailService mailService;

    public UserControlerImpl(HttpServletRequest request) {
        this.request = request;
    }

    public UserControlerImpl(HttpServletRequest request, MailService mailService) {
        this.request = request;
        this.mailService = mailService;
    }

    private void setSessionObject(HttpServletRequest req, User user) {
        req.getSession(true).setAttribute(AUTHENTICATED_USER, user);
    }

    @Override
    public User login() throws UserControlerException  {
        try(SolrClient client = buildClient()) {
            JSONObject ret = new JSONObject();
            try {
                JSONObject json = new JSONObject(IOUtils.toString(this.request.getInputStream(), "UTF-8"));
                String username = json.getString("user");
                String pwdHashed =  DigestUtils.sha256Hex(json.getString("pwd"));
                ret.put("username", username);
                User user = findOneUser(client, "username:\"" + username + "\"");
                if (user != null && user.pwd != null && user.pwd.equals(pwdHashed)) {
                    setSessionObject(this.request, user);
                    return toTOObject(user);
                } else throw new UserControlerException("Cannot find user or invalid password");
            } catch (Exception ex) {
                throw new UserControlerException(ex);
            }
        } catch (IOException ex) {
            throw new UserControlerException(ex);
        }
    }

    @Override
    public User logout() throws UserControlerException {
        this.request.getSession().invalidate();
        return getUser();
    }

    @Override
    public User getUser() {
        return (User) this.request.getSession(true).getAttribute("user");
    }

    @Override
    public List<User> getAll() throws UserControlerException{
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery("*:*")
                    .setRows(1000);
            return solr.query("users", query).getBeans(User.class).stream().map(this::toTOObject).collect(Collectors.toList());
        } catch (SolrServerException | IOException ex) {
            throw new UserControlerException(ex);
        }
    }

    @Override
    public User findUserByApiKey(String apikey) throws UserControlerException{
        try (SolrClient solr = buildClient()) {
            return toTOObject(findOneUser(solr, "apikey:\"" + apikey + "\""));
        } catch (SolrServerException | IOException ex) {
            throw new UserControlerException(ex);
        }
    }


    @Override
    public List<User> findUsersByPrefix(String prefix) throws UserControlerException {
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery(String.format("username:%s* OR jmeno:%s* OR prijmeni:%s*",  prefix, prefix, prefix))
                    .setRows(1000);
            return solr.query("users", query).getBeans(User.class).stream().map(this::toTOObject).collect(Collectors.toList());
        } catch (SolrServerException | IOException ex) {
            throw new UserControlerException(ex);
        }
    }

    @Override
    public User findUser(String username) throws UserControlerException{
        try (SolrClient solr = buildClient()) {
            return toTOObject(findOneUser(solr, "username:\"" + username + "\""));
        } catch (SolrServerException | IOException ex) {
            throw new UserControlerException(ex);
        }
    }


    @Override
    public User register(String js) throws UserControlerException {
        User old = findUser((new JSONObject(js)).getString("username"));
        if (old != null) {
            throw new UserControlerException("username_exists");
        }
        try  {
            User user = User.fromJSON(js);

            String newPwd = GeneratePSWDUtility.generatePwd();
            user.pwd = DigestUtils.sha256Hex(newPwd);

            // generate token and store expiration
            user.resetPwdToken = UUID.randomUUID().toString();
            user.resetPwdExpiration = Date.from(LocalDateTime.now().plusDays(Options.getInstance().getInt("resetPwdExpirationDays", 3)).toInstant(ZoneOffset.UTC));

            save(user);

            Pair<String,String> userRecepient = Pair.of(user.email, user.jmeno +" "+user.prijmeni);
            mailService.sendRegistrationMail(user, userRecepient,newPwd, user.resetPwdToken);

            return toTOObject(user);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            throw  new UserControlerException(ex);
        }
    }

    @Override
    public User resetPwd(JSONObject js) throws UserControlerException {
        String newPwd = GeneratePSWDUtility.generatePwd();
        String username = js.optString("username", "");
        try {
            User subject = findUser(username);

            if (subject != null && subject.email != null) {
                subject.pwd = DigestUtils.sha256Hex(newPwd);
                mailService.sendResetPasswordMail(subject, Pair.of(subject.email, subject.jmeno +" "+subject.prijmeni), newPwd);
                return  toTOObject(save(subject));
            } else {
                throw new UserControlerException("User's email must be specified");
            }
        } catch (IOException | EmailException e) {
            throw new UserControlerException(e);
        }

    }


    @Override
    public String forgotPwd( JSONObject inputJs) throws UserControlerException {
        String input =inputJs.optString("username", "");
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery("email:\"" + input + "\" OR username:\""+input+"\"")
                    .setRows(1);
            List<User> users = solr.query("users", query).getBeans(User.class);
            if (users.isEmpty()) {
                throw new UserControlerException("Cannot find user");
            } else {
                User user = users.get(0);
                if (user.email != null) {

                    // generate token and store expiration
                    user.resetPwdToken = UUID.randomUUID().toString();
                    user.resetPwdExpiration = Date.from(LocalDateTime.now().plusDays(Options.getInstance().getInt("resetPwdExpirationDays", 3)).toInstant(ZoneOffset.UTC));
                    save(user);

                    //save everything to user
                    mailService.sendResetPasswordRequest(user, Pair.of(user.email, user.jmeno +" "+user.prijmeni), user.resetPwdToken);
                    return user.resetPwdToken;

                } else {
                    throw new UserControlerException(String.format("User %s doesnt have  email",user.username));
                }
            }
        } catch (SolrServerException | IOException |EmailException ex) {
            throw new UserControlerException(ex);
        }
    }


    @Override
    public boolean validatePwdToken(String token) throws UserControlerException {
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery("resetPwdToken:\"" + token + "\"")
                    .setRows(1);
            List<User> users = solr.query("users", query).getBeans(User.class);
            return !users.isEmpty();
        } catch (SolrServerException | IOException   ex) {
            throw  new UserControlerException(ex);
        }
    }

    @Override
    public User changePwdUser( String pwd) throws UserControlerException, NotAuthorizedException {
        User sender = getUser();
        if (sender != null) {
            sender.pwd  = DigestUtils.sha256Hex(pwd);
            sender.resetPwdToken = null;
            sender.resetPwdExpiration = null;
            return toTOObject(save(sender));
        } else {
            throw new NotAuthorizedException("not authorized");
        }
    }

    @Override
    public User changePwdToken( String token, String pwd) throws UserControlerInvalidPwdTokenException, UserControlerException, UserControlerExpiredTokenException {
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery("resetPwdToken:\"" + token + "\"")
                    .setRows(1);
            List<User> users = solr.query("users", query).getBeans(User.class);
            solr.close();
            if (users.isEmpty()) {
                throw new UserControlerInvalidPwdTokenException("invalid token");
            } else {
                User user = users.get(0);
                if (user.resetPwdExpiration.after(new Date())) {
                    user.pwd  = DigestUtils.sha256Hex(pwd);
                    user.resetPwdToken = null;
                    user.resetPwdExpiration = null;
                    return toTOObject(save(user));
                } else {
                    throw new UserControlerExpiredTokenException("expired");
                }
            }
        } catch (SolrServerException | IOException   ex) {
            throw new UserControlerException(ex);
        }
    }

    @Override
    public List<User> findUsersByNotificationInterval(String interval) throws UserControlerException {

        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery("notifikace_interval:\""+interval+"\"")
                    .setRows(1000);
            return solr.query("users", query).getBeans(User.class).stream().map(this::toTOObject).collect(Collectors.toList());
        } catch (SolrServerException | IOException ex) {
            throw new UserControlerException(ex);
        }
    }


    @Override
    public User userSave(User user) throws UserControlerException, NotAuthorizedException {
        User found = null;
        try (SolrClient client = buildClient()) {
            found = findOneUser(client, "username:\"" + user.username + "\"");
        } catch (IOException | SolrServerException ex) {
            throw new UserControlerException(ex.getMessage());
        }

        if (found != null) {
            user.pwd=found.pwd;
            return toTOObject(save(user));
        } else {
            throw new UserControlerException(String.format("Cannot save user %s", user.username));
        }
    }

    public User save(User user) throws UserControlerException{
        try (SolrClient client = buildClient()) {
            return save(user, client);
        } catch (IOException | SolrServerException ex) {
            throw new  UserControlerException(ex);
        }
    }

    @Override
    public List<Zadost> getZadost(String username) throws UserControlerException {
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery("user:\"" + username + "\"")
                    .addFilterQuery("state:open")
                    .setFields("id", "identifiers", "typ", "user", "state", "navrh", "poznamka", "pozadavek", "datum_zadani", "datum_vyrizeni", "formular")
                    .setRows(10);
            List<Zadost> zadost = solr.query("zadost", query).getBeans(Zadost.class);
            return zadost;
        } catch (SolrServerException | IOException ex) {
            throw new  UserControlerException(ex);
        }
    }


    private  User save(User user, SolrClient client) throws IOException, SolrServerException {
        try {
            client.addBean("users", user);
            return toTOObject(user);
        } finally {
            SolrUtils.quietCommit(client, "users");
        }
    }


    @Override
    public List<User> findUsersByRole(Role role) throws UserControlerException {
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery("role:\""+role.name()+"\"")
                    .setRows(1000);
            return solr.query("users", query).getBeans(User.class).stream().map(this::toTOObject).collect(Collectors.toList());
        } catch (SolrServerException | IOException ex) {
            throw new UserControlerException(ex);
        }
    }

    User toTOObject(User user) {
        if (user != null) {
            User toObject = new User();
            toObject.username = user.username;
            toObject.jmeno = user.jmeno;
            toObject.prijmeni = user.prijmeni;
            toObject.apikey = user.apikey;
            toObject.resetPwdToken = user.resetPwdToken;
            toObject.resetPwdExpiration = user.resetPwdExpiration;
            toObject.email = user.email;
            toObject.adresa = user.adresa;
            toObject.notifikace_interval = user.notifikace_interval;
            toObject.role = user.role;
            toObject.institution = user.institution;
            toObject.ico = user.ico;
            toObject.poznamka = user.poznamka;
            return toObject;
        } else {
            return null;
        }
    }

    User findOneUser(SolrClient solr, String q) throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery(q)
                .setRows(1);
        List<User> users = solr.query("users", query).getBeans(User.class);
        solr.close();
        if (users.isEmpty()) {
            return null;
        } else {
            return users.get(0);
        }
    }

    SolrClient buildClient() {
        return new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build();
    }

}
