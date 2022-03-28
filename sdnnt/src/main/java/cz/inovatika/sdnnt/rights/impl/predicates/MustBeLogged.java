package cz.inovatika.sdnnt.rights.impl.predicates;

import cz.inovatika.sdnnt.rights.Predicate;
import cz.inovatika.sdnnt.services.impl.users.UserControlerImpl;

import javax.servlet.http.HttpServletRequest;

public class MustBeLogged implements Predicate {
    @Override
    public boolean permit(HttpServletRequest req) {
        return new UserControlerImpl(req).getUser() != null;
    }
}
