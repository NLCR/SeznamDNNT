package cz.inovatika.sdnnt.utils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import cz.inovatika.sdnnt.services.impl.hackcerts.HttpsTrustManager;

public class OAIIterationTest {

	public static final Logger LOGGER = Logger.getLogger(OAIIterationTest.class.getName());
	
	//resumptionToken
	//sdnnt/oai?verb=ListRecords&metadataFormat=marc21&set=DNNTO
	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
		
		List<String> allIdentifiers = new ArrayList<>();
		
        HttpsTrustManager.allowAllSSL();
        long start = System.currentTimeMillis();
        int totalNumberOfDocs = 0;
        String resumptionToken = null;
        String nResumptionToken = null;
        
        //int rows = 3000;
        
       
        int numberOfDocs = 0;
        do {

        	String url =  devBaseUrl();
        	if (resumptionToken != null) {
        		url = url +"&resumptionToken="+resumptionToken;
        	} else {
        		url = url + DNNTO();
        	}
        	LOGGER.info(String.format("Requesting url %s",url));
        	String string = SimpleGET.get(url);
        	Document parseDocument = XMLUtils.parseDocument(new StringReader(string));
        	Element findElement = XMLUtils.findElement(parseDocument.getDocumentElement(), "resumptionToken");
        	if (findElement != null) {
            	resumptionToken = findElement.getTextContent();
        	} else {
        		resumptionToken = null;
        	}


        	List<Element> elements = XMLUtils.getElementsRecursive(parseDocument.getDocumentElement(), (elm)->{
        		return (elm.getNodeName().equals("record"));
        	});

        	List<String> collect = elements.stream().map(elm->{
        		Element identifier = XMLUtils.findElement(elm, "identifier");
        		return identifier.getTextContent();
        	}).collect(Collectors.toList());

        	
        	numberOfDocs = collect.size();
        	allIdentifiers.addAll(collect);
        	LOGGER.info(String.format("Identifiers : %d %s", collect.size(),collect));
        	LOGGER.info(String.format("Number of identifiers : %d", allIdentifiers.size()));
        	
        	
        }while(resumptionToken != null);

        LOGGER.info(" Number of records :"+allIdentifiers.size());
        LOGGER.info(" Number of records of set :"+new HashSet(allIdentifiers).size());
        LOGGER.info("It took :"+(System.currentTimeMillis() - start));
	}

   private static String N() {
           return "&metadataFormat=marc21&set=SDNNT-N";
   }

	private static String PA() {
	        return "&metadataFormat=marc21&set=SDNNT-PA";
    }

	private static String DNNTO() {
		return "&metadataFormat=marc21&set=DNNTO";
	}
	private static String DNNTT() {
		return "&metadataFormat=marc21&set=DNNTT";
	}

	private static String devBaseUrl() {
		String devBaseUrl = "https://sdnnt-dev.nkp.cz/sdnnt/oai?verb=ListIdentifiers";
		return devBaseUrl;
	}

	private static String testBaseUrl() {
		String devBaseUrl = "https://sdnnt-test.nkp.cz/sdnnt/oai?verb=ListRecords";
		return devBaseUrl;
	}
}
