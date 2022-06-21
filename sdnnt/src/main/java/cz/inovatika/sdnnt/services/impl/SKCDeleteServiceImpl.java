package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.DNTSTAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.workflow.duplicate.Case;
import cz.inovatika.sdnnt.model.workflow.duplicate.DuplicateDNTUtils;
import cz.inovatika.sdnnt.model.workflow.duplicate.DuplicateSKCUtils;
import cz.inovatika.sdnnt.services.SKCDeleteService;
import cz.inovatika.sdnnt.utils.MarcRecordFields;

public class SKCDeleteServiceImpl extends AbstractCheckDeleteService implements SKCDeleteService {

    protected List<String> deletedInfo;
    
    protected Logger logger = Logger.getLogger(SKCDeleteServiceImpl.class.getName());

    public SKCDeleteServiceImpl(String loggerName, JSONObject results, List<String> deletedInfo) {
        super(loggerName, results);
        this.deletedInfo = deletedInfo;
        if (loggerName != null) {
            this.logger = Logger.getLogger(loggerName);
        }
    }

    public SKCDeleteServiceImpl(String loggerName, JSONObject results) {
        super(loggerName, results);
        if (loggerName != null) {
            this.logger = Logger.getLogger(loggerName);
        }
    }

    
    
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void updateDeleteInfo(List<String> deleteInfo) {
        this.deletedInfo = deleteInfo;
    }
    
    protected void deleteRecords(List<String> identifiers) throws IOException, SolrServerException {
        if (identifiers != null && !identifiers.isEmpty()) {
            try(SolrClient sClient = buildClient()) {
                sClient.deleteById(DataCollections.catalog.name(),  identifiers);
            }
        }
    }

    
    @Override
    public Logger getLogger() {
        return this.logger;
    }

    public Options getOptions() {
        return Options.getInstance();
    }

    public SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }

    
    @Override
    protected Map<Case, List<Pair<String, List<String>>>> checkUpdate() throws IOException, SolrServerException {
        Map<Case, List<Pair<String, List<String>>>> retvals = new HashMap<>();
        try (final SolrClient solrClient = buildClient()) {
           for (int i = 0; i < this.deletedInfo.size(); i++) {
               String id = this.deletedInfo.get(i);
               SolrQuery zadostQuery = new SolrQuery(String.format("identifiers:\"%s\"", id))
                       .addFilterQuery("navrh:DXN")
                       .setRows(1)
                   .setStart(0);
               SolrDocumentList zadosti = solrClient.query(DataCollections.zadost.name(), zadostQuery).getResults();
               if (zadosti.getNumFound() == 0) {
                   SolrQuery query = new SolrQuery(String.format("identifier:\"%s\"", id))
                           .addFilterQuery("dntstav:*")
                           .addFilterQuery("NOT "+MarcRecordFields.KURATORSTAV_FIELD+":DX")
                           .addFilterQuery("NOT "+MarcRecordFields.KURATORSTAV_FIELD+":D")
                           .setRows(1)
                       .setStart(0);
                   SolrDocumentList docs = solrClient.query(DataCollections.catalog.name(), query).getResults();
                   if (docs.getNumFound()  == 1) {
                       SolrDocument solrDocument = docs.get(0);
                       MarcRecord fromIndex = MarcRecord.fromSolrDoc(solrDocument);
                       Pair<Case,List<String>> followers = DuplicateSKCUtils.findSKCFollowers(solrClient, fromIndex);
                       if (!retvals.containsKey(followers.getKey())) {
                           retvals.put(followers.getKey(),new ArrayList<>());
                       }
                       retvals.get(followers.getKey()).add(Pair.of(fromIndex.identifier, followers.getRight()));
                   }
               }
           }
        }
        return retvals;
    }

    @Override
    protected List<String> checkDelete() throws IOException, SolrServerException {
        List<String> list = new ArrayList<>();
        try (final SolrClient solrClient = buildClient()) {
           for (int i = 0; i < this.deletedInfo.size(); i++) {
               String id = this.deletedInfo.get(i);
               SolrQuery query = new SolrQuery(String.format("identifier:\"%s\"", id))
                       .addFilterQuery("NOT dntstav:*")
                       .setRows(1)
                   .setStart(0);
               SolrDocumentList docs = solrClient.query(DataCollections.catalog.name(), query).getResults();
               if (docs.getNumFound()  == 1) {
                   list.add(docs.get(0).getFieldValue(MarcRecordFields.IDENTIFIER_FIELD).toString());
               }
           }
        }
        return list;
    }

}
