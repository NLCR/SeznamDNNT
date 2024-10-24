package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.ID_CCNB_FIELD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.Hash;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.services.UpdateDates;
import cz.inovatika.sdnnt.services.kraminstances.CheckKrameriusConfiguration;
import cz.inovatika.sdnnt.utils.DetectYear;
import cz.inovatika.sdnnt.utils.DetectYear.Bound;
import cz.inovatika.sdnnt.utils.LinksUtilities;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.QuartzUtils;
import cz.inovatika.sdnnt.utils.SolrJUtilities;

public class UpdateDatesImpl implements UpdateDates{


    public static final Logger LOGGER = Logger.getLogger(UpdateAlternativeAlephLinksImpl.class.getName());
    public static final int LIMIT = 30000;
    public static final int BATCH_SIZE = 40000;

    public Logger logger;
    
    public UpdateDatesImpl(String loggerName) {
        if (logger != null) {
            this.logger = Logger.getLogger(loggerName);
        } else {
            this.logger = LOGGER;
        }
    }
    

    @Override
    public void updateDates() {
        long start = System.currentTimeMillis();
        try(SolrClient client = buildClient()) {
            // lower bound
            updateDates("date1", "date1_int", Bound.DOWN, client);
            //upper bound
            updateDates("date2", "date2_int", Bound.UP, client);
            
        } catch (IOException | SolrServerException e) {
            logger.log(Level.SEVERE, e.getMessage(),e);
        } finally {
            QuartzUtils.printDuration(LOGGER, start);
        }
    }


    private void updateDates(String dateFieldStrkey, String dateFieldIntKey, Bound direction, SolrClient client)
            throws SolrServerException, IOException {

        //List<Pair<String,String>> lists = new ArrayList<>();
            
        List<Map<String,String>> lists = new ArrayList<>();
        
        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("rows", "" + LIMIT);
        CatalogIterationSupport support = new CatalogIterationSupport();
        
        List<String> plusFilter = new ArrayList<>(Arrays.asList(
                String.format("-%s:*", dateFieldIntKey),
                "setSpec:SKC",
                String.format("%s:*", dateFieldStrkey)));

        logger.info("Current iteration filter " + plusFilter);
        support.iterate(client, reqMap, null, plusFilter, new ArrayList<>(), Arrays.asList(
                IDENTIFIER_FIELD,
                "date1","date1_int","date2","date2_int"

        ), (rsp) -> {

            Object identifier = rsp.getFieldValue(IDENTIFIER_FIELD);
            Object date1 = rsp.getFieldValue("date1");
            Object date2 = rsp.getFieldValue("date2");
            
            Map<String,String> doc  = new HashMap<>();
            doc.put(IDENTIFIER_FIELD, identifier.toString());
            doc.put("date1", date1.toString());
            doc.put("date2", date2.toString());
            
            lists.add(doc);
            
        }, IDENTIFIER_FIELD);

        logger.info("Number of records:"+lists.size());
        int numberOfBatches = lists.size() / BATCH_SIZE + (lists.size() % BATCH_SIZE == 0 ? 0 :1);
        for (int i = 0; i < numberOfBatches; i++) {
            int startList = i*BATCH_SIZE;
            int endList = Math.min(startList + BATCH_SIZE,lists.size());

            UpdateRequest uReq = new UpdateRequest();

            List<Map<String,String>> subList = lists.subList(startList, endList);
            for (Map<String,String> srcDoc : subList) {

                SolrInputDocument idoc = new SolrInputDocument();
                idoc.setField(IDENTIFIER_FIELD, srcDoc.get(IDENTIFIER_FIELD));
                
                // pokud horni mez obsahuje mezery, pak doplnit to co je v horni 
                if (dateFieldStrkey.equals("date2") && srcDoc.get(dateFieldStrkey).trim().equals("")) {
                    srcDoc.put("date2", srcDoc.get("date1"));
                }

                int dateInt = DetectYear.detectYear(srcDoc.get(dateFieldStrkey), direction);
                SolrJUtilities.atomicSet(idoc, dateInt, dateFieldIntKey);
                uReq.add(idoc);

            }
            UpdateResponse response = uReq.process(client, DataCollections.catalog.name());
            int status = response.getStatus();
            if (status != 200) {
                getLogger().info(String.format("Updated retval  %d", status));
            }
            this.logger.info("Commiting: "+(i*BATCH_SIZE + subList.size()));
            SolrJUtilities.quietCommit(client, DataCollections.catalog.name());
        }
    }

    
    public Logger getLogger() {
        return logger;
    }
    
    protected Options getOptions() {
        return Options.getInstance();
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }

    public static void main(String[] args) {
        UpdateDatesImpl dates = new UpdateDatesImpl("test");
        dates.updateDates();
    }

}
