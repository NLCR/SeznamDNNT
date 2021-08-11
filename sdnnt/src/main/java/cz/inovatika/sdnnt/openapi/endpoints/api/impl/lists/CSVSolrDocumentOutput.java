package cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists;

import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CSVSolrDocumentOutput implements  SolrDocumentOutput{

    public static final Logger LOGGER = Logger.getLogger(CSVSolrDocumentOutput.class.getName());

    private CSVPrinter printer;

    public CSVSolrDocumentOutput(CSVPrinter csvPrinter) {
        this.printer = csvPrinter;
    }

    @Override
    public void output(String selectedInstitution, String label, Collection<Object> nazev, String identifier, String... pids) {
        for (int i = 0; i < pids.length; i++) {
            try {
                printer.printRecord(pids[i], label, selectedInstitution ,nazev, identifier);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE,e.getMessage(),e);
            }
        }
    }
}
