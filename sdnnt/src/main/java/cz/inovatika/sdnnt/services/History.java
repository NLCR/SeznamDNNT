package cz.inovatika.sdnnt.services;

public interface History {

    public void log(String identifier, String oldRaw, String newRaw, String user, String type, String workflowId);

}
