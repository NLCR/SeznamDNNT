package cz.inovatika.sdnnt.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.workflow.duplicate.Case;
import cz.inovatika.sdnnt.services.SKCDeleteService;

public class SKCUpdateSupportServiceImpl extends AbstractCheckDeleteService implements SKCDeleteService {

    protected List<String> deletedInfo = new ArrayList<>();
    
    protected Logger logger = Logger.getLogger(SKCUpdateSupportServiceImpl.class.getName());

    public SKCUpdateSupportServiceImpl(String loggerName, JSONObject results, List<String> deletedInfo) {
        super(loggerName, results);
        this.deletedInfo = deletedInfo;
        if (loggerName != null) {
            this.logger = Logger.getLogger(loggerName);
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

    public void updateDeleteInfo(List<String> deleteInfo) {
        deleteInfo.stream().forEach(info-> {
            if (!this.deletedInfo.contains(info)) {
                this.deletedInfo.add(info);
            }
        });
    }
    
    protected void deleteRecords(List<String> identifiers) throws IOException, SolrServerException {
    }

    @Override
    protected Map<Case, List<Pair<String, List<String>>>> checkUpdate() throws IOException, SolrServerException {
        Map<Case, List<Pair<String, List<String>>>> retvals = new HashMap<>();
        try (final SolrClient solrClient = buildClient()) {
           for (int i = 0; i < this.deletedInfo.size(); i++) {
               if (!retvals.containsKey(Case.SKC_4a)) {
                   retvals.put(Case.SKC_4a, new ArrayList<>());
               }
               String id = this.deletedInfo.get(i);
               SolrQuery idQuery = new SolrQuery(String.format("identifier:\"%s\"", id));
               idQuery = idQuery.addFilterQuery("(NOT dntstav:D OR NOT kuratorstav:DX)").setRows(100);
               SolrDocumentList results = solrClient.query(DataCollections.catalog.name(), idQuery).getResults();
               if (results.getNumFound() == 1) {
                   retvals.get(Case.SKC_4a).add(Pair.of(id, new ArrayList<>()));
               } else {
                   getLogger().info(String.format("Title %s has already state D or DX", id));
               }
               
           }
        }
        getLogger().info(String.format("Check update %s", retvals.toString()));
        return retvals;
    }

    @Override
    protected List<String> checkDelete() throws IOException, SolrServerException {
        getLogger().info(String.format("Check delete %s", new ArrayList<String>().toString()));
        return new ArrayList<>();
    }
}
