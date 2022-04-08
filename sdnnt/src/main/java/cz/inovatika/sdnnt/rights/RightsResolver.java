package cz.inovatika.sdnnt.rights;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The rights resolver. The class aggregates implementations of predicate interfaces and tries to find 
 * the first who says <code>false</code>. If there is no such one, it returns true.    
 */
public class RightsResolver {

    private HttpServletRequest req;
    private List<Predicate> predicates;

    public RightsResolver(HttpServletRequest req, List<Predicate> predicates) {
        this.req = req;
        this.predicates = predicates;
    }
    public RightsResolver(HttpServletRequest req, Predicate... predicates) {
        this.req = req;
        this.predicates = Arrays.stream(predicates).collect(Collectors.toList());
    }

    public boolean permit() {
        for (Predicate predicate :
                predicates) {
            if (!predicate.permit(req)) return false;

        }
        return true;
    }



}
