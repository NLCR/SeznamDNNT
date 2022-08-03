package cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists;

import org.apache.commons.csv.CSVPrinter;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.openapi.endpoints.api.impl.utils.PIDSupport;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CSVSolrDocumentOutput implements  SolrDocumentOutput{

    public static final Logger LOGGER = Logger.getLogger(CSVSolrDocumentOutput.class.getName());

    public AtomicInteger counter = new AtomicInteger();
    
    private CSVPrinter printer;

    public CSVSolrDocumentOutput(CSVPrinter csvPrinter) {
        this.printer = csvPrinter;
    }

    @Override
    public void output(Map<String, Object> outputDocument, List<String> fields, String endpointLicense, boolean doNotEmitParent) {

        Set<String> emmitedGranularityPIDS = new HashSet<>();
        Set<String> allGranularityPIDS = new HashSet<>();
        
        Object object = outputDocument.get(FMT_KEY);
        
        Collection<String> pids = (Collection<String>) outputDocument.get(PIDS_KEY);
        String masterPid = !pids.isEmpty() ? pids.iterator().next() : "";


        List<String> granularity = (List<String>) outputDocument.get(GRANUARITY_KEY);
        granularity.stream().map(it-> new JSONObject(it)).forEach(jsonObject -> {
            // pokud je vyrazeno nebo x - nepropagovat 
            JSONArray stav = jsonObject.optJSONArray("stav");
            String license = jsonObject.optString("license");
            String cislo = jsonObject.optString("cislo");
            String link = jsonObject.optString("link");
            String rocnik = jsonObject.optString("rocnik");
            String fetched = jsonObject.optString("fetched");

            if (jsonObject.has("cislo") || jsonObject.has("rocnik") || jsonObject.has("fetched")) {
                if (link != null)  {
                    String pid = PIDSupport.pidNormalization(PIDSupport.pidFromLink(link));
                    if (!pid.equals(masterPid)) {
                        // nesmi byt stav x - kdyby nahodou byla prirazena licence 
                        if (license != null && license.equals(endpointLicense)) {
                            try {
                                List<String> record = csvRecord(outputDocument, fields, pid);
                                printer.printRecord(record);
                                emmitedGranularityPIDS.add(pid);
                                //LOGGER.info(String.format(" CSV %s counter %d",record.toString() ,counter.incrementAndGet()));
                            } catch (IOException e) {
                                LOGGER.log(Level.SEVERE,e.getMessage(),e);
                            }
                        }
                        allGranularityPIDS.add(pid);
                    }
                }
            }
        });
        
        // podminka pro vynechani parentu 
        boolean skip = doNotEmitParent && allGranularityPIDS.size() > 0;
        if (object != null && (!skip)) {
            if (pids != null) {
                pids.stream().forEach(p-> {
                    try {
                        List<String> record = csvRecord(outputDocument, fields, p);
                        printer.printRecord(record);
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE,e.getMessage(),e);
                    }
                });
            }
        }
    }

    private List<String> csvRecord(Map<String, Object> outputDocument, List<String> fields, String p) {
        List<String> record = new ArrayList<>();
        Map<String, Object> nOutput = new HashMap<>(outputDocument);
        nOutput.put(PID_KEY, p);
        List<String> flist = fields.stream().filter(f-> {
            // vyfiltruje pids a pid
            return !Arrays.asList(PIDS_KEY).contains(f);
        }).collect(Collectors.toList());

        flist.forEach(f-> {
            String o = nOutput.get(f) != null ? nOutput.get(f).toString() : "";
            record.add(o);
        });
        return record;
    }


}
