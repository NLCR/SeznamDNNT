package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

/**
 * Represents a service for handling requests
 * 
 * TODO: return types
 */
public interface AccountService {
    
    /** Maximum number of items in one requests */
    public static final int MAXIMUM_ITEMS_IN_ZADOST = 3000;

    /**
     * Finds request by given id
     * @param id Requet's identifier
     * @return
     * @throws SolrServerException
     * @throws IOException
     */
    public JSONObject getRequest(String id) throws SolrServerException, IOException;

    /**
     * Searching  in requests
     * @param q Query
     * @param status  Filter for request status: open, waiting, waiting_for_automatic_process, processed
     * @param navrhy Filter for type of request: NZN, VN, VNL, VNZ
     * @param institution Filter for institution
     * @param priority Filter for priority
     * @param delegated Filtr for delegated person
     * @param typeOfReq
     * @param sort Sorting support (u uzivatele datum vytvoreni a zpracovani, u kuratoru - deadline, priority atd.. )
     * @param rows Number items in one page
     * @param page Page number
     * @return Search results
     * @throws SolrServerException Chyba spojeni se solrem
     * @throws IOException IO chyba
     * @throws AccountException Genericka vyjimka pri ukladani zadosti
     */
    public JSONObject search(String q, String state, List<String> navrhy, String institution, String priority, String delegated, String typeOfReq, String sort, int rows, int page) throws SolrServerException, IOException,AccountException;


    /**
     * Prepare empty request
     * @param navrh Type of request
     * @return Return serialized request
     * @throws SolrServerException
     * @throws IOException
     * @throws AccountException
     * @throws ConflictException
     */
    public JSONObject prepare(String navrh) throws SolrServerException, IOException, AccountException, ConflictException;


    /**
     * Saving requests from the end of the curator
     * @param payload Request serialized in json 
     * @param inform Information callback
     * @return Returns stored json
     * @throws SolrServerException Solr exception
     * @throws IOException IO error
     * @throws ConflictException Saving conflict
     * @throws AccountException Generic account exception
     * @see AccountServiceInform
     */
    public JSONObject saveRequest(String payload, AccountServiceInform inform) throws SolrServerException, IOException, ConflictException, AccountException;



    /**
     * Saving requests from the end of the user
     * @param payload Requests in json
     * @param inform 
     * @return Stored requests
     * @throws SolrServerException Solr exception
     * @throws IOException IO error
     * @throws ConflictException Konflikt, zadost byla zmenene
     * @throws AccountException Genericka vyjimka pri ukladani zadosti
     * @see AccountServiceInform
     */
    public JSONObject saveCuratorRequest(String payload, AccountServiceInform inform) throws SolrServerException, IOException, ConflictException,AccountException;


    public JSONObject saveRequestWithFRBR(String payload, User user , String frbr, AccountServiceInform inform) throws SolrServerException, IOException, ConflictException,AccountException;

    /**
     * Returns items in request
     * @param id Request identifiers
     * @param rows
     * @param page
     * @return
     * @throws SolrServerException
     * @throws IOException
     * @throws ConflictException
     * @throws AccountException
     */
    public JSONObject getRecords(String id, int rows, int page) throws SolrServerException, IOException, ConflictException,AccountException;

    /**
     * Closing requests - from user's end
     * @param payload Serialized request
     * @return
     * @throws ConflictException
     * @throws AccountException
     */
    public JSONObject userCloseRequest(String payload) throws ConflictException,AccountException;

    /**
     * Closing requests - from scheduler's end
     * @param payload Serialized request
     * @return
     * @throws ConflictException
     * @throws AccountException
     */
    public JSONObject schedulerDefinedCloseRequest(String payload) throws ConflictException,AccountException;

    /**
     * Closing requests - from curactor's end
     * @param payload Serialized request
     * @return
     * @throws ConflictException
     * @throws AccountException
     */
    public JSONObject curatorCloseRequest(String payload) throws ConflictException,AccountException;

    /**
     * Delete request 
     * @param payload  Serialized request
     * @return
     * @throws ConflictException
     * @throws AccountException
     * @throws IOException
     * @throws SolrServerException
     */
    public JSONObject deleteRequest(String payload) throws ConflictException, AccountException, IOException, SolrServerException;
    
    /**
     * Switch state - curator's end
     * @param zadostJson Serialized request
     * @param documentId Document id
     * @param reason Reason for switch
     * @return Returns result of switch
     * @throws ConflictException 
     * @throws AccountException
     * @throws IOException
     * @throws SolrServerException
     */
    public JSONObject curatorSwitchState(JSONObject zadostJson, String documentId, String reason) throws ConflictException, AccountException, IOException, SolrServerException;

    /**
     * Switch state - alternative state
     * @param alternative Alternative state
     * @param zadostJson Serialized request
     * @param documentId Document id 
     * @param reason Reason
     * @return
     * @throws ConflictException
     * @throws AccountException
     * @throws IOException
     * @throws SolrServerException
     */
    public JSONObject curatorSwitchAlternativeState(String alternative, JSONObject zadostJson, String documentId, String reason) throws ConflictException, AccountException, IOException, SolrServerException;

    /**
     * Curactor reject state 
     * @param zadostJson Serialized request
     * @param documentId Document id
     * @param reason Reason for rejection
     * @return
     * @throws ConflictException
     * @throws AccountException
     * @throws IOException
     * @throws SolrServerException
     */
    public JSONObject curatorRejectSwitchState(JSONObject zadostJson, String documentId, String reason) throws ConflictException, AccountException, IOException, SolrServerException;

    /**
     * Scheduler switch states 
     * @throws ConflictException
     * @throws AccountException
     * @throws IOException
     * @throws SolrServerException
     */
    public void schedulerSwitchStates() throws ConflictException, AccountException, IOException, SolrServerException;

    public void schedulerSwitchStates(String id) throws ConflictException, AccountException, IOException, SolrServerException;
    
    /**
     * Explicit commit 
     * @param indicies Indicies
     * @throws ConflictExceptio n
     * @throws AccountException
     * @throws IOException
     * @throws SolrServerException
     */
    public void commit(String ... indicies) throws ConflictException, AccountException, IOException, SolrServerException;

    
    /**
     * Finds all identifiers used in requests associated with a given user
     * @param user User 
     * @return 
     * @throws AccountException
     * @throws IOException
     * @throws SolrServerException
     */
    public List<String> findIdentifiersUsedInRequests(String user) throws  AccountException, IOException, SolrServerException; 
    
    /**
     * Finds all identifiers used in requests associated with a given user and request status
     * @param user User
     * @param requestState Request status
     * @return
     * @throws AccountException
     * @throws IOException
     * @throws SolrServerException
     */
    public List<String> findIdentifiersUsedInRequests(String user, String requestState) throws  AccountException, IOException, SolrServerException; 

    
    public List<String> findIdentifiersUsedInRequests(String user, List<String>requestsStates) throws  AccountException, IOException, SolrServerException; 
    
    /**
     * 
     * @param user
     * @param navrh
     * @param requestState
     * @return
     * @throws AccountException
     * @throws IOException
     * @throws SolrServerException
     */
    public List<String> findIdentifiersUsedInRequests(String user, String navrh, String requestState) throws  AccountException, IOException, SolrServerException; 
}
