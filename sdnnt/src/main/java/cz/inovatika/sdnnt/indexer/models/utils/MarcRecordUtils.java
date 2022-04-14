package cz.inovatika.sdnnt.indexer.models.utils;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.ID_OAIIDENT;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.ID_SDNNT;

import org.apache.solr.common.SolrInputDocument;

import cz.inovatika.sdnnt.index.MD5;

public class MarcRecordUtils {
    
    private MarcRecordUtils() {}
    
    public static void derivedIdentifiers(String identifier,SolrInputDocument sdoc) {
        sdoc.setField(IDENTIFIER_FIELD, identifier);
        String[] split = identifier.split(":");
        if (split.length > 0) {
            sdoc.setField(ID_OAIIDENT, split[split.length -1]);
        }
        sdoc.setField(ID_SDNNT, MD5.generate(identifier));
    }
}
