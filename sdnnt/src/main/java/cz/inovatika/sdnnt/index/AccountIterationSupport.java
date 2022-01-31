package cz.inovatika.sdnnt.index;

public class AccountIterationSupport extends IterationSupport{

    public static final String ZADOST_INDEX = "zadost";

    @Override
    protected String getCollection() {
        return ZADOST_INDEX;
    }
}
