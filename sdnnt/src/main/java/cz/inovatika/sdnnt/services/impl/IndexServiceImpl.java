package cz.inovatika.sdnnt.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.services.IndexService;
import cz.inovatika.sdnnt.utils.MarcRecordFields;

public class IndexServiceImpl implements IndexService {
    
    public static final Logger LOGGER = Logger.getLogger(IndexServiceImpl.class.getName());
    
    @Override
    public List<MarcRecord> findById(List<String> ids) throws SolrServerException {
        List<MarcRecord> retList = new ArrayList<>();
        if (ids != null && !ids.isEmpty()) {
            try (final SolrClient solrClient = buildClient()) {
                String query = "("+ids.stream().map(it-> {
                    return '"'+it+'"';  
                }).collect(Collectors.joining(" OR "))+")";
                SolrQuery sQuery = new SolrQuery(MarcRecordFields.IDENTIFIER_FIELD+":"+query)
                        .setRows(100)
                        .setStart(0);
                
                SolrDocumentList docs = solrClient.query(DataCollections.catalog.name(), sQuery).getResults();
                if (docs.getNumFound()> 0) {
                    for (int i = 0; i < docs.getNumFound(); i++) {
                        SolrDocument doc = docs.get(i);
                        retList.add(MarcRecord.fromSolrDoc(doc));
                    }
                }
            } catch(IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return retList;
    }

    protected Options getOptions() {
        return Options.getInstance();
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }

    
    
}
