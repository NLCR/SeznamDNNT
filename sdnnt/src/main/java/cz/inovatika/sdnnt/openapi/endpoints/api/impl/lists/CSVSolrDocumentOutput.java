package cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists;

import org.apache.commons.csv.CSVPrinter;

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
    public void output(Map<String, Object> outputDocument, List<String> fields) {
        Collection<String> pids = (Collection<String>) outputDocument.get(PIDS_KEY);
        if (pids != null) {
            pids.stream().forEach(p-> {
                try {
                    Map<String, Object> nOutput = new HashMap<>(outputDocument);
                    nOutput.put(PID_KEY, p);

                    List<String> record = new ArrayList<>();

                    List<String> flist = fields.stream().filter(f-> {
                        // vyfiltruje pids a pid
                        return !Arrays.asList(PIDS_KEY).contains(f);
                    }).collect(Collectors.toList());

                    flist.forEach(f-> {
                        String o = nOutput.get(f) != null ? nOutput.get(f).toString() : "";
                        record.add(o);
                    });

                    printer.printRecord(record);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE,e.getMessage(),e);
                }
            });
        }
    }


}
