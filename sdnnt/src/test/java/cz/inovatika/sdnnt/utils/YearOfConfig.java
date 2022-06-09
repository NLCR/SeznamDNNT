package cz.inovatika.sdnnt.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import cz.inovatika.sdnnt.services.impl.PXKrameriusServiceImpl;

public class YearOfConfig {
    public static void main(String[] args) throws IOException {
        List<String> identifiers = new ArrayList<>(500000);
        
        File folder = new File("C:\\Users\\happy\\Projects\\SeznamDNNT\\tt");
        File[] listFiles = folder.listFiles();
        for (File file : listFiles) {
            System.out.println("Processing file "+file.getAbsolutePath());
            List<String> readLines = IOUtils.readLines(new FileInputStream(file),Charset.forName("UTF-8"));
            System.out.println("Read file, number of lines "+readLines.size());
            List<String> filtered = readLines.stream().filter(line-> {
                return line.matches(".*Testing url.*");
            }).map(line-> {
                
                int indexOf = line.indexOf("list of identifiers [");
                if (indexOf > 0) {
                    return line.substring(indexOf+"list of identifiers [".length(), line.length()-1);
                } else return "";
            }).collect(Collectors.toList());

            System.out.println("Filtered file, number of lines "+filtered.size());
         
            filtered.stream().forEach(line-> {
                String[] split = line.split(",");
                for (int i = 0; i < split.length; i++) {
                    String oneIdent = split[i].trim();
                    if (!identifiers.contains(oneIdent)) {
                        if (identifiers.size() % 10000 == 0) {
                           System.out.println("\t identifiers.size() == "+identifiers.size());
                        }
                        identifiers.add(oneIdent);
                    } else throw new IllegalStateException("Identifier already added to array "+oneIdent);
                }
            });
            System.out.println("Number of identifiers "+identifiers.size());
        }
        System.out.println("Identifiers: "+identifiers.size());
    }

}
