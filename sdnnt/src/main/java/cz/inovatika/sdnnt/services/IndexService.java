package cz.inovatika.sdnnt.services;

import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;


public interface IndexService {
    
    public List<MarcRecord> findById(List<String>  ids) throws SolrServerException;
}
