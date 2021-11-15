package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.rights.exceptions.NotAuthorizedException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerExpiredTokenException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerInvalidPwdTokenException;
import org.json.JSONObject;

import java.util.List;
import java.util.logging.Logger;

public interface UserControler {

    public static final Logger LOGGER = Logger.getLogger(UserControler.class.getName());
    public static final String AUTHENTICATED_USER = "user";

    public User login() throws UserControlerException;
    public User logout() throws UserControlerException;

    public User getUser();
    public List<User> getAll() throws UserControlerException;
    public List<User> findUsersByRole(Role role) throws UserControlerException;
    public List<User> findUsersByPrefix(String prefix) throws UserControlerException;


    public User findUserByApiKey(String apikey) throws UserControlerException;
    public User findUser(String username) throws UserControlerException;

    public List<User> findUsersByNotificationInterval(String interval) throws UserControlerException;

    public User register(String js) throws UserControlerException;
    public User resetPwd(JSONObject js)throws UserControlerException, NotAuthorizedException;
    public String forgotPwd(JSONObject inputJs) throws UserControlerException;

    public boolean validatePwdToken(String token) throws UserControlerException;
    public User changePwdUser(String pwd ) throws UserControlerException, NotAuthorizedException;

    public User changePwdToken( String token, String pwd ) throws UserControlerInvalidPwdTokenException, UserControlerException, UserControlerExpiredTokenException;

    public User userSave(User user) throws UserControlerException, NotAuthorizedException;

    public List<Zadost> getZadost(String username) throws UserControlerException;
}
