package cz.inovatika.sdnnt.index;

public class UsersIterationSupport  extends IterationSupport{

    public static final String USERS_INDEX = "users";

    @Override
    public String getCollection() {
        return USERS_INDEX;
    }
}
