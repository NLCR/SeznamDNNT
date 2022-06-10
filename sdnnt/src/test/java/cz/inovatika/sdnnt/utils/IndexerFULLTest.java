package cz.inovatika.sdnnt.utils;

import cz.inovatika.sdnnt.index.OAIHarvester;
import org.json.JSONObject;

// vytvorit utilitu
public class IndexerFULLTest {

    public static void main(String[] args) {
        OAIHarvester oai = new OAIHarvester();
        String set = "SKC";
        String core = "catalog";

        oai.full(set, core, false, false,  false);
    }
}
