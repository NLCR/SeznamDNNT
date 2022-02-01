package cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists;

import org.apache.commons.csv.CSVPrinter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CSVSolrDocumentOutput implements  SolrDocumentOutput{

    public static final Logger LOGGER = Logger.getLogger(CSVSolrDocumentOutput.class.getName());

    private CSVPrinter printer;

    public CSVSolrDocumentOutput(CSVPrinter csvPrinter) {
        this.printer = csvPrinter;
    }

    @Override
    public void output(Map<String, Object> outputDocument, List<String> fields, String endpointLicense) {

        Collection<String> pids = (Collection<String>) outputDocument.get(PIDS_KEY);
        String masterPid = !pids.isEmpty() ? pids.iterator().next() : "";
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

        // pokud master pid ma pak jo, pokud ne, pak vynechat
        List<String> granularity = (List<String>) outputDocument.get(GRANUARITY_KEY);
        granularity.stream().map(it-> new JSONObject(it)).forEach(jsonObject -> {

            JSONArray stav = jsonObject.optJSONArray("stav");
            String license = jsonObject.optString("license");
            String cislo = jsonObject.optString("cislo");
            String link = jsonObject.optString("link");
            String rocnik = jsonObject.optString("rocnik");
            // TODO:
            if (link != null)  {
                if (link.contains("uuid:")){
                    String pid = link.substring(link.indexOf("uuid:"));
                    try {
                        if (!pid.equals(masterPid)) {
                            List<String> record = csvRecord(outputDocument, fields, pid);
                            printer.printRecord(record);
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE,e.getMessage(),e);
                    }
                }
            }
        });

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
