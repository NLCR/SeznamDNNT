package cz.inovatika.sdnnt.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.time.DurationFormatUtils;

public class QuartzUtils {
    
    private QuartzUtils() {}
    
    public static void printDuration(Logger logger, long start) {
        long diff = System.currentTimeMillis() - start;
        
        logger.log(Level.INFO, "FINISHED. TotalTime: {0}.", new Object[]{DurationFormatUtils.formatDurationHMS(diff)});
    }
}
