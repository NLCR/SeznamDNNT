package cz.inovatika.sdnnt.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public class Time {
    
    public static void main(String[] args) {
        ZoneId zone1 = ZoneId.of("Europe/Bratislava");
        ZoneId zone2 = ZoneId.of("Europe/Moscow");

        System.out.println(zone1);
        
        
        //test();
    }

    private static void test() {
        LocalTime locTime = LocalTime.now();
        System.out.println(locTime);
        
        LocalTime locTime1 = LocalDateTime.now().toLocalTime();
        System.out.println(locTime1);
        System.out.println(LocalDateTime.now());
        
        
        
    }
}
