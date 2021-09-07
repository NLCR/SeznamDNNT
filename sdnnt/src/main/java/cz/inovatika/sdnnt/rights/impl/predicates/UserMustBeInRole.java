package cz.inovatika.sdnnt.rights.impl.predicates;

import cz.inovatika.sdnnt.UserController;
import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.rights.Predicate;
import cz.inovatika.sdnnt.rights.Role;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UserMustBeInRole implements Predicate {

    private List<Role> list = new ArrayList<>();

    public UserMustBeInRole(List<Role> list) {
        this.list = list;
    }

    public UserMustBeInRole(Role... roles) {
        this(Arrays.stream(roles).collect(Collectors.toList()));
    }

    @Override
    public boolean permit(HttpServletRequest req) {
        User user = UserController.getUser(req);
        if (user != null) {
            Role role = Role.valueOf(user.role);
            return list.contains(role);
        } else return false;
    }
}
