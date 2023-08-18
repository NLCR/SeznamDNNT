package cz.inovatika.sdnnt.index;

import org.junit.Assert;
import org.junit.Test;

import cz.inovatika.sdnnt.model.workflow.duplicate.Case;

public class IndexerUtilsTests {
    
    public static final String CASE4ARAW = "[\r\n"
            + "          {\r\n"
            + "            \"stav\": \"PA\",\r\n"
            + "            \"date\": \"20210131\",\r\n"
            + "            \"license\": \"dnnto\",\r\n"
            + "            \"user\": \"batch\"\r\n"
            + "          },\r\n"
            + "          {\r\n"
            + "            \"stav\": \"A\",\r\n"
            + "            \"date\": \"20210801\",\r\n"
            + "            \"license\": \"dnnto\",\r\n"
            + "            \"user\": \"batch\"\r\n"
            + "          },\r\n"
            + "          {\r\n"
            + "            \"stav\": \"A\",\r\n"
            + "            \"date\": \"20220811\",\r\n"
            + "            \"license\": \"dnnto\",\r\n"
            + "            \"comment\": \"scheduler/SKC_4a\",\r\n"
            + "            \"user\": \"scheduler\"\r\n"
            + "          }\r\n"
            + "        ]";

    public static final String CASE4BRAW = "[\r\n"
            + "          {\r\n"
            + "            \"stav\": \"PA\",\r\n"
            + "            \"date\": \"20210131\",\r\n"
            + "            \"license\": \"dnnto\",\r\n"
            + "            \"user\": \"batch\"\r\n"
            + "          },\r\n"
            + "          {\r\n"
            + "            \"stav\": \"A\",\r\n"
            + "            \"date\": \"20210801\",\r\n"
            + "            \"license\": \"dnnto\",\r\n"
            + "            \"user\": \"batch\"\r\n"
            + "          },\r\n"
            + "          {\r\n"
            + "            \"stav\": \"A\",\r\n"
            + "            \"date\": \"20220811\",\r\n"
            + "            \"license\": \"dnnto\",\r\n"
            + "            \"comment\": \"scheduler/SKC_4b\",\r\n"
            + "            \"user\": \"scheduler\"\r\n"
            + "          }\r\n"
            + "        ]";


    
    @Test
    public void testCase() {
        
        
        Assert.assertTrue(Indexer.historyCase(CASE4ARAW, Case.SKC_4a));
        Assert.assertTrue(Indexer.historyCase(CASE4BRAW, Case.SKC_4b));
        
        Assert.assertFalse(Indexer.historyCase(CASE4ARAW, Case.SKC_4b));
        Assert.assertFalse(Indexer.historyCase(CASE4BRAW, Case.SKC_4a));
    }
}
