package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.ALTERNATIVE_ALEPH_LINK;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.ID_CCNB_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.SET_SPEC_FIELD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.services.UpdateDigitalLibraries;
import cz.inovatika.sdnnt.services.kraminstances.CheckKrameriusConfiguration;
import cz.inovatika.sdnnt.utils.LinksUtilities;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.QuartzUtils;
import cz.inovatika.sdnnt.utils.SolrJUtilities;

public class UpdateDigitalLibrariesImpl implements UpdateDigitalLibraries {

    public static final Logger LOGGER = Logger.getLogger(UpdateAlternativeAlephLinksImpl.class.getName());
    public static final int LIMIT = 30000;
    public static final int BATCH_SIZE = 1000;
    public static final int CHECK_SIZE = 50;

    public UpdateDigitalLibrariesImpl() {}
    
    
    @Override
    public void updateDL() {
        long start = System.currentTimeMillis();
        Options options = getOptions();
        CheckKrameriusConfiguration checkKram = CheckKrameriusConfiguration.initConfiguration(options.getJSONObject("check_kramerius"));

        final Map<String, List<String>> idLibs = new HashMap<>();
        
        try(SolrClient client = buildClient()) {
            Map<String, String> reqMap = new HashMap<>();
            reqMap.put("rows", "" + LIMIT);
            CatalogIterationSupport support = new CatalogIterationSupport();
            
            List<String> plusFilter = new ArrayList<>(Arrays.asList("id_pid:uuid"));
            LOGGER.info("Current iteration filter " + plusFilter);
            support.iterate(client, reqMap, null, plusFilter, new ArrayList<>(), Arrays.asList(
                    IDENTIFIER_FIELD,
                    ID_CCNB_FIELD,
                    "marc_911u","marc_856u", "marc_956u"
            ), (rsp) -> {

                Object identifier = rsp.getFieldValue(IDENTIFIER_FIELD);
                List<String> links = LinksUtilities.linksFromDocument(rsp);
                List<String> digitalizedKeys = LinksUtilities.digitalizedKeys(checkKram, links);
                if (!digitalizedKeys.isEmpty()) {
                    idLibs.put(identifier.toString(), digitalizedKeys);
                }
            }, IDENTIFIER_FIELD);

            List<String> idList = new ArrayList<>(idLibs.keySet());
            
            int numberOfBatches = idLibs.keySet().size() / BATCH_SIZE + (idLibs.size() % BATCH_SIZE == 0 ? 0 :1);
            for (int i = 0; i < numberOfBatches; i++) {
                int startList = i*CHECK_SIZE;
                int endList = Math.min(startList + CHECK_SIZE,idLibs.keySet().size());
                List<String> subList = idList.subList(startList, endList);
                this.update(client, subList, idLibs);
            }

            SolrJUtilities.quietCommit(client, DataCollections.catalog.name());

        } catch (IOException | SolrServerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
        } finally {
            QuartzUtils.printDuration(LOGGER, start);
        }
    }
    
    public void update(SolrClient solr, List<String> identifiers, Map<String,List<String>> mapping) throws  IOException,  SolrServerException {
        if (!identifiers.isEmpty()) {
            UpdateRequest uReq = new UpdateRequest();
            for (String identifier : identifiers) {
                if (mapping.containsKey(identifier)) {
                    SolrInputDocument idoc = new SolrInputDocument();
                    idoc.setField(IDENTIFIER_FIELD, identifier);
                    atomicUpdate(idoc, mapping.get(identifier), MarcRecordFields.DIGITAL_LIBRARIES);
                    uReq.add(idoc);
                }
            }
            if (!uReq.getDocuments().isEmpty()) {
                uReq.process(solr, DataCollections.catalog.name());
            }
            LOGGER.info("Updating identifier "+identifiers);
        }
    }

    protected void atomicUpdate(SolrInputDocument idoc, Object fValue, String fName) {
        Map<String, Object> modifier = new HashMap<>(1);
        modifier.put("set", fValue);
        idoc.addField(fName, modifier);
    }


    protected Options getOptions() {
        return Options.getInstance();
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }

    public static void main(String[] args) {
        UpdateDigitalLibraries lib = new UpdateDigitalLibrariesImpl();
        lib.updateDL();
    }
    
    
}
