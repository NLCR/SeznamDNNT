package cz.inovatika.sdnnt.services.impl.users;

import cz.inovatika.sdnnt.model.User;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

import java.io.IOException;

public class UsersUtils {

    private UsersUtils() {}

    public static User findOneUser(SolrClient solr, String q) throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery(q)
                .setRows(1);

        QueryResponse usersResult = solr.query("users", query);

        long numFound = usersResult.getResults().getNumFound();
        if (numFound >0 ) {
            SolrDocument document = usersResult.getResults().get(0);
            return User.fromSolrDocument(document);
        } else {
            return null;
        }
    }

    public static User toTOObject(User user) {
        if (user != null) {
            User toObject = new User();
            toObject.setUsername(user.getUsername());
            toObject.setJmeno(user.getJmeno());
            toObject.setPrijmeni(user.getPrijmeni());
            toObject.setApikey(user.getApikey());
            toObject.setResetPwdToken(user.getResetPwdToken());
            toObject.setResetPwdExpiration(  user.getResetPwdExpiration());
            toObject.setEmail( user.getEmail()) ;
            toObject.setNotifikaceInterval(user.getNotifikaceInterval());
            toObject.setNositel(user.getNositel());
            toObject.setRole(user.getRole());
            toObject.setInstitution( user.getInstitution());
            toObject.setIco(user.getIco());
            toObject.setPoznamka(user.getPoznamka());
            toObject.setTyp(user.getTyp());
            toObject.setMesto(user.getMesto());
            toObject.setAdresa(user.getAdresa());
            toObject.setUlice(user.getUlice());
            toObject.setCislo(user.getCislo());
            toObject.setTelefon(user.getTelefon());
            toObject.setPsc(user.getPsc());
            toObject.setNazevSpolecnosti(user.getNazevSpolecnosti());

            return toObject;
        } else {
            return null;
        }
    }
}
