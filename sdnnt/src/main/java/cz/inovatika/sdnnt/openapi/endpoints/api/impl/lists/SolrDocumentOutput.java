package cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists;

import java.util.List;
import java.util.Map;

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


    void output(Map<String, Object> outputDocument, List<String> fields, String endpointLicense);

}
