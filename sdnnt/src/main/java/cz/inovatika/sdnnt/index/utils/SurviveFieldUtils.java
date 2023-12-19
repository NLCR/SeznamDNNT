package cz.inovatika.sdnnt.index.utils;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

import cz.inovatika.sdnnt.indexer.models.utils.MarcRecordUtils;
import cz.inovatika.sdnnt.utils.MarcRecordFields;

public class SurviveFieldUtils {
    
    private SurviveFieldUtils() {}
    
    /** Pole, ktera musi zustat po update zaznamu */
    public static void surviveFields(SolrDocument doc, SolrInputDocument cDoc) {
        ensureAndSetField(doc, cDoc, MarcRecordFields.DNTSTAV_FIELD);
        ensureAndSetField(doc, cDoc, MarcRecordFields.KURATORSTAV_FIELD);
        ensureAndSetField(doc, cDoc, MarcRecordFields.HISTORIE_GRANULOVANEHOSTAVU_FIELD);
        ensureAndSetField(doc, cDoc, MarcRecordFields.DATUM_STAVU_FIELD);
        ensureAndSetField(doc, cDoc, MarcRecordFields.DATUM_KURATOR_STAV_FIELD);
        ensureAndSetField(doc, cDoc, MarcRecordFields.HISTORIE_STAVU_FIELD);
        ensureAndSetField(doc, cDoc, MarcRecordFields.HISTORIE_KURATORSTAVU_FIELD);
        ensureAndSetField(doc, cDoc, MarcRecordFields.LICENSE_FIELD);
        ensureAndSetField(doc, cDoc, MarcRecordFields.LICENSE_HISTORY_FIELD);
        ensureAndSetField(doc, cDoc, MarcRecordFields.GRANULARITY_FIELD);
        ensureAndSetField(doc, cDoc, MarcRecordFields.FLAG_PUBLIC_IN_DL);
        ensureAndSetField(doc, cDoc, MarcRecordFields.ALTERNATIVE_ALEPH_LINK);
        ensureAndSetField(doc, cDoc, MarcRecordFields.MASTERLINKS_FIELD);
        ensureAndSetField(doc, cDoc, MarcRecordFields.MASTERLINKS_DISABLED_FIELD);
        
        // musi zustat vlastnosti pro export euipo
        ensureAndSetField(doc, cDoc,MarcRecordFields.ID_EUIPO);
        ensureAndSetField(doc, cDoc,MarcRecordFields.ID_EUIPO_CANCELED);
        ensureAndSetField(doc, cDoc,MarcRecordFields.ID_EUIPO_LASTACTIVE);
        ensureAndSetField(doc, cDoc,MarcRecordFields.EXPORT);
        ensureAndSetField(doc, cDoc,MarcRecordFields.ID_EUIPO_EXPORT);
        ensureAndSetField(doc, cDoc,MarcRecordFields.ID_EUIPO_EXPORT_ACTIVE);

        ensureAndSetField(doc, cDoc,MarcRecordFields.CURATOR_ACTIONS);
        
        
        MarcRecordUtils.derivedIdentifiers(doc.getFieldValue(MarcRecordFields.IDENTIFIER_FIELD).toString(), cDoc);
    }

    
    
    private static void ensureAndSetField(SolrDocument doc, SolrInputDocument cDoc, String field) {
        if (doc.containsKey(field)) {
            cDoc.setField(field, doc.getFieldValue(field));
        }
    }
}
