package cz.inovatika.sdnnt.index.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import cz.inovatika.sdnnt.indexer.models.DataField;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.indexer.models.SubField;

public class OAIXMLRecordsReader {
    
    public static final Logger LOGGER = Logger.getLogger(OAIXMLRecordsReader.class.getName());
    
    private InputStream input;
    private List<MarcRecord> records = new ArrayList();
    private List<String> toDelete = new ArrayList();
    
    public OAIXMLRecordsReader(InputStream input) {
        super();
        this.input = input;
    }
    
    public List<MarcRecord> getRecords() {
        return records;
    }
    
    
    public List<String> getToDelete() {
        return toDelete;
    }
    
    public String readFromXML() throws XMLStreamException {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLStreamReader reader = null;
        try {
            reader = inputFactory.createXMLStreamReader(this.input);
            return readDocument(reader);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return null;
    }

    /**
     * Reads OAI XML document
     *
     * @param reader
     * @return resuptionToken or null
     * @throws XMLStreamException
     * @throws IOException
     */
    private String readDocument(XMLStreamReader reader) throws XMLStreamException, IOException {
        String resumptionToken = null;
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    String elementName = reader.getLocalName();
                    if (elementName.equals("record")) {
                        readMarcRecords(reader);
                    } else if (elementName.equals("resumptionToken")) {
                        resumptionToken = reader.getElementText();
                    } else if (elementName.equals("error")) {
                        throw new IOException(reader.getElementText());
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    break;
            }
        }
        return resumptionToken;
        //throw new XMLStreamException("Premature end of file");
    }

    private void readMarcRecords(XMLStreamReader reader) throws XMLStreamException, IOException {
        MarcRecord mr = new MarcRecord();
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    String elementName = reader.getLocalName();
                    if (elementName.equals("header")) {
                        String status = reader.getAttributeValue(null, "status");
                        if (!"deleted".equals(status)) {
                            readRecordHeader(reader, mr);
                        } else {
                            mr.isDeleted = true;
                        }
                    } else if (elementName.equals("metadata")) {
                        readRecordMetadata(reader, mr);
                        if (!mr.isDeleted) {
                            records.add(mr);
                        } else {
                            LOGGER.log(Level.INFO, "Record {0} is deleted", mr.identifier);
                            toDelete.add(mr.identifier);
                        }
                    } else {
                        skipElement(reader, elementName);
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    return;
            }
        }
        throw new XMLStreamException("Premature end of ListRecords");
    }

    private void readRecordHeader(XMLStreamReader reader, MarcRecord mr) throws XMLStreamException {

        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    String elementName = reader.getLocalName();
                    if (elementName.equals("identifier")) {
                        mr.identifier = reader.getElementText();
                    } else if (elementName.equals("datestamp")) {
                        mr.datestamp = reader.getElementText();
                    } else if (elementName.equals("setSpec")) {
                        mr.setSpec = reader.getElementText();
                    }
                case XMLStreamReader.END_ELEMENT:
                    elementName = reader.getLocalName();
                    if (elementName.equals("header")) {
                        return;
                    }
            }
        }

        throw new XMLStreamException("Premature end of header");
    }

    private void readRecordMetadata(XMLStreamReader reader, MarcRecord mr) throws XMLStreamException {

        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    String elementName = reader.getLocalName();
                    if (elementName.equals("record")) {
                        readMarcRecord(reader, mr);
                    }
                case XMLStreamReader.END_ELEMENT:
                    elementName = reader.getLocalName();
                    if (elementName.equals("metadata")) {
                        return;
                    }
            }
        }

        throw new XMLStreamException("Premature end of metadata");
    }

    private MarcRecord readMarcRecord(XMLStreamReader reader, MarcRecord mr) throws XMLStreamException {
      int index = 0;
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    String elementName = reader.getLocalName();
                    if (elementName.equals("leader")) {
                        mr.leader = reader.getElementText();

                    } else if (elementName.equals("controlfield")) {
                        // <marc:controlfield tag="003">CZ PrDNT</marc:controlfield>
                        String tag = reader.getAttributeValue(null, "tag");
                        String v = reader.getElementText();
                        mr.controlFields.put(tag, v);
                    } else if (elementName.equals("datafield")) {
                        readDatafields(reader, mr, index);
                    }
                case XMLStreamReader.END_ELEMENT:
                    elementName = reader.getLocalName();
                    if (elementName.equals("record")) {
                        return mr;
                    }
            }
        }
        throw new XMLStreamException("Premature end of marc:record");
    }

    private MarcRecord readDatafields(XMLStreamReader reader, MarcRecord mr, int index) throws XMLStreamException {
        String tag = reader.getAttributeValue(null, "tag");
        if (!mr.dataFields.containsKey(tag)) {
            mr.dataFields.put(tag, new ArrayList());
        }
        List<DataField> dfs = mr.dataFields.get(tag);
        int subFieldIndex = 0;

        DataField df = new DataField(tag, reader.getAttributeValue(null, "ind1"), reader.getAttributeValue(null, "ind2"), index);
        dfs.add(df);
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    String elementName = reader.getLocalName();
                    if (elementName.equals("subfield")) {
                        // readSubFields(reader, df);

                        String code = reader.getAttributeValue(null, "code");
                        if (!df.subFields.containsKey(code)) {
                            df.getSubFields().put(code, new ArrayList());
                        }
                        List<SubField> sfs = df.getSubFields().get(code);
                        String val = reader.getElementText();
                        sfs.add(new SubField(code, val, subFieldIndex++));
                        //mr.sdoc.addField("" + tag + code, val);
                    }
                case XMLStreamReader.END_ELEMENT:
                    elementName = reader.getLocalName();
                    if (elementName.equals("datafield")) {
                        return mr;
                    }
            }
        }

        throw new XMLStreamException("Premature end of datafield");
    }

    private void skipElement(XMLStreamReader reader, String name) throws XMLStreamException {

        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.END_ELEMENT:
                    String elementName = reader.getLocalName();
                    if (elementName.equals(name)) {
                        //LOGGER.log(Level.INFO, "eventType: {0}, elementName: {1}", new Object[]{eventType, elementName});
                        return;
                    }
            }
        }
//    throw new XMLStreamException("Premature end of file");
    }

}
