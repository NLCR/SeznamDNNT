package cz.inovatika.sdnnt.rights.impl.predicates;

import cz.inovatika.sdnnt.rights.Predicate;
import cz.inovatika.sdnnt.utils.IPAddressUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * The predicate will allow the action if the request comes from localhost
 * @author happy
 */
public class MustBeCalledFromLocalhost implements Predicate {
    @Override
    public boolean permit(HttpServletRequest req) {
        return IPAddressUtils.isLocalAddress(req);
    }
}
