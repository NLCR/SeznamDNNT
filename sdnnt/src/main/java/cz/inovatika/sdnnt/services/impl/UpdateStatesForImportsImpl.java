/*
 * Copyright (C) 2025  Inovatika
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.ImportDocsIterationSupport;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.services.UpdateStatesForImports;
import cz.inovatika.sdnnt.utils.*;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.KURATORSTAV_FIELD;

public class UpdateStatesForImportsImpl implements UpdateStatesForImports {

    public static final Logger LOGGER = Logger.getLogger(UpdateAlternativeAlephLinksImpl.class.getName());
    //public static final  List<String> ALLOWED_STATES = Arrays.asList("A", "PA", "NL");

    public Logger logger;

    public UpdateStatesForImportsImpl(String loggerName) {
        if (logger != null) {
            this.logger = Logger.getLogger(loggerName);
        } else {
            this.logger = LOGGER;
        }
    }


    @Override
    public void updateImports() {
        long start = System.currentTimeMillis();
        try(SolrClient client = buildClient()) {
            UpdateRequest updateRequest = new UpdateRequest();
            ImportDocsIterationSupport support = new ImportDocsIterationSupport();
            Map<String,String> reqMap = new HashMap<>();
            reqMap.put("rows", ""+1000);


            support.iterate( client, reqMap, null, Arrays.asList("hits_na_vyrazeni:[1 TO *]"), new ArrayList<String>(), Arrays.asList("id","identifiers", "dntstav", "na_vyrazeni"), (rsp) -> {
                Object docsId = rsp.getFieldValue("id");

                //Object ids = rsp.getFieldValue("identifiers");
                List<String> importStates = (List<String>) rsp.getFieldValue("dntstav");
                List<String> naVyrazeni = (List<String>) rsp.getFieldValue("na_vyrazeni");
                List<String> realStates = new ArrayList<>();
                List<String> identifiers = new ArrayList<>();


                if (naVyrazeni != null) { identifiers.addAll(naVyrazeni); }

                int batchSize = 40;
                int number = identifiers.size() / batchSize;
                number = number + ((identifiers.size() % batchSize == 0) ? 0 : 1);

                for (int j = 0; j < number; j++) {
                    int startOffset = j * batchSize;
                    int endOffset = Math.min((j + 1) * batchSize, identifiers.size());
                    List<String> sublist = identifiers.subList(startOffset, endOffset);
                    String collect = sublist.stream().map(it -> '"' + it + '"').collect(Collectors.joining(" OR "));

                    SolrQuery catalogQuery = null;
                    try {
                        catalogQuery = new SolrQuery("*")
                                .setRows(sublist.size())
                                .setStart(0)
                                .addFilterQuery("identifier:(" + collect + ")")
                                .setFields("identifier,"
                                        + "dntstav");


                        QueryResponse catalogResp = client.query(DataCollections.catalog.name(),catalogQuery);
                        long numFound = catalogResp.getResults().getNumFound();
                        for (int i = 0; i < numFound; i++) {
                            SolrDocument doc = catalogResp.getResults().get(i);
                            List<String> rStav = (List<String>) doc.getFieldValue(MarcRecordFields.DNTSTAV_FIELD);
                            if (rStav != null && !rStav.isEmpty()) {
                                realStates.addAll(rStav);
                            }
                        }

                    } catch (SolrServerException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                if ( (realStates != null && !realStates.equals(importStates)) || (realStates == null && importStates != null) ) {
                    SolrInputDocument idoc = new SolrInputDocument();
                    idoc.setField("id", docsId);

                    if (realStates != null) {
                        SolrJUtilities.atomicSet(idoc, realStates, "dntstav");
                    } else {
                        SolrJUtilities.atomicSet(idoc, null, "dntstav");
                    }
                    updateRequest.add(idoc);
                }

            }, "id");

            if (updateRequest.getDocuments() != null &&  !updateRequest.getDocuments().isEmpty()) {
                LOGGER.info("Number of updating imports: "+updateRequest.getDocuments().size());
                updateRequest.process(client, DataCollections.imports_documents.name());
                client.commit(DataCollections.imports_documents.name());
            }
        } catch (IOException  e) {
            logger.log(Level.SEVERE, e.getMessage(),e);
        } catch (SolrServerException e) {
            logger.log(Level.SEVERE, e.getMessage(),e);
        } finally {
            QuartzUtils.printDuration(LOGGER, start);
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
        UpdateStatesForImportsImpl dates = new UpdateStatesForImportsImpl("test");
        dates.updateImports();
    }
}
