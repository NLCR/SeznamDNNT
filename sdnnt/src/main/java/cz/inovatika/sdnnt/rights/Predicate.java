package cz.inovatika.sdnnt.rights;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * Right predicate
 */
public interface Predicate {

    /**
     * Returns true if current user is permitted to perform action
     * @param req HttpRequest
     * @return
     */
    public boolean permit(HttpServletRequest req);
}
