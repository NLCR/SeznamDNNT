package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;

/**
 * Implementation of interfaces is able to provide login/logout functionality
 * @author happy
 *
 */
public interface ApplicationUserLoginSupport {

    public static final String AUTHENTICATED_USER = "user";

    /**
     * Login method
     * @return
     * @throws UserControlerException
     */
    public User login() throws UserControlerException;

    /**
     * Logout
     * @return
     * @throws UserControlerException
     */
    public User logout() throws UserControlerException;

    /**
     * Returns logged user or null
     * @return
     */
    public User getUser();

}
