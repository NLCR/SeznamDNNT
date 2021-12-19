package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;

public interface ApplicationUserLoginSupport {

    //public static final String USER_SESSION_KYE = "user";

    public static final String AUTHENTICATED_USER = "user";

    public User login() throws UserControlerException;

    public User logout() throws UserControlerException;

    public User getUser();

}
