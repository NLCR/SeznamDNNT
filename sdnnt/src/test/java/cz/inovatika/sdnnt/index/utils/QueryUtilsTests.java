package cz.inovatika.sdnnt.index.utils;

import org.junit.Test;

public class QueryUtilsTests {

    @Test
    public void testQuery() {
        String query = QueryUtils.query(" \"test\" ab c");
        String query2 = QueryUtils.query(" \"test\" \"ab c");
        String query3 = QueryUtils.query("slovo1\tslovo2\tslovo3");
    }
}
