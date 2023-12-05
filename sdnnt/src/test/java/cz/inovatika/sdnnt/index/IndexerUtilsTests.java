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


    
    public static final String CASE4a_FROM_SOLR = "[{\"stav\":\"PA\",\"date\":\"20200731\",\"license\":\"dnnto\",\"user\":\"batch\"},{\"stav\":\"A\",\"date\":\"20210201\",\"license\":\"dnnto\",\"user\":\"batch\"},{\"stav\":\"A\",\"date\":\"20220726\",\"license\":\"dnnto\",\"comment\":\"scheduler/SKC_4b\",\"user\":\"scheduler\"},{\"stav\":\"D\",\"date\":\"20220726\",\"zadost\":\"cd1676e3-4ffb-4e68-8ec8-01d8e2141528\",\"comment\":\"Schváleno, jde o hudebninu, viz SKC 009568289. Špatné odůvodnění má být SKC_4a.\",\"user\":\"Kurator-DC\"}]";
    public static final String CASE4a_FROM_SOLR2 ="[{\"stav\":\"PA\",\"date\":\"20210131\",\"license\":\"dnnto\",\"user\":\"batch\"},{\"stav\":\"A\",\"date\":\"20210801\",\"license\":\"dnnto\",\"user\":\"batch\"},{\"stav\":\"A\",\"date\":\"20230123\",\"license\":\"dnnto\",\"comment\":\"scheduler/SKC_4a\",\"user\":\"scheduler\"},{\"stav\":\"D\",\"date\":\"20230123\",\"zadost\":\"1bff1f20-09ed-4c43-8955-a6a1479932dd\",\"comment\":\"Změna země vydání - vydáno mimo území ČR\",\"user\":\"Kurator-DC\"}]";
    
    
    @Test
    public void testCase() {
        
        
        Assert.assertTrue(Indexer.historyCase(CASE4ARAW, Case.SKC_4a));
        Assert.assertTrue(Indexer.historyCase(CASE4BRAW, Case.SKC_4b));
        
        Assert.assertFalse(Indexer.historyCase(CASE4ARAW, Case.SKC_4b));
        Assert.assertFalse(Indexer.historyCase(CASE4BRAW, Case.SKC_4a));
    }

    @Test
    public void testCase4a() {
        Assert.assertTrue(Indexer.historyCase(CASE4a_FROM_SOLR, Case.SKC_4b));
        Assert.assertTrue(Indexer.historyCase(CASE4a_FROM_SOLR2, Case.SKC_4a));
    }
}
