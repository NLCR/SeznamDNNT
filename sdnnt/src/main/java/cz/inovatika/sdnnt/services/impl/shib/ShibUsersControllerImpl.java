package cz.inovatika.sdnnt.services.impl.shib;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONObject;

import cz.inovatika.sdnnt.indexer.models.NotificationInterval;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.rights.exceptions.NotAuthorizedException;
import cz.inovatika.sdnnt.services.UserController;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerExpiredTokenException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerInvalidPwdTokenException;
import cz.inovatika.sdnnt.services.impl.AbstractUserController;
import cz.inovatika.sdnnt.services.impl.users.UsersUtils;

public class ShibUsersControllerImpl extends AbstractUserController implements UserController{


    @Override
    public List<User> findUsersByRole(Role role) throws UserControlerException {
        String collection = DataCollections.shibusers.name();
        return findUsersByRole(role, collection);
    }

    @Override
    public List<User> findUsersByPrefix(String prefix) throws UserControlerException {
        String collection = DataCollections.shibusers.name();
        return findUserByPrefixImpl(prefix, collection);
    }

    @Override
    public User findUserByApiKey(String apikey) throws UserControlerException {
        throw new NotImplementedException("not implemented for shib users"); 
    }

    @Override
    public User findUser(String username) throws UserControlerException {
        try (SolrClient solr = buildClient()) {
            return UsersUtils.toTOObject(UsersUtils.findOneUser(solr, "username:\"" + username + "\"", DataCollections.shibusers.name()));
        } catch (SolrServerException | IOException ex) {
            throw new UserControlerException(ex);
        }
    }

    @Override
    public List<User> findUsersByNotificationInterval(String interval) throws UserControlerException {
        String collection = DataCollections.shibusers.name();
        return findUsersByNotificationIntervalImpl(interval, collection);
    }

    @Override
    public User register(String rawJson) throws UserControlerException {
        throw new NotImplementedException("not implemented for shib users"); 
    }

    @Override
    public User resetPwd(JSONObject json) throws UserControlerException, NotAuthorizedException {
        throw new NotImplementedException("not implemented for shib users"); 
    }

    @Override
    public String forgotPwd(JSONObject inputJs) throws UserControlerException {
        throw new NotImplementedException("not implemented for shib users"); 
    }

    @Override
    public boolean validatePwdToken(String token) throws UserControlerException {
        throw new NotImplementedException("not implemented for shib users"); 
    }

    @Override
    public User changePwdUser(String pwd) throws UserControlerException, NotAuthorizedException {
        throw new NotImplementedException("not implemented for shib users"); 
    }

    @Override
    public User changePwdToken(String token, String pwd)
            throws UserControlerInvalidPwdTokenException, UserControlerException, UserControlerExpiredTokenException {
        throw new NotImplementedException("not implemented for shib users"); 
    }

    @Override
    public User userSave(User user) throws UserControlerException, NotAuthorizedException {
        throw new NotImplementedException("not implemented for shib users"); 
    }

    @Override
    public User adminSave(User user) throws UserControlerException, NotAuthorizedException {
        return UsersUtils.toTOObject(save(user));
    }

    public User save(User user) throws UserControlerException{
        try (SolrClient client = buildClient()) {
            return save(user, client, DataCollections.shibusers.name());
        } catch (IOException | SolrServerException ex) {
            throw new  UserControlerException(ex);
        }
    }

    @Override
    public User changeIntervalForUser(String username, NotificationInterval interval) throws UserControlerException {
        return changeIntervalImpl(username, interval, DataCollections.shibusers.name());
    }

    @Override
    public List<User> getAll() throws UserControlerException {
        String collection = DataCollections.shibusers.name();
        return getUsersImpl(collection);
    }
    
}
