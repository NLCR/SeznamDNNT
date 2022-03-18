package cz.inovatika.sdnnt.services;

/**
 * Storing history of record
 * @author happy
 */
public interface History {
    
    public void log(String identifier, String oldRaw, String newRaw, String user, String type, String workflowId);

    public void log(String identifier, String oldRaw, String newRaw, String user, String type, String workflowId,boolean commit);
}

