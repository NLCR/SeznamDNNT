package cz.inovatika.sdnnt.utils;

import java.io.IOException;

public class TestHEaders {
    
    public static void main(String[] args) throws IOException {
        SimpleGET.get("http://localhost:18080/sdnnt/assets/config.json");
        
    }  
}
