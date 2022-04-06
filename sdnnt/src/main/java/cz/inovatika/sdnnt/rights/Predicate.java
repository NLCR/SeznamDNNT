package cz.inovatika.sdnnt.rights;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import cz.inovatika.sdnnt.rights.impl.predicates.MustBeCalledFromLocalhost;
import cz.inovatika.sdnnt.rights.impl.predicates.MustBeLogged;
import cz.inovatika.sdnnt.rights.impl.predicates.UserMustBeInRole;

/**
 * The implementation of this class is able to decide if the caller is permitted to do the action according to current state: user's role, ip address, etc..
 *
 * @see MustBeLogged
 * @see MustBeCalledFromLocalhost
 * @see UserMustBeInRole 
 */
public interface Predicate {

    /**
     * Returns true if current user has permittion to perform the action
     * @param req HttpRequest
     * @return
     */
    public boolean permit(HttpServletRequest req);
}
