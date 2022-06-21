package cz.inovatika.sdnnt.model.workflow.duplicate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONObject;

import cz.inovatika.sdnnt.indexer.models.DataField;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.indexer.models.SubField;
import cz.inovatika.sdnnt.model.DataCollections;

public class DuplicateDNTUtils {

    private DuplicateDNTUtils() {}
    
    
    
    public static Pair<Case, List<String>> findDNTFollowers(SolrClient solrClient, MarcRecord origin) throws SolrServerException, IOException {
        List<DataField> marc996 = origin.dataFields.get("996");
        List<DataField> marc035 = origin.dataFields.get("035");
        if (marc996 != null &&  !marc996.isEmpty()) {
            List<SubField> skcIdent = marc996.get(0).subFields.get("a");
            if (!skcIdent.isEmpty()) {
                String marc996a = skcIdent.get(0).getValue().toString();
                String ident = String.format(DuplicateDNTUtils.IDENTIFIER_PATTERN, marc996a);
                SolrQuery idQuery = new SolrQuery(String.format("identifier:\"%s\"", ident)).setRows(1);
                SolrDocumentList results = solrClient.query(DataCollections.catalog.name(), idQuery).getResults();
                if (results.getNumFound() > 0) {
                    SolrDocument solrDoc = results.get(0);
                    boolean req = false;
                    if (!marc035.isEmpty()) {
                        Object raw = solrDoc.get("raw");
                        JSONObject jsonObject = new JSONObject(raw.toString());
                        JSONObject controlFields = jsonObject.getJSONObject("controlFields");
                        if (controlFields.has("001")) {
                            String controlField001 = controlFields.getString("001");
                            if (!checkControl001AndMarc35a(marc035.get(0), controlField001)) {
                                req = true;
                                List<SubField> subFields = marc035.get(0).getSubFields().get("a");
                                // do zadosti 
                                DuplicateUtils.LOGGER.warning(String.format("marc_035a and controlField 001 doesnt match (%s, %s, %s)",subFields.get(0).getValue(), controlField001,  skcIdent.get(0).getValue()));
                            }
                        }
                    }
                    String identifier = MarcRecord.fromSolrDoc(solrDoc).identifier;
                    Pair<Case,List<String>> pair = Pair.of(req ? Case.DNT_2 : Case.DNT_1, Arrays.asList(identifier));
                    return pair;
                } else {
                    return Pair.of(Case.DNT_3, new ArrayList<>());
                }
            } else {
                return Pair.of(Case.DNT_3, new ArrayList<>());
            }
        } else {
            return Pair.of(Case.DNT_3, new ArrayList<>());
        }
    }
    
    
    private static boolean checkControl001AndMarc35a(DataField dataField, String controlField001) {
        List<SubField> subFields = dataField.getSubFields().get("a");
        if (!subFields.isEmpty()) {
            String value = subFields.get(0).getValue();
            if (value.contains(controlField001)) {
                return true;
            }
        }
        
        return false;
    }


    public static final String IDENTIFIER_PATTERN = "oai:aleph-nkp.cz:%s";


}
