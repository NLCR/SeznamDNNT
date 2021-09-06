package cz.inovatika.sdnnt.rights;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

public interface Predicate {

    public boolean permit(HttpServletRequest req);
}
