package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

/**
 * Reprezentuje sluzbu pro operace nad zadostmi
 */
public interface AccountService {

    public static final int MAXIMUM_ITEMS_IN_ZADOST = 300;

    /**
     * Najde zadost dle id
     * @param id Identifikator zadosti
     * @return
     * @throws SolrServerException
     * @throws IOException
     */
    public JSONObject getRequest(String id) throws SolrServerException, IOException;

    /**
     * Hledani v zadostech
     * @param q Dotaz
     * @param state  Filtr pro stav zadosti open, waiting, waiting_for_automatic_process, processed
     * @param navrhy Filtr pro  zadosti NZN, VN, VNL, VNZ
     * @param institution Filtr pro instituci
     * @param priority Filtr pro prioritu
     * @param delegated Filtr pro delegovanou osobu
     * @param sort Sortovani dle (u uzivatele datum vytvoreni a zpracovani, u kuratoru - deadline, priority atd.. )
     * @param rows
     * @param page
     * @return
     * @throws SolrServerException Chyba spojeni se solrem
     * @throws IOException IO chyba
     * @throws AccountException Genericka vyjimka pri ukladani zadosti
     */
    public JSONObject search(String q, String state, List<String> navrhy, String institution, String priority, String delegated, String sort, int rows, int page) throws SolrServerException, IOException,AccountException;


    /**
     * Uzivatel muze mit pouze jednu navrh pro kazdy typ. Dela pripravu pro tuto situaci - vraci existujici otevrenou pokud neni, vytvori a vrati
     * @param navrh Typ navrhu/vyrazeni
     * @return
     * @throws SolrServerException
     * @throws IOException
     * @throws AccountException
     * @throws ConflictException
     */
    public JSONObject prepare(String navrh) throws SolrServerException, IOException, AccountException, ConflictException;


    /**
     * Ulozeni zadosti pro uzivatele, musi byt ve stavu open
     * @param payload Zadost ve formatu json
     * @param inform Informace o ulozeni zdosti
     * @return Vraci json ulozene zadosti
     * @throws SolrServerException Chyba spojeni se solrem
     * @throws IOException IO chyba
     * @throws ConflictException Konflikt, zadost byla zmenene
     * @throws AccountException Genericka vyjimka pri ukladani zadosti
     * @see AccountServiceInform
     */
    public JSONObject saveRequest(String payload, AccountServiceInform inform) throws SolrServerException, IOException, ConflictException, AccountException;



    /**
     * Ulozeni pro kuratory
     * @param payload Zadost ve formatu json
     * @param inform
     * @return
     * @return Vraci json ulozene zadosti
     * @throws SolrServerException Chyba spojeni se solrem
     * @throws IOException IO chyba
     * @throws ConflictException Konflikt, zadost byla zmenene
     * @throws AccountException Genericka vyjimka pri ukladani zadosti
     * @see AccountServiceInform
     */
    public JSONObject saveCuratorRequest(String payload, AccountServiceInform inform) throws SolrServerException, IOException, ConflictException,AccountException;


    /**
     *
     * @param payload
     * @param user
     * @param frbr
     * @param inform
     * @return
     * @throws SolrServerException
     * @throws IOException
     * @throws ConflictException
     * @throws AccountException
     */
    public JSONObject saveRequestWithFRBR(String payload, User user , String frbr, AccountServiceInform inform) throws SolrServerException, IOException, ConflictException,AccountException;

    /**
     * Vraci zazany
     * @param id
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
     * Zavreni pozadavku z pohledu uzivatele
     * @param payload
     * @return
     * @throws ConflictException
     * @throws AccountException
     */
    public JSONObject userCloseRequest(String payload) throws ConflictException,AccountException;

    /**
     * Zarvreni pozdadavku z pohledu kuratora
     * @param payload
     * @return
     * @throws ConflictException
     * @throws AccountException
     */
    public JSONObject curatorCloseRequest(String payload) throws ConflictException,AccountException;

    /**
     * Smaze pozadavek
     * @param payload
     * @return
     * @throws ConflictException
     * @throws AccountException
     * @throws IOException
     * @throws SolrServerException
     */
    public JSONObject deleteRequest(String payload) throws ConflictException, AccountException, IOException, SolrServerException;

    public JSONObject curatorSwitchState(String zadostId, String documentId, String reason) throws ConflictException, AccountException, IOException, SolrServerException;

    public JSONObject curatorSwitchAlternativeState(String alternative, String zadostId, String documentId, String reason) throws ConflictException, AccountException, IOException, SolrServerException;

    public JSONObject curatorRejectSwitchState(String zadostId, String documentId, String reason) throws ConflictException, AccountException, IOException, SolrServerException;


    public void schedulerSwitchStates() throws ConflictException, AccountException, IOException, SolrServerException;

    public void schedulerSwitchStates(String id) throws ConflictException, AccountException, IOException, SolrServerException;

}
