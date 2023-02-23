package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.GRANULARITY_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MARC_911_U;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MARC_956_U;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.SIGLA_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.YEAR_OF_PUBLICATION_1;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.YEAR_OF_PUBLICATION_2;

import java.io.IOException;
import java.util.Arrays;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Assert;

import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.utils.MarcRecordFields;

public class PXYearServiceImplTest {
    
    public static void main(String[] args) throws SolrServerException, IOException {
        SolrClient client = Indexer.getClient();
        String id1 = "oai:aleph-nkp.cz:DNT01-000329072";
        String id2 = "oai:aleph-nkp.cz:SKC01-006521550";
                
        SolrQuery query = new SolrQuery("identifier:\"" + id2 + "\"")
                .setFields(                    
                    IDENTIFIER_FIELD,
                    SIGLA_FIELD,
                    MARC_911_U,
                    MARC_956_U,
                    GRANULARITY_FIELD,
                    "date1",
                    "date2",
                    YEAR_OF_PUBLICATION_1, 
                    YEAR_OF_PUBLICATION_2)
                .setRows(1)
                .setStart(0);
                //.setFields(String.format("%s, %s", MarcRecordFields.FLAG_PUBLIC_IN_DL, MarcRecordFields.KURATORSTAV_FIELD));

        SolrDocumentList docs = client.query(DataCollections.catalog.name(), query).getResults();
        Assert.assertTrue(docs.getNumFound() == 1);
        
        boolean checkYP1 = PXYearServiceImpl.checkYP1(docs.get(0));
        System.out.println(checkYP1);
        
        
    }
}
