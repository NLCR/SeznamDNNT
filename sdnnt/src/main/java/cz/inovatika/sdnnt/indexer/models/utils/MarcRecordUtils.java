package cz.inovatika.sdnnt.indexer.models.utils;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.ID_OAIIDENT;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.ID_SDNNT;

import java.util.UUID;

import org.apache.solr.common.SolrInputDocument;

import cz.inovatika.sdnnt.index.MD5;

public class MarcRecordUtils {
    
    private MarcRecordUtils() {}
    
    /**
     * Prepare SDNNT identifier; it is only hash of default SKC identifier
     * @param identifier
     * @param sdoc
     */
    public static void derivedIdentifiers(String identifier,SolrInputDocument sdoc) {
        sdoc.setField(IDENTIFIER_FIELD, identifier);
        String[] split = identifier.split(":");
        if (split.length > 0) {
            sdoc.setField(ID_OAIIDENT, split[split.length -1]);
        }
        sdoc.setField(ID_SDNNT, MD5.generate(identifier));
    }
    
    public static final String EUIPO_PREFIX = "euipo:";
    
    public static String generateEUIPOIdent() {
        UUID randomUUID = UUID.randomUUID();
        return EUIPO_PREFIX + randomUUID.toString();
    }
    
    /*
    public static void main(String[] args) {
        System.out.println(generateEUIPOIdent());
    }*/
}
