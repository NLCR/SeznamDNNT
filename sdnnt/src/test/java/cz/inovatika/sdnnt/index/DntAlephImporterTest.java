package cz.inovatika.sdnnt.index;

import cz.inovatika.sdnnt.index.utils.torefactor.MarcRecordUtilsToRefactor;
import cz.inovatika.sdnnt.indexer.models.DataField;
import cz.inovatika.sdnnt.utils.MarcRecordFields;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DntAlephImporterTest {

    @Test
    public void testReadDocument1() throws XMLStreamException {
        InputStream resourceAsStream = DntAlephImporterTest.class.getResourceAsStream("oai.1.xml");
        Assert.assertNotNull(resourceAsStream);
        XMLStreamReader reader = null;
        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            reader = inputFactory.createXMLStreamReader(resourceAsStream);
            Assert.assertNotNull(reader);

            DntAlephImporter importer = new DntAlephImporter();
            importer.readDocument(reader);

            Assert.assertTrue(importer.recs.size() == 41);
            List<String> ids = importer.recs.stream().map(it -> it.identifier).collect(Collectors.toList());

            DntAlephTestUtils.document2_expectedIDs().stream().forEach(id-> {
                Assert.assertTrue(ids.contains(id));
            });


            List<String> controlFields001 = importer.recs.stream().map(it -> it.controlFields.get("001")).collect(Collectors.toList());
            DntAlephTestUtils.document1_ExpectedControlFields001().stream().forEach(id-> {
                boolean contains = controlFields001.contains(id);
                Assert.assertTrue(contains);
            });

            List<String> controlFields003 = importer.recs.stream().map(it -> it.controlFields.get("003")).collect(Collectors.toList());
            controlFields003.stream().forEach(id-> {
                Assert.assertTrue(id.equals("CZ PrDNT"));
            });

            List<String> brokenItems = Arrays.asList("oai:aleph-nkp.cz:DNT01-000161565","oai:aleph-nkp.cz:DNT01-000161566", "oai:aleph-nkp.cz:DNT01-000161569", "oai:aleph-nkp.cz:DNT01-000161570");
            importer.recs.stream().forEach(it-> {
                if (!brokenItems.contains(it.identifier)) {
                    List<DataField> dataFields = (List<DataField>) it.dataFields.get("990");
                    Assert.assertNotNull(dataFields);
                }
            });

            List<SolrInputDocument> collected = importer.recs.stream().map(r -> {
                SolrInputDocument sDoc = new SolrInputDocument();
                MarcRecordUtilsToRefactor.addStavFromMarc(sDoc, r.dataFields);
                return sDoc;
            }).collect(Collectors.toList());



        } catch (IOException e) {
            Assert.fail(e.getMessage());
        } finally {
            if (reader != null) reader.close();
        }
    }

    @Test
    public void testReadDocument2() throws XMLStreamException {
        InputStream resourceAsStream = DntAlephImporterTest.class.getResourceAsStream("oai.2.xml");
        Assert.assertNotNull(resourceAsStream);
        XMLStreamReader reader = null;
        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            reader = inputFactory.createXMLStreamReader(resourceAsStream);
            Assert.assertNotNull(reader);

            DntAlephImporter importer = new DntAlephImporter();
            importer.readDocument(reader);

            Assert.assertTrue(importer.recs.size() == 36);

            List<SolrInputDocument> collected = importer.recs.stream().map(r -> {
                SolrInputDocument sDoc = new SolrInputDocument();
                MarcRecordUtilsToRefactor.addStavFromMarc(sDoc, r.dataFields);
                return sDoc;
            }).collect(Collectors.toList());

            collected.stream().forEach(sdoc-> {
                Assert.assertTrue(sdoc.containsKey(MarcRecordFields.DNTSTAV_FIELD));
                Assert.assertNotNull(sdoc.getFieldValue(MarcRecordFields.DNTSTAV_FIELD));
                Assert.assertTrue(sdoc.containsKey(MarcRecordFields.KURATORSTAV_FIELD));
                Assert.assertNotNull(sdoc.getFieldValue(MarcRecordFields.KURATORSTAV_FIELD));
            });

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        } finally {
            if (reader != null) reader.close();
        }
    }

    @Test
    public void testReadDocument3() throws XMLStreamException {
        InputStream resourceAsStream = DntAlephImporterTest.class.getResourceAsStream("oai.3.xml");
        Assert.assertNotNull(resourceAsStream);
        XMLStreamReader reader = null;
    }
    
    @Test
    public void testReadDocument4() throws XMLStreamException {
        InputStream resourceAsStream = DntAlephImporterTest.class.getResourceAsStream("oai.4_996.xml");
        Assert.assertNotNull(resourceAsStream);
        
        Assert.assertNotNull(resourceAsStream);
        XMLStreamReader reader = null;
        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            reader = inputFactory.createXMLStreamReader(resourceAsStream);
            Assert.assertNotNull(reader);

            DntAlephImporter importer = new DntAlephImporter();
            importer.readDocument(reader);

            System.out.println(importer.recs.size());
            Assert.assertTrue(importer.recs.size() == 37);

            List<SolrInputDocument> collected = importer.recs.stream().map(r -> {
               return r.toSolrDoc();
            }).collect(Collectors.toList());

            collected.stream().forEach(sdoc-> {
                //sdoc.get
                Assert.assertTrue(sdoc.containsKey(MarcRecordFields.MARC_996_A));
            });

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        } finally {
            if (reader != null) reader.close();
        }

    }
    
}
