package cz.inovatika.sdnnt.rights.impl.predicates;

import cz.inovatika.sdnnt.rights.Predicate;
import cz.inovatika.sdnnt.services.impl.users.UserControlerImpl;

import javax.servlet.http.HttpServletRequest;

/**
 * The predicate will allow  action if the caller is a valid user; i.e. Must be logged
 * @author happy
 *
 */
public class MustBeLogged implements Predicate {
    @Override
    public boolean permit(HttpServletRequest req) {
        return new UserControlerImpl(req).getUser() != null;
    }
}
