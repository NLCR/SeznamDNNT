package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.indexer.models.NotificationInterval;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.rights.exceptions.NotAuthorizedException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerExpiredTokenException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerInvalidPwdTokenException;
import org.json.JSONObject;

import java.util.List;
import java.util.logging.Logger;

/**
 * Implementation of this class is able to manage regular users' account
 * @author happy
 */
public interface UserController {

    public static final Logger LOGGER = Logger.getLogger(UserController.class.getName());
    
    /**
     * Returns all users
     * @return
     * @throws UserControlerException
     */
    public List<User> getAll() throws UserControlerException;
    
    /**
     * Finds users by given role
     * @param role
     * @return
     * @throws UserControlerException
     */
    public List<User> findUsersByRole(Role role) throws UserControlerException;
    
    /**
     * Find users by given prefix
     * @param prefix Search prefix (name, surname, username, email)
     * @return
     * @throws UserControlerException
     */
    public List<User> findUsersByPrefix(String prefix) throws UserControlerException;


    /**
     * Find user by given apikey
     * @param apikey Given api key
     * @return
     * @throws UserControlerException
     */
    public User findUserByApiKey(String apikey) throws UserControlerException;
    
    /**
     * Find user by given username
     * @param username 
     * @return
     * @throws UserControlerException
     */
    public User findUser(String username) throws UserControlerException;
    
    /**
     * Find all users by given notification interval
     * @param interval
     * @return
     * @throws UserControlerException
     */
    public List<User> findUsersByNotificationInterval(String interval) throws UserControlerException;
    
    /**
     * Register new user
     * @param rawJson Raw json of new user
     * @return
     * @throws UserControlerException
     */
    public User register(String rawJson) throws UserControlerException;
    
    /**
     * Reseting password
     * @param json Reseting password jeson
     * @return Returns user object if  the password has been reseted
     * @throws UserControlerException
     * @throws NotAuthorizedException
     */
    public User resetPwd(JSONObject json)throws UserControlerException, NotAuthorizedException;
    
    /**
     * Generate token for reseting password
     * @param inputJs
     * @return
     * @throws UserControlerException
     */
    public String forgotPwd(JSONObject inputJs) throws UserControlerException;

    /**
     * Validate given password token
     * @param token
     * @return
     * @throws UserControlerException
     */
    public boolean validatePwdToken(String token) throws UserControlerException;
    
    /**
     * Chage user's password
     * @param pwd
     * @return
     * @throws UserControlerException
     * @throws NotAuthorizedException
     */
    public User changePwdUser(String pwd ) throws UserControlerException, NotAuthorizedException;
    
    /**
     * Change password in case of receving token
     * @param token Change password token
     * @param pwd New password
     * @return
     * @throws UserControlerInvalidPwdTokenException
     * @throws UserControlerException
     * @throws UserControlerExpiredTokenException
     */
    public User changePwdToken( String token, String pwd ) throws UserControlerInvalidPwdTokenException, UserControlerException, UserControlerExpiredTokenException;

    /**
     * Saving user for user's purpose. Dedicated for user's save. 
     * <i>It doesn't touch password, role and type of user</i>
     * @param user Use to store
     * @return Stored user
     * @throws UserControlerException
     * @throws NotAuthorizedException
     */
    public User userSave(User user) throws UserControlerException, NotAuthorizedException;
    

    /**
     * Save user for admins purpose.  
     * <i>Note: It dosn't touch password field </i>
     * @param user User to tore
     * @return Stored user
     * @throws UserControlerException
     * @throws NotAuthorizedException
     */
    public User adminSave(User user) throws UserControlerException, NotAuthorizedException;
    
    
    /**
     * Resturns requests associated with user
     * @param username Given user name
     * @return List of requests
     * @throws UserControlerException
     */
    public List<Zadost> getZadost(String username) throws UserControlerException;
    

    /** 
     * Change notification property
     * @param interval
     * @return
     * @throws UserControlerException Something happened ;-)
     */
    public User changeIntervalForUser(String username, NotificationInterval interval) throws UserControlerException;
}
