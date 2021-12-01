package cz.inovatika.sdnnt.openapi.endpoints.api.impl;

import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.services.impl.UserControlerImpl;

import java.util.logging.Level;
import java.util.logging.Logger;

public class OpenApiLoginSupportImpl implements ApplicationUserLoginSupport {

    public static final Logger LOGGER = Logger.getLogger(OpenApiLoginSupportImpl.class.getName());

    private String key;

    public OpenApiLoginSupportImpl(String key) {
        this.key = key;
    }

    @Override
    public User login() throws UserControlerException {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public User logout() throws UserControlerException {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public User getUser() {
        try {
            return new UserControlerImpl(null).findUserByApiKey(this.key);
        } catch (UserControlerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
            return null;
        }
    }
}
