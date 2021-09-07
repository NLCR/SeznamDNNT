package cz.inovatika.sdnnt.rights.impl.predicates;

import cz.inovatika.sdnnt.rights.Predicate;
import cz.inovatika.sdnnt.utils.IPAddressUtils;

import javax.servlet.http.HttpServletRequest;

public class MustBeCalledFromLocalhost implements Predicate {
    @Override
    public boolean permit(HttpServletRequest req) {
        return IPAddressUtils.isLocalAddress(req);
    }
}
