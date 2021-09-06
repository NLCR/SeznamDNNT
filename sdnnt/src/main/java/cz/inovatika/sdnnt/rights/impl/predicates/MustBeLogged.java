package cz.inovatika.sdnnt.rights.impl.predicates;

import cz.inovatika.sdnnt.UserController;
import cz.inovatika.sdnnt.rights.Predicate;

import javax.servlet.http.HttpServletRequest;

public class MustBeLogged implements Predicate {
    @Override
    public boolean permit(HttpServletRequest req) {
        return UserController.getUser(req) != null;
    }
}
