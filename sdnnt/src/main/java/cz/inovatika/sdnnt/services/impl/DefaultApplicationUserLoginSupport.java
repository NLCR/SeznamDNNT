package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.tracking.TrackSessionUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static cz.inovatika.sdnnt.services.impl.users.UsersUtils.*;

public class DefaultApplicationUserLoginSupport implements ApplicationUserLoginSupport {

    private HttpServletRequest request;

    public DefaultApplicationUserLoginSupport(HttpServletRequest servletRequest) {
        this.request = servletRequest;
    }

    @Override
    public User login() throws UserControlerException {
        try(SolrClient client = buildClient()) {
            JSONObject ret = new JSONObject();
            try {
                JSONObject json = new JSONObject(IOUtils.toString(this.request.getInputStream(), "UTF-8"));
                String username = json.getString("user");
                String pwdHashed =  DigestUtils.sha256Hex(json.getString("pwd"));
                ret.put("username", username);
                User user = findOneUser(client, "username:\"" + username + "\"");
                if (user != null && user.getPwd() != null && user.getPwd().equals(pwdHashed)) {
                    setSessionObject(this.request, user);

                    return toTOObject(user);
                } else throw new UserControlerException("Cannot find user or invalid password");
            } catch (Exception ex) {
                throw new UserControlerException(ex);
            }
        } catch (IOException ex) {
            throw new UserControlerException(ex);
        }

    }



    private void setSessionObject(HttpServletRequest req, User user) {
        req.getSession(true).setAttribute(AUTHENTICATED_USER, user);
        TrackSessionUtils.touchSession(req.getSession());
    }

    @Override
    public User logout() throws UserControlerException {
        this.request.getSession().invalidate();
        return getUser();
    }

    @Override
    public User getUser() {
        return (User) this.request.getSession(true).getAttribute(AUTHENTICATED_USER);
    }

    SolrClient buildClient() {
        return new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build();
    }

}
