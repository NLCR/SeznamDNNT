package cz.inovatika.sdnnt.services.kraminstances;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.junit.Test;

import cz.inovatika.sdnnt.services.impl.kramerius.granularities.Granularity;
import cz.inovatika.sdnnt.services.impl.kramerius.granularities.GranularityField;

public class GranularityTest {
    
    /*
    {"stav":["X"],"cislo":"VI","link":"https://kramerius.lib.cas.cz/uuid/uuid:cce376e2-3c8d-11e1-8339-001143e3f55c","kuratorstav":["X"],"rocnik":"1961"}
    {"stav":["X"],"cislo":"VII","link":"https://kramerius.lib.cas.cz/uuid/uuid:cd9784a2-3c8d-11e1-1729-001143e3f55c","kuratorstav":["X"],"rocnik":"1962"}
    {"stav":["X"],"cislo":"VIII","link":"https://kramerius.lib.cas.cz/uuid/uuid:ce4a80f2-3c8d-11e1-1586-001143e3f55c","kuratorstav":["X"],"rocnik":"1963"}
    {"stav":["X"],"cislo":"IX","link":"https://kramerius.lib.cas.cz/uuid/uuid:cf00ffb2-3c8d-11e1-1586-001143e3f55c","kuratorstav":["X"],"rocnik":"1964"}
    {"stav":["X"],"cislo":"X","link":"https://kramerius.lib.cas.cz/uuid/uuid:cfb53482-3c8d-11e1-8339-001143e3f55c","kuratorstav":["X"],"rocnik":"1965"}
    {"stav":["X"],"cislo":"XI","link":"https://kramerius.lib.cas.cz/uuid/uuid:d06dfd32-3c8d-11e1-1729-001143e3f55c","kuratorstav":["X"],"rocnik":"1966"}
    {"stav":["X"],"cislo":"XII","link":"https://kramerius.lib.cas.cz/uuid/uuid:d12650b2-3c8d-11e1-8339-001143e3f55c","kuratorstav":["X"],"rocnik":"1967"}
    {"stav":["X"],"cislo":"XIII","link":"https://kramerius.lib.cas.cz/uuid/uuid:d1decb42-3c8d-11e1-8339-001143e3f55c","kuratorstav":["X"],"rocnik":"1968"}
    {"stav":["X"],"cislo":"XIV","link":"https://kramerius.lib.cas.cz/uuid/uuid:d295bf32-3c8d-11e1-8339-001143e3f55c","kuratorstav":["X"],"rocnik":"1969"}
    {"stav":["X"],"cislo":"XV","link":"https://kramerius.lib.cas.cz/uuid/uuid:d34c3df2-3c8d-11e1-8339-001143e3f55c","kuratorstav":["X"],"rocnik":"1970"}
    {"stav":["X"],"cislo":"XVI","link":"https://kramerius.lib.cas.cz/uuid/uuid:d3fffd92-3c8d-11e1-1586-001143e3f55c","kuratorstav":["X"],"rocnik":"1971"}
    {"stav":["X"],"cislo":"XVII","link":"https://kramerius.lib.cas.cz/uuid/uuid:d4b1e872-3c8d-11e1-8339-001143e3f55c","kuratorstav":["X"],"rocnik":"1972"}
    {"stav":["X"],"cislo":"XVIII","link":"https://kramerius.lib.cas.cz/uuid/uuid:d5642172-3c8d-11e1-8339-001143e3f55c","kuratorstav":["X"],"rocnik":"1973"}
    {"stav":["X"],"cislo":"XIX","link":"https://kramerius.lib.cas.cz/uuid/uuid:d613e972-3c8d-11e1-1729-001143e3f55c","kuratorstav":["X"],"rocnik":"1974"}
    {"stav":["X"],"cislo":"XX","link":"https://kramerius.lib.cas.cz/uuid/uuid:d6c2c712-3c8d-11e1-8339-001143e3f55c","kuratorstav":["X"],"rocnik":"1975"}
    {"stav":["X"],"cislo":"XXI","link":"https://kramerius.lib.cas.cz/uuid/uuid:d7750012-3c8d-11e1-1729-001143e3f55c","kuratorstav":["X"],"rocnik":"1976"}
    {"stav":["X"],"cislo":"XXII","link":"https://kramerius.lib.cas.cz/uuid/uuid:d8269cd2-3c8d-11e1-1586-001143e3f55c","kuratorstav":["X"],"rocnik":"1977"}
    {"stav":["X"],"cislo":"XXIII","link":"https://kramerius.lib.cas.cz/uuid/uuid:d8de5412-3c8d-11e1-8339-001143e3f55c","kuratorstav":["X"],"rocnik":"1978"}
    {"stav":["X"],"cislo":"XXIV","link":"https://kramerius.lib.cas.cz/uuid/uuid:d9960b52-3c8d-11e1-1586-001143e3f55c","kuratorstav":["X"],"rocnik":"1979"}
    {"stav":["X"],"cislo":"XXV","link":"https://kramerius.lib.cas.cz/uuid/uuid:da4ab552-3c8d-11e1-8339-001143e3f55c","kuratorstav":["X"],"rocnik":"1980"}
    {"stav":["X"],"cislo":"XXVI","link":"https://kramerius.lib.cas.cz/uuid/uuid:db01a942-3c8d-11e1-8339-001143e3f55c","kuratorstav":["X"],"rocnik":"1981"}
    {"stav":["X"],"cislo":"XXVII","link":"https://kramerius.lib.cas.cz/uuid/uuid:dbb91262-3c8d-11e1-8339-001143e3f55c","kuratorstav":["X"],"rocnik":"1982"}
    {"stav":["X"],"cislo":"XXVIII","link":"https://kramerius.lib.cas.cz/uuid/uuid:dc71db12-3c8d-11e1-8339-001143e3f55c","kuratorstav":["X"],"rocnik":"1983"}
    {"stav":["X"],"cislo":"XXIX","link":"https://kramerius.lib.cas.cz/uuid/uuid:dd27bd92-3c8d-11e1-8339-001143e3f55c","kuratorstav":["X"],"rocnik":"1984"}
    {"stav":["X"],"cislo":"XXX","link":"https://kramerius.lib.cas.cz/uuid/uuid:ddddc722-3c8d-11e1-8339-001143e3f55c","kuratorstav":["X"],"rocnik":"1985"}
    {"stav":["X"],"cislo":"XXXI","link":"https://kramerius.lib.cas.cz/uuid/uuid:de93f7c2-3c8d-11e1-8339-001143e3f55c","kuratorstav":["X"],"rocnik":"1986"}
    {"stav":["X"],"cislo":"XXXII","link":"https://kramerius.lib.cas.cz/uuid/uuid:df4baf02-3c8d-11e1-1586-001143e3f55c","kuratorstav":["X"],"rocnik":"1987"}
    {"stav":["X"],"cislo":"XXXIII","link":"https://kramerius.lib.cas.cz/uuid/uuid:dfff6ea2-3c8d-11e1-1729-001143e3f55c","kuratorstav":["X"],"rocnik":"1988"}
    {"stav":["X"],"cislo":"XXXIV","link":"https://kramerius.lib.cas.cz/uuid/uuid:e0b689a2-3c8d-11e1-8339-001143e3f55c","kuratorstav":["X"],"rocnik":"1989"}
    {"stav":["X"],"cislo":"XXXV","link":"https://kramerius.lib.cas.cz/uuid/uuid:e16e19d2-3c8d-11e1-1729-001143e3f55c","kuratorstav":["X"],"rocnik":"1990"}
    */
    
    @Test
    public void testGranularity() {
        // root pid
        List<String> jsons = Arrays.asList(
                "{\"stav\":[\"X\"],\"link\":\"https://kramerius.lib.cas.cz/uuid/uuid:c81615f8-3c8d-11e1-87b4-005056a60003\",\"kuratorstav\":[\"X\"]}",
                "{\"stav\":[\"X\"],\"cislo\":\"I\",\"link\":\"https://kramerius.lib.cas.cz/uuid/uuid:c9552002-3c8d-11e1-8339-001143e3f55c\",\"kuratorstav\":[\"X\"],\"rocnik\":\"1956\"}",
                "{\"stav\":[\"X\"],\"cislo\":\"II\",\"link\":\"https://kramerius.lib.cas.cz/uuid/uuid:ca0c6212-3c8d-11e1-8339-001143e3f55c\",\"kuratorstav\":[\"X\"],\"rocnik\":\"1957\"}",
                "{\"stav\":[\"X\"],\"cislo\":\"III\",\"link\":\"https://kramerius.lib.cas.cz/uuid/uuid:cac18142-3c8d-11e1-8339-001143e3f55c\",\"kuratorstav\":[\"X\"],\"rocnik\":\"1958\"}",
                "{\"stav\":[\"X\"],\"cislo\":\"IV\",\"link\":\"https://kramerius.lib.cas.cz/uuid/uuid:cb79fbd2-3c8d-11e1-8339-001143e3f55c\",\"kuratorstav\":[\"X\"],\"rocnik\":\"1959\"}",
                "{\"stav\":[\"X\"],\"cislo\":\"V\",\"link\":\"https://kramerius.lib.cas.cz/uuid/uuid:cc2caa02-3c8d-11e1-8339-001143e3f55c\",\"kuratorstav\":[\"X\"],\"rocnik\":\"1960\"}"
        );
        List<GranularityField> collected = jsons.stream().map(it-> {
            return new JSONObject(it);
        }).map(GranularityField::initFromSDNNTSolrJson).collect(Collectors.toList());
        
        Granularity granularity = new Granularity("oai:aleph-nkp.cz:DNT01-000159263");
        collected.stream().forEach(gf-> {
            granularity.addGranularityField(gf);
        });
        
        
        System.out.println(granularity);
    }
}
