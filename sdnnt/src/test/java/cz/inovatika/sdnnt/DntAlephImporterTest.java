package cz.inovatika.sdnnt;

import java.io.InputStream;

import org.junit.Test;

import cz.inovatika.sdnnt.index.DntAlephImporter;

public class DntAlephImporterTest {
    
    
    @Test
    public void testImport() {
        InputStream is = DntAlephImporterTest.class.getResourceAsStream("ojHOeldgcRSoVurX9GZweqeq");
        DntAlephImporter importer = new DntAlephImporter();
        //importer.
    }
}
