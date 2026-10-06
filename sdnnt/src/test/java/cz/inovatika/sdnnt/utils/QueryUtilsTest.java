package cz.inovatika.sdnnt.utils;

import org.easymock.EasyMock;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.utils.QueryUtils;

public class QueryUtilsTest {

    @Test
    public void queryPreservesAndRoutesQuotedPhrases() {
        Assert.assertEquals("museum", QueryUtils.query("museum"));
        Assert.assertEquals("\"narodni museum\"", QueryUtils.query("\"narodni museum\""));
        Assert.assertEquals(
                "dejiny fullText_exact:\"narodni museum\" praha",
                QueryUtils.query("dejiny \"narodni museum\" praha", QueryUtils.IDENTIFIER_PREFIX, "fullText_exact"));
        Assert.assertEquals(
                "fullText_exact:\"museum\"",
                QueryUtils.query("\"museum\"", QueryUtils.IDENTIFIER_PREFIX, "fullText_exact"));
    }

    @Test
    public void queryEscapesSpecialCharactersAndHandlesUnclosedQuote() {
        Assert.assertEquals("foo\\+bar", QueryUtils.query("foo+bar"));
        Assert.assertEquals("\\\"museum", QueryUtils.query("\"museum"));
    }

    @Test
    public void identifierQueryRemainsAnExactPhrase() {
        String identifier = "oai:aleph-nkp.cz:SKC01-123";
        Assert.assertEquals("\"" + identifier + "\"", QueryUtils.query(identifier));
    }
        
    @Test
    public void bookQueryFilter() {
        
        JSONObject searchObject = new JSONObject();
        /*
         * int yearsBK = opts.getJSONObject("search").getInt("yearsBK");
         */
        searchObject.put("yearsBK", 11);
        searchObject.put("yearsSE", 10);
        
        
        Options opts = EasyMock.createMock(Options.class);
        EasyMock.expect(opts.getJSONObject("search")).andReturn(searchObject).anyTimes();
        
        EasyMock.replay(opts);

        String catalogBKFilterQueryPart = QueryUtils.catalogBKFilterQueryPart(opts, 2023, 1911);
        Assert.assertTrue("(fmt:BK AND ((date1_int:[1911 TO 2012] AND -date2_int:*) OR (date1_int:[1911 TO 2012] AND date2_int:[1911 TO 2012])))".equals(catalogBKFilterQueryPart));
        
        catalogBKFilterQueryPart = QueryUtils.catalogBKFilterQueryPartOnlyLowerBound(opts, 2023, 1911);
        //System.out.println("TEST "+catalogBKFilterQueryPart);
        Assert.assertTrue("(fmt:BK AND (date1_int:[1911 TO 2012] OR (NOT date1_int:* AND date1:(19* OR \"20\" OR \"200\" OR \"201\" OR \"20uu\" OR \"200u\" OR \"201u\"))))".equals(catalogBKFilterQueryPart));
        
        
        String catalogSEFilterQueryPart = QueryUtils.catalogSEFilterQueryPart(opts, 2023, 1911);
        Assert.assertTrue("(fmt:SE AND ((date1_int:[1911 TO 2013] AND date2_int:9999) OR date2_int:[1911 TO 2013]))".equals(catalogSEFilterQueryPart));

        catalogSEFilterQueryPart = QueryUtils.catalogSEFilterQueryPartOnlyLowerBound(opts, 2023, 1911);
        //System.out.println(catalogSEFilterQueryPart);
        Assert.assertTrue("(fmt:SE AND (date1_int:[1911 TO 2013] OR (NOT date1_int:* AND date1:(19* OR \"20\" OR \"200\" OR \"201\" OR \"20uu\" OR \"200u\" OR \"201u\"))))".equals(catalogSEFilterQueryPart));
    }
    
    
}
