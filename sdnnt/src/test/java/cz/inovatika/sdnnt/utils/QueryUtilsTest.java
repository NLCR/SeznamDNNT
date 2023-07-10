package cz.inovatika.sdnnt.utils;

import org.easymock.EasyMock;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.utils.QueryUtils;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.model.workflow.WorkflowOwner;

public class QueryUtilsTest {
        
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
