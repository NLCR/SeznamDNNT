package cz.inovatika.sdnnt.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.indexer.models.NotificationInterval;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.services.UserController;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.services.impl.users.UsersUtils;
import cz.inovatika.sdnnt.utils.SolrJUtilities;

public abstract class AbstractUserController implements UserController {

    private static int USERS_LIMIT = 10000;


    public SolrClient buildClient() {
        return new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build();
    }

    protected void atomicUpdate(SolrInputDocument idoc, Object fValue, String fName) {
        Map<String, Object> modifier = new HashMap<>(1);
        modifier.put("set", fValue);
        idoc.addField(fName, modifier);
    }

    protected User changeIntervalImpl(String username, NotificationInterval interval, String dataCollection)
            throws UserControlerException {
        try (SolrClient solr = buildClient()) {
            try {
                SolrInputDocument idoc = new SolrInputDocument();
                idoc.setField("username", username);
                atomicUpdate(idoc, interval.name(), "notifikace_interval");
                solr.add(dataCollection, idoc);
            } catch (SolrServerException e) {
                throw new UserControlerException(e);
            } finally {
                SolrJUtilities.quietCommit(solr, dataCollection);
            }
        } catch ( IOException ex) {
            throw new UserControlerException(ex);
        }
        return findUser(username);
    }
    
    
    protected List<User> getUsersImpl(String collection) throws UserControlerException {
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery("*:*")
                    .setRows(USERS_LIMIT);
            QueryResponse users = solr.query(collection, query);
            List<User> collect = users.getResults().stream().map(User::fromSolrDocument).map(UsersUtils::toTOObject).collect(Collectors.toList());
            //return solr.query("users", query).getBeans(User.class).stream().map(this::toTOObject).collect(Collectors.toList());
            return collect;
        } catch (SolrServerException | IOException ex) {
            throw new UserControlerException(ex);
        }
    }


    @Override
    public List<Zadost> getZadost(String username) throws UserControlerException {
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery("user:\"" + username + "\"")
                    .addFilterQuery("state:open")
                    .setFields("id", "identifiers", "typ", "user", "state", "navrh", "poznamka", "pozadavek", "datum_zadani", "datum_vyrizeni", "formular")
                    .setRows(10);
            //List<Zadost> zadost = solr.query("zadost", query).getBeans(Zadost.class);
    
            List<Zadost> zadosti = new ArrayList<>();
            SolrJUtilities.jsonDocsFromResult(solr, query, "zadost").forEach(z-> {
                zadosti.add(Zadost.fromJSON(z.toString()));
            });
    
            return zadosti;
        } catch (SolrServerException | IOException ex) {
            throw new  UserControlerException(ex);
        }
    }
    
    protected List<User> findUsersByRole(Role role, String collection) throws UserControlerException {
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery("role:\""+role.name()+"\"")
                    .setRows(1000);
            QueryResponse usersResponse = solr.query(collection, query);
            List<User> users = usersResponse.getResults().stream().map(User::fromSolrDocument).map(UsersUtils::toTOObject).collect(Collectors.toList());
            return users;

        } catch (SolrServerException | IOException ex) {
            throw new UserControlerException(ex);
        }
    }


    protected List<User> findUserByPrefixImpl(String prefix, String collection) throws UserControlerException {
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery(String.format("fullText:%s*",  prefix))
                    .setRows(1000);
            QueryResponse users = solr.query(collection, query);
            List<User> userList = users.getResults().stream().map(User::fromSolrDocument).collect(Collectors.toList());
            List<User> collect = userList.stream().map(UsersUtils::toTOObject).collect(Collectors.toList());
            return collect;
            //return solr.query("users", query).getBeans(User.class).stream().map(this::toTOObject).collect(Collectors.toList());
        } catch (SolrServerException | IOException ex) {
            throw new UserControlerException(ex);
        }
    }
    

    protected  User save(User user, SolrClient client, String collection) throws IOException, SolrServerException {
        try {
            client.add(collection, user.toSolrInputDocument());
            return UsersUtils.toTOObject(user);
        } finally {
            SolrJUtilities.quietCommit(client, collection);
        }
    }


    public List<User> findUsersByNotificationIntervalImpl(String interval, String collection)
            throws UserControlerException {
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery("notifikace_interval:\""+interval+"\"")
                    .setRows(1000);
            QueryResponse usersResponse = solr.query(collection, query);
            List<User> users = usersResponse.getResults().stream().map(User::fromSolrDocument).map(UsersUtils::toTOObject).collect(Collectors.toList());
            return users;
        } catch (SolrServerException | IOException ex) {
            throw new UserControlerException(ex);
        }
    }

}
