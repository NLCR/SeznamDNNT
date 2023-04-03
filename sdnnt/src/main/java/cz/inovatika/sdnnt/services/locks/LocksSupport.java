package cz.inovatika.sdnnt.services.locks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LocksSupport {

    // services lock
    public static final Lock SERVICES_LOCK = new ReentrantLock();
    
}
