package cz.inovatika.sdnnt.index.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.utils.XMLUtils;

public class OAIXMLHeadersReader {
    
    private InputStream input;
    private List<String> records = new ArrayList();
    private List<String> toDelete = new ArrayList();
    
    
    
    public OAIXMLHeadersReader(InputStream input) {
        super();
        this.input = input;
    }

    
    public List<String> getRecords() {
        return records;
    }

    
    public List<String> getToDelete() {
        return toDelete;
    }
    
    public String readFromXML() throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document parsed = parser.parse(this.input);

        List<Element> headers = XMLUtils.getElementsRecursive(parsed.getDocumentElement(), (elm)-> {
            return elm.getNodeName().equals("header");
        }).stream().collect(Collectors.toList());
        
        
        List<Element> deletedHeaders = headers.stream().filter(elm-> {
            boolean status = elm.hasAttribute("status");
            if (status && "deleted".equals(elm.getAttribute("status"))) {
                return true;
            } else return false;
        }).collect(Collectors.toList());
        
        List<Element> activeHeaders = headers.stream().filter(elm-> {
            return !elm.hasAttribute("status");
        }).collect(Collectors.toList());
        

        List<String> deleted = deletedHeaders.stream().map(elm-> {
            Element ident = XMLUtils.findElement(elm, "identifier");
            return ident != null ? ident.getTextContent() : null;
        }).collect(Collectors.toList());

        List<String> active = activeHeaders.stream().map(elm-> {
            Element ident = XMLUtils.findElement(elm, "identifier");
            return ident != null ? ident.getTextContent() : null;
        }).collect(Collectors.toList());

        this.toDelete.addAll(deleted);
        this.records.addAll(active);
        
        Element token = XMLUtils.findElement(parsed.getDocumentElement(), "resumptionToken");
        return token != null ? token.getTextContent() : null;
    }
}
