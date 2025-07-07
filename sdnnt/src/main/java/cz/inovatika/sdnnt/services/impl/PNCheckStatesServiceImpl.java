package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.DATUM_KURATOR_STAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.EAN_FIELD;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.index.utils.imports.ImporterUtils;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.model.workflow.ZadostTypNavrh;
import cz.inovatika.sdnnt.services.PNCheckStatesService;
import cz.inovatika.sdnnt.utils.RequestsUtils;
import org.json.JSONObject;

public class PNCheckStatesServiceImpl extends AbstractRequestService implements PNCheckStatesService{

    
    private Logger logger = Logger.getLogger(PNCheckStatesServiceImpl.class.getName());

    public static final int LIMIT = 1000;
    private int checkPNStates;
    private String chronoUnit;
    
    
    private List<Pair<String,String>> processors = null;
    
    

    public PNCheckStatesServiceImpl(String logger, List<Pair<String,String>> prcs, int checkPNStates, String chronoUnit) {
        if (logger != null) {
            this.logger = Logger.getLogger(logger);
        }
        
        if (checkPNStates > -1) {
            this.checkPNStates = checkPNStates;
            this.chronoUnit = chronoUnit;
            getLogger().info(String.format("Older than %d %s",  this.checkPNStates, this.chronoUnit));
        }
        this.processors = prcs;
        this.typeOfRequest = ZadostTypNavrh.NZN.name();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public List<Pair<String, String>> check() {

        AccountService accountService = new AccountServiceImpl();

        CatalogIterationSupport support = new CatalogIterationSupport();
        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("rows", "" + LIMIT);

        List<String> plusFilter = new ArrayList<>(Arrays.asList(
                "dntstav:"+PublicItemState.N.name(),
                "kuratorstav:"+CuratorItemState.PN.name()
        ));
        
        Instant inst = Instant.now();
        
        List<Pair<String, String>> pairs = new ArrayList<>();
        List<String> allUsedIdentifiers = new ArrayList<>();

        
        
        logger.info("Current iteration filter " + plusFilter);
        try (final SolrClient solrClient = buildClient()) {
            
            
            support.iterate(solrClient, reqMap, null, plusFilter, new ArrayList<>(), Arrays.asList(
                    IDENTIFIER_FIELD,
                    EAN_FIELD,
                    DATUM_KURATOR_STAV_FIELD
            ), (rsp) -> {
                
                
                Object identifier = rsp.getFieldValue(IDENTIFIER_FIELD);
                Date datumKuratorStav = (Date) rsp.getFieldValue(DATUM_KURATOR_STAV_FIELD);
                Object ean = rsp.getFieldValue(EAN_FIELD);

                getLogger().info(String.format( "Testing identifier %s", identifier.toString()));

                Instant parsedInstant = datumKuratorStav.toInstant();
                ChronoUnit selected = getChronoUnit();

                // If date of curator state is bigger then configuration value
                if (ImporterUtils.calculateInterval(parsedInstant, inst, selected) > this.checkPNStates) {
                    SolrQuery q = (new SolrQuery("*"))
                            .addFilterQuery("na_vyrazeni:\""+identifier.toString()+"\"")
                            .addFilterQuery("controlled:true")
                            .addSort(SortClause.desc("indextime"))
                            .setRows(1);


                    try {
                        QueryResponse resp = solrClient.query(DataCollections.imports_documents.name(), q);
                        SolrDocumentList results = resp.getResults();
                        if (results.size() > 0) {
                            String foundEan = (String) results.get(0).getFieldValue("ean");
                            if (foundEan != null) {
                                getLogger().info(String.format("Adding %s",  identifier.toString()));
                                Pair<String,String> pair = Pair.of(identifier.toString(), foundEan);
                                pairs.add(pair);
                            }
                        }
                    } catch (SolrServerException | IOException e) {
                        getLogger().severe(String.format("No ean %s, skipping",  identifier.toString()));
                    }
                } else {
                    getLogger().info(String.format("New PN state %s, skipping",  identifier.toString()));
                }
                
            }, IDENTIFIER_FIELD);

            // remove used identifiers - date conndition
            List<String> pidsToRemove = removeUsedIdentifiersDateCondition(pairs.stream().map(Pair::getLeft).collect(Collectors.toList()), accountService, inst);
            allUsedIdentifiers.addAll(pidsToRemove);

        } catch(IOException | AccountException | SolrServerException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        Map<String,List<Pair<String,String>>> processingMap = new HashMap<>();
        pairs.forEach(p ->{
            String ean = p.getRight();
            if (!allUsedIdentifiers.contains(p.getLeft())) {
                if (!processingMap.containsKey(ean)) { processingMap.put(ean, new ArrayList<>());}
                processingMap.get(ean).add(p);
            } else {
                this.getLogger().info(String.format("Identifier '%s' has been used in NZN req", p.getLeft()));
            }
        });

        
        for (Pair<String, String> proc : this.processors) {
            String name = proc.getKey();
            String url = proc.getValue();
            Parsers parser = Parsers.valueOf(name);
            try {
                if (url.startsWith("http")) {
                    Path p = ImporterUtils.download(url);
                    try (FileInputStream fis = new FileInputStream(p.toFile())) {
                        XMLInputFactory factory = XMLInputFactory.newInstance();
                        XMLStreamReader reader = factory.createXMLStreamReader(fis);
                        parser.process(getLogger(), reader, processingMap);
                    }
                } else {
                    URL urlObj = new URL(url);
                    try (InputStream is = urlObj.openStream()) {
                        XMLInputFactory factory = XMLInputFactory.newInstance();
                        XMLStreamReader reader = factory.createXMLStreamReader(is);
                        parser.process(getLogger(), reader, processingMap);
                    }
                }
            } catch (FactoryConfigurationError | XMLStreamException | IOException e) {
                getLogger().log(Level.SEVERE,e.getMessage(),e);
            }
        }

        List<Pair<String, String>> retval = processingMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        
        return retval;
    }

    private List<String> removeUsedIdentifiersDateCondition(List<String> allUsedIdentifiers, AccountService accountService, Instant inst) throws AccountException, IOException, SolrServerException {
        List<String> pidsToRemove = new ArrayList<>();
        int batchSize = 30;
        int numberOfIteration = allUsedIdentifiers.size() / batchSize;
        numberOfIteration  = numberOfIteration + (allUsedIdentifiers.size() % batchSize == 0 ? 0 : 1);
        for (int i = 0; i < numberOfIteration; i++) {
            int start = i * batchSize;
            int end = Math.min((i + 1) * batchSize, allUsedIdentifiers.size());

            List<String> sublist = allUsedIdentifiers.subList(start, end);

            List<JSONObject> foundRequests = accountService.findAllRequestForGivenIds(null, Arrays.asList("NZN"), null, sublist);
            //  Only scheduler requests
            List<Zadost> allRequests =  foundRequests.stream().map(Object::toString).map(Zadost::fromJSON).collect(Collectors.toList());


            allRequests.forEach(req-> {
                Instant datumZadani = req.getDatumZadani().toInstant();

                if (req.getState().equals("processed")) {
                 // remove all if created date is newer then configuration border date
                    if (ImporterUtils.calculateInterval(datumZadani, inst, getChronoUnit()) <= this.checkPNStates) {
                        getLogger().info(String.format("New NZN request, %s,  %s, skipping", req.getId(),  req.getIdentifiers().toString()));
                        pidsToRemove.addAll(req.getIdentifiers());
                    } else {
                        getLogger().info(String.format("Old NZN request, %s, %s", req.getId(), req.getIdentifiers().toString()));
                    }
                } else {
                    getLogger().info(String.format("Open NZN request, %s,  %s, skipping", req.getId(),  req.getIdentifiers().toString()));
                    pidsToRemove.addAll(req.getIdentifiers());
                }
            });

        }
        return pidsToRemove;
    }

    private ChronoUnit getChronoUnit() {
        ChronoUnit selected = ChronoUnit.DAYS;
        ChronoUnit[] values = ChronoUnit.values();
        for (ChronoUnit chUnit : values) {
            if (chUnit.name().toLowerCase().equals(this.chronoUnit)) {
                selected = chUnit;
            }
        }
        return selected;
    }


    protected Options getOptions() {
        return Options.getInstance();
    }

    @Override
    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }

    
    static enum Parsers {
        
        districz {

            @Override
            public void process(Logger logger,XMLStreamReader reader, Map<String, List<Pair<String, String>>> map)
                    throws XMLStreamException {
                
                String ean = null;
                
                boolean insideItem = false;
                boolean foundEan = false;
                boolean foundAvailability = false;
                int availibiltiy = -1;
                
                while (reader.hasNext()) {
                    int event = reader.next();

                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT:
                            String elementName = reader.getLocalName();

                            if ("ITEM".equals(elementName)) {
                                ean = null;
                                insideItem = true;
                                foundEan = false;
                                foundAvailability = false; 
                                availibiltiy  = -1;
                                
                            }

                            if (insideItem) {
                                if ("EAN".equals(elementName)) {
                                    ean = reader.getElementText();
                                    if (map.containsKey(ean))  {
                                        foundEan = true; 
                                    }
                                }

                                if ("AVAILABILITY".equals(elementName)) {
                                    foundAvailability = true;
                                    availibiltiy = Integer.parseInt(reader.getElementText());
                                }
                            }
                            break;

                        case XMLStreamConstants.END_ELEMENT:
                            if ("ITEM".equals(reader.getLocalName())) {
                                removeEanIfFound(logger,map, ean, foundEan, foundAvailability);
                                insideItem = false; 
                            }
                            break;

                        default:
                            break;
                    }
                }
            }
            
        },
        
        kosmas {

            @Override
            public void process(Logger logger,XMLStreamReader reader, Map<String, List<Pair<String, String>>> map)
                    throws XMLStreamException {
                
                String ean = null;
                
                boolean insideArticle = false;
                boolean foundEan = false;
                boolean foundAvailability = false;

                int availability = -1;

                while (reader.hasNext()) {
                    int event = reader.next();

                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT:
                            String elementName = reader.getLocalName();

                            if ("ARTICLE".equals(elementName)) {
                                ean = null;
                                insideArticle = true;
                                foundEan = false;
                                foundAvailability = false; 
                                availability = -1;
                            }

                            if (insideArticle) {
                                if ("EAN".equals(elementName)) {
                                    ean = reader.getElementText();
                                    if (map.containsKey(ean)) {
                                        foundEan = true; 
                                    }
                                }

                                if ("AVAILABILITY".equals(elementName)) {
                                    foundAvailability = true;
                                    availability = Integer.parseInt(reader.getElementText());
                                }

                            }
                            break;

                        case XMLStreamConstants.END_ELEMENT:
                            if ("ARTICLE".equals(reader.getLocalName())) {
                                removeEanIfFound(logger,map, ean, foundEan, foundAvailability);
                                insideArticle = false; 
                            }
                            break;

                        default:
                            break;
                    }
                }
                
            }
            
        },
        
        
        heureka {
            @Override
            public void process(Logger logger,XMLStreamReader reader, Map<String, List<Pair<String, String>>> map)
                    throws XMLStreamException {

                String ean = null;

                boolean insideShopItem = false;
                boolean foundEan = false;
                


                while (reader.hasNext()) {
                    int event = reader.next();

                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT:
                            String elementName = reader.getLocalName();

                            if ("SHOPITEM".equals(elementName)) {
                                insideShopItem = true;
                                foundEan = false;
                            }

                            if (insideShopItem) {
                                if ("EAN".equals(elementName)) {
                                    ean = reader.getElementText();
                                    if (map.containsKey(ean)) {
                                        foundEan = true;
                                    }
                                }
                            }
                            break;

                        case XMLStreamConstants.END_ELEMENT:
                            if ("SHOPITEM".equals(reader.getLocalName())) {
                                removeEanIfFound(logger, map, ean, foundEan, true);
                                insideShopItem = false; 
                            }
                            break;

                        default:
                            break;
                    }
                }
            }
        };
        private static void removeEanIfFound(Logger logger, Map<String, List<Pair<String, String>>> map, String ean, boolean foundEan,
                boolean foundAvailability) {
            if (foundEan && foundAvailability) {
                if (map.containsKey(ean)) {
                    List<Pair<String, String>> list = map.get(ean);
                    logger.info(String.format("Removing %s", list.toString()));
                    map.remove(ean);
                   
                }
            }
        }

        public abstract void process(Logger logger, XMLStreamReader reader, Map<String,List<Pair<String,String>>> map) throws XMLStreamException;
    }
    
    
    
}
