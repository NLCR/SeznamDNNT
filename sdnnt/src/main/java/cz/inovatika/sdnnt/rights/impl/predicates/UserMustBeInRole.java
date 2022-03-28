package cz.inovatika.sdnnt.rights.impl.predicates;

import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.rights.Predicate;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.services.impl.users.UserControlerImpl;

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
        User user = new UserControlerImpl(req).getUser();
        if (user != null) {
            Role role = Role.valueOf(user.getRole());
            return list.contains(role);
        } else return false;
    }
}
