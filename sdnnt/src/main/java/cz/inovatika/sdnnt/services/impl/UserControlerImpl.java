package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.rights.exceptions.NotAuthorizedException;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.MailService;
import cz.inovatika.sdnnt.services.UserControler;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerExpiredTokenException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerInvalidPwdTokenException;
import cz.inovatika.sdnnt.services.impl.users.UsersUtils;
import cz.inovatika.sdnnt.tracking.TrackSessionUtils;
import cz.inovatika.sdnnt.utils.GeneratePSWDUtility;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.mail.EmailException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static cz.inovatika.sdnnt.utils.ServletsSupport.errorJson;

public class UserControlerImpl implements UserControler, ApplicationUserLoginSupport {


    private static int USERS_LIMIT = 10000;

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
        TrackSessionUtils.touchSession(req.getSession());
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
                User user = UsersUtils.findOneUser(client, "username:\"" + username + "\"");
                if (user != null && user.getPwd() != null && user.getPwd().equals(pwdHashed)) {
                    setSessionObject(this.request, user);
                    return UsersUtils.toTOObject(user);
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
                    .setRows(USERS_LIMIT);
            QueryResponse users = solr.query("users", query);
            List<User> collect = users.getResults().stream().map(User::fromSolrDocument).map(UsersUtils::toTOObject).collect(Collectors.toList());
            //return solr.query("users", query).getBeans(User.class).stream().map(this::toTOObject).collect(Collectors.toList());
            return collect;
        } catch (SolrServerException | IOException ex) {
            throw new UserControlerException(ex);
        }
    }

    @Override
    public User findUserByApiKey(String apikey) throws UserControlerException{
        try (SolrClient solr = buildClient()) {
            return UsersUtils.toTOObject(UsersUtils.findOneUser(solr, "apikey:\"" + apikey + "\""));
        } catch (SolrServerException | IOException ex) {
            throw new UserControlerException(ex);
        }
    }


    @Override
    public List<User> findUsersByPrefix(String prefix) throws UserControlerException {
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery(String.format("fullText:%s*",  prefix))
                    .setRows(1000);
            QueryResponse users = solr.query("users", query);
            List<User> userList = users.getResults().stream().map(User::fromSolrDocument).collect(Collectors.toList());
            List<User> collect = userList.stream().map(UsersUtils::toTOObject).collect(Collectors.toList());
            return collect;
            //return solr.query("users", query).getBeans(User.class).stream().map(this::toTOObject).collect(Collectors.toList());
        } catch (SolrServerException | IOException ex) {
            throw new UserControlerException(ex);
        }
    }

    @Override
    public User findUser(String username) throws UserControlerException{
        try (SolrClient solr = buildClient()) {
            return UsersUtils.toTOObject(UsersUtils.findOneUser(solr, "username:\"" + username + "\""));
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
            user.setPwd( DigestUtils.sha256Hex(newPwd));

            // generate token and store expiration
            user.setResetPwdToken( UUID.randomUUID().toString());
            user.setResetPwdExpiration(Date.from(LocalDateTime.now().plusDays(Options.getInstance().getInt("resetPwdExpirationDays", 3)).toInstant(ZoneOffset.UTC)));

            save(user);

            Pair<String,String> userRecepient = Pair.of(user.getEmail(), user.getJmeno() +" "+user.getPrijmeni());
            mailService.sendRegistrationMail(user, userRecepient,newPwd, user.getResetPwdToken());

            return UsersUtils.toTOObject(user);
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

            if (subject != null && subject.getEmail() != null) {
                subject.setPwd(DigestUtils.sha256Hex(newPwd));
                mailService.sendResetPasswordMail(subject, Pair.of(subject.getEmail(), subject.getJmeno() +" "+subject.getPrijmeni()), newPwd);
                return  UsersUtils.toTOObject(save(subject));
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

            QueryResponse usersResponse = solr.query("users", query);
            List<User> users = usersResponse.getResults().stream().map(User::fromSolrDocument).map(UsersUtils::toTOObject).collect(Collectors.toList());
            //List<User> users = solr.query("users", query).getBeans(User.class);
            if (users.isEmpty()) {
                throw new UserControlerException("Cannot find user");
            } else {
                User user = users.get(0);
                if (user.getEmail() != null) {

                    // generate token and store expiration
                    user.setResetPwdToken( UUID.randomUUID().toString());
                    user.setResetPwdExpiration( Date.from(LocalDateTime.now().plusDays(Options.getInstance().getInt("resetPwdExpirationDays", 3)).toInstant(ZoneOffset.UTC)));
                    save(user);

                    //save everything to user
                    mailService.sendResetPasswordRequest(user, Pair.of(user.getEmail(), user.getJmeno() +" "+user.getPrijmeni()), user.getResetPwdToken());
                    return user.getResetPwdToken();

                } else {
                    throw new UserControlerException(String.format("User %s doesnt have  email",user.getUsername()));
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

            List<User> users = solr.query("users", query).getResults().stream().map(User::fromSolrDocument).collect(Collectors.toList());

            return !users.isEmpty();
        } catch (SolrServerException | IOException   ex) {
            throw  new UserControlerException(ex);
        }
    }

    @Override
    public User changePwdUser( String pwd) throws UserControlerException, NotAuthorizedException {
        User sender = getUser();
        if (sender != null) {
            sender.setPwd(DigestUtils.sha256Hex(pwd));
            sender.setResetPwdToken(null);
            sender.setResetPwdExpiration(null);
            return UsersUtils.toTOObject(save(sender));
        } else {
            throw new NotAuthorizedException("not authorized");
        }
    }

    @Override
    public User changePwdToken( String token, String pwd) throws UserControlerInvalidPwdTokenException, UserControlerException, UserControlerExpiredTokenException {
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery("resetPwdToken:\"" + token + "\"")
                    .setRows(1);

            QueryResponse usersResponse = solr.query("users", query);
            List<User> users = usersResponse.getResults().stream().map(User::fromSolrDocument).map(UsersUtils::toTOObject).collect(Collectors.toList());

            //List<User> users = solr.query("users", query).getBeans(User.class);
            solr.close();
            if (users.isEmpty()) {
                throw new UserControlerInvalidPwdTokenException("invalid token");
            } else {
                User user = users.get(0);
                if (user.getResetPwdExpiration().after(new Date())) {
                    user.setPwd(DigestUtils.sha256Hex(pwd));
                    user.setResetPwdToken( null );
                    user.setResetPwdExpiration( null);
                    return UsersUtils.toTOObject(save(user));
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

            QueryResponse usersResponse = solr.query("users", query);
            List<User> users = usersResponse.getResults().stream().map(User::fromSolrDocument).map(UsersUtils::toTOObject).collect(Collectors.toList());
            return users;
            //return solr.query("users", query).getBeans(User.class).stream().map(this::toTOObject).collect(Collectors.toList());
        } catch (SolrServerException | IOException ex) {
            throw new UserControlerException(ex);
        }
    }


    @Override
    public User adminSave(User user) throws UserControlerException, NotAuthorizedException {
        User found = null;
        try (SolrClient client = buildClient()) {
            found = UsersUtils.findOneUser(client, "username:\"" + user.getUsername() + "\"");
        } catch (IOException | SolrServerException ex) {
            throw new UserControlerException(ex.getMessage());
        }

        if (found != null) {
            user.setPwd(found.getPwd());
            return UsersUtils.toTOObject(save(user));
        } else {
            throw new UserControlerException(String.format("Cannot save user %s", user.getUsername()));
        }
    }
        @Override
    public User userSave(User user) throws UserControlerException, NotAuthorizedException {
        User found = null;
        try (SolrClient client = buildClient()) {
            found = UsersUtils.findOneUser(client, "username:\"" + user.getUsername() + "\"");
        } catch (IOException | SolrServerException ex) {
            throw new UserControlerException(ex.getMessage());
        }

        if (found != null) {
            user.setTyp(found.getTyp());
            user.setPwd(found.getPwd());
            user.setRole(found.getRole());
            return UsersUtils.toTOObject(save(user));
        } else {
            throw new UserControlerException(String.format("Cannot save user %s", user.getUsername()));
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
            //List<Zadost> zadost = solr.query("zadost", query).getBeans(Zadost.class);

            List<Zadost> zadosti = new ArrayList<>();
            SolrJUtilities.jsonDocsFromResult(solr, query, "zadost").forEach(z-> {
                zadosti.add(Zadost.fromJSON(z.toString()));
            });

            return zadosti;
        } catch (SolrServerException | IOException ex) {
            throw new  UserControlerException(ex);
        }
    }


    private  User save(User user, SolrClient client) throws IOException, SolrServerException {
        try {
            client.add("users", user.toSolrInputDocument());
            return UsersUtils.toTOObject(user);
        } finally {
            SolrJUtilities.quietCommit(client, "users");
        }
    }


    @Override
    public List<User> findUsersByRole(Role role) throws UserControlerException {
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery("role:\""+role.name()+"\"")
                    .setRows(1000);

            QueryResponse usersResponse = solr.query("users", query);
            List<User> users = usersResponse.getResults().stream().map(User::fromSolrDocument).map(UsersUtils::toTOObject).collect(Collectors.toList());
            return users;

        } catch (SolrServerException | IOException ex) {
            throw new UserControlerException(ex);
        }
    }

    SolrClient buildClient() {
        return new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build();
    }

}
