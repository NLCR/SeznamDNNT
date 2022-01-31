package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;

public class StaticApplicationLoginSupport implements ApplicationUserLoginSupport  {

    private User user;

    public StaticApplicationLoginSupport(User user) {
        this.user = user;
    }

    @Override
    public User login() throws UserControlerException {
        throw new UnsupportedOperationException("unsupported");
    }

    @Override
    public User logout() throws UserControlerException {
        throw new UnsupportedOperationException("unsupported");
    }

    @Override
    public User getUser() {
        return this.user;
    }
}
