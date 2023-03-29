package cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import cz.inovatika.sdnnt.utils.MarcRecordFields;

@FunctionalInterface
public interface SolrDocumentOutput {

    public static final String PID_KEY = "pid";
    public static final String SELECTED_INSTITUTION_KEY = "institution";
    public static final String SELECTED_DL_KEY = "dl";

    public static final String LABEL_KEY = "label";
    public static final String NAZEV_KEY = "title";
    public static final String IDENTIFIER_KEY = "identifier";
    public static final String SDNNT_ID_KEY = MarcRecordFields.ID_SDNNT;

    public static final String FMT_KEY = "fmt";
    public static final String PIDS_KEY = "pids";
    public static final String GRANUARITY_KEY = "granularity";

    public static final String DNTSTAV_KEY  = MarcRecordFields.DNTSTAV_FIELD;
    
    // SKC properties
    //public static final String RAW_KEY = MarcRecordFields.RAW_FIELD;
    public static final String CONTROL_FIELD_001_KEY = "controlfield_001";
    public static final String CONTROL_FIELD_003_KEY = "controlfield_003";
    public static final String CONTROL_FIELD_005_KEY = "controlfield_005";
    public static final String CONTROL_FIELD_007_KEY = "controlfield_007";
    public static final String CONTROL_FIELD_008_KEY = "controlfield_008";

    public static final String RAW_KEY = MarcRecordFields.RAW_FIELD;

    // vystupni metoda
    void output(Pair<String,String> digitalLibraryFilter, Map<String, Object> outputDocument, List<String> fields, String endpointLicense, boolean doNotEmitParent);

}
