package cz.inovatika.sdnnt.jobs;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

import cz.inovatika.sdnnt.utils.GeneratePSWDUtility;

public class Testik {
    
    public static class ObjectSizeFetcher {
        private static Instrumentation instrumentation;

        public static void premain(String args, Instrumentation inst) {
            instrumentation = inst;
        }

        public static long getObjectSize(Object o) {
            return instrumentation.getObjectSize(o);
        }
    }
    
    public static void main(String[] args) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 500000; i++) {
            ids.add(GeneratePSWDUtility.generatePwd());
            if (i % 10000 == 0) {
                System.out.println("Iteration: "+i);
            }
        }
        System.out.println(ObjectSizeFetcher.getObjectSize(ids));
        
    }
}
