package cz.inovatika.sdnnt.it;

import org.apache.solr.client.solrj.*;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class JUnitClient extends SolrClient {

    EmbeddedSolrServer embeddedSolrServer;

    public JUnitClient(EmbeddedSolrServer embeddedSolrServer) {
        this.embeddedSolrServer = embeddedSolrServer;
    }

    public void down() throws IOException {
        this.embeddedSolrServer.close();
    }

    @Override
    public NamedList<Object> request(SolrRequest solrRequest, String s) throws SolrServerException, IOException {
        return this.embeddedSolrServer.request(solrRequest, s);
    }

    @Override
    public void close() throws IOException {
        // do nothing because of junit tests
    }

    @Override
    public UpdateResponse add(String collection, Collection<SolrInputDocument> docs) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.add(collection, docs);
    }

    @Override
    public UpdateResponse add(Collection<SolrInputDocument> docs) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.add(docs);
    }

    @Override
    public UpdateResponse add(String collection, Collection<SolrInputDocument> docs, int commitWithinMs) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.add(collection, docs, commitWithinMs);
    }

    @Override
    public UpdateResponse add(Collection<SolrInputDocument> docs, int commitWithinMs) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.add(docs, commitWithinMs);
    }

    @Override
    public UpdateResponse add(String collection, SolrInputDocument doc) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.add(collection, doc);
    }

    @Override
    public UpdateResponse add(SolrInputDocument doc) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.add(doc);
    }

    @Override
    public UpdateResponse add(String collection, SolrInputDocument doc, int commitWithinMs) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.add(collection, doc, commitWithinMs);
    }

    @Override
    public UpdateResponse add(SolrInputDocument doc, int commitWithinMs) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.add(doc, commitWithinMs);
    }

    @Override
    public UpdateResponse add(String collection, Iterator<SolrInputDocument> docIterator) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.add(collection, docIterator);
    }

    @Override
    public UpdateResponse add(Iterator<SolrInputDocument> docIterator) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.add(docIterator);
    }

    @Override
    public UpdateResponse addBean(String collection, Object obj) throws IOException, SolrServerException {
        return  this.embeddedSolrServer.addBean(collection, obj);
    }

    @Override
    public UpdateResponse addBean(Object obj) throws IOException, SolrServerException {
        return  this.embeddedSolrServer.addBean(obj);
    }

    @Override
    public UpdateResponse addBean(String collection, Object obj, int commitWithinMs) throws IOException, SolrServerException {
        return  this.embeddedSolrServer.addBean(collection, obj, commitWithinMs);
    }

    @Override
    public UpdateResponse addBean(Object obj, int commitWithinMs) throws IOException, SolrServerException {
        return  this.embeddedSolrServer.addBean(obj, commitWithinMs);
    }

    @Override
    public UpdateResponse addBeans(String collection, Collection<?> beans) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.addBeans(collection, beans);
    }

    @Override
    public UpdateResponse addBeans(Collection<?> beans) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.addBeans(beans);
    }

    @Override
    public UpdateResponse addBeans(String collection, Collection<?> beans, int commitWithinMs) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.addBeans(collection, beans, commitWithinMs);
    }

    @Override
    public UpdateResponse addBeans(Collection<?> beans, int commitWithinMs) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.addBeans(beans, commitWithinMs);
    }

    @Override
    public UpdateResponse addBeans(String collection, Iterator<?> beanIterator) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.addBeans(collection, beanIterator);
    }

    @Override
    public UpdateResponse addBeans(Iterator<?> beanIterator) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.addBeans(beanIterator);
    }

    @Override
    public UpdateResponse commit(String collection) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.commit(collection);
    }

    @Override
    public UpdateResponse commit() throws SolrServerException, IOException {
        return  this.embeddedSolrServer.commit();
    }

    @Override
    public UpdateResponse commit(String collection, boolean waitFlush, boolean waitSearcher) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.commit(collection, waitFlush, waitSearcher);
    }

    @Override
    public UpdateResponse commit(boolean waitFlush, boolean waitSearcher) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.commit(waitFlush, waitSearcher);
    }

    @Override
    public UpdateResponse commit(String collection, boolean waitFlush, boolean waitSearcher, boolean softCommit) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.commit(collection, waitFlush, waitSearcher, softCommit);
    }

    @Override
    public UpdateResponse commit(boolean waitFlush, boolean waitSearcher, boolean softCommit) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.commit(waitFlush, waitSearcher, softCommit);
    }

    @Override
    public UpdateResponse optimize(String collection) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.optimize(collection);
    }

    @Override
    public UpdateResponse optimize() throws SolrServerException, IOException {
        return  this.embeddedSolrServer.optimize();
    }

    @Override
    public UpdateResponse optimize(String collection, boolean waitFlush, boolean waitSearcher) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.optimize(collection, waitFlush, waitSearcher);
    }

    @Override
    public UpdateResponse optimize(boolean waitFlush, boolean waitSearcher) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.optimize(waitFlush, waitSearcher);
    }

    @Override
    public UpdateResponse optimize(String collection, boolean waitFlush, boolean waitSearcher, int maxSegments) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.optimize(collection, waitFlush, waitSearcher, maxSegments);
    }

    @Override
    public UpdateResponse optimize(boolean waitFlush, boolean waitSearcher, int maxSegments) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.optimize(waitFlush, waitSearcher, maxSegments);
    }

    @Override
    public UpdateResponse rollback(String collection) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.rollback(collection);
    }

    @Override
    public UpdateResponse rollback() throws SolrServerException, IOException {
        return  this.embeddedSolrServer.rollback();
    }

    @Override
    public UpdateResponse deleteById(String collection, String id) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.deleteById(collection, id);
    }

    @Override
    public UpdateResponse deleteById(String id) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.deleteById(id);
    }

    @Override
    public UpdateResponse deleteById(String collection, String id, int commitWithinMs) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.deleteById(collection, id, commitWithinMs);
    }

    @Override
    public UpdateResponse deleteById(String id, int commitWithinMs) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.deleteById(id, commitWithinMs);
    }

    @Override
    public UpdateResponse deleteById(String collection, List<String> ids) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.deleteById(collection, ids);
    }

    @Override
    public UpdateResponse deleteById(List<String> ids) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.deleteById(ids);
    }

    @Override
    public UpdateResponse deleteById(String collection, List<String> ids, int commitWithinMs) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.deleteById(collection, ids, commitWithinMs);
    }

    @Override
    public UpdateResponse deleteById(List<String> ids, int commitWithinMs) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.deleteById(ids, commitWithinMs);
    }

    @Override
    public UpdateResponse deleteByQuery(String collection, String query) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.deleteByQuery(collection, query);
    }

    @Override
    public UpdateResponse deleteByQuery(String query) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.deleteByQuery(query);
    }

    @Override
    public UpdateResponse deleteByQuery(String collection, String query, int commitWithinMs) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.deleteByQuery(collection, query, commitWithinMs);
    }

    @Override
    public UpdateResponse deleteByQuery(String query, int commitWithinMs) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.deleteByQuery(query, commitWithinMs);
    }

    @Override
    public SolrPingResponse ping(String collection) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.ping(collection);
    }

    @Override
    public SolrPingResponse ping() throws SolrServerException, IOException {
        return  this.embeddedSolrServer.ping();
    }

    @Override
    public QueryResponse query(String collection, SolrParams params) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.query(collection, params);
    }

    @Override
    public QueryResponse query(SolrParams params) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.query(params);
    }

    @Override
    public QueryResponse query(String collection, SolrParams params, SolrRequest.METHOD method) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.query(collection, params, method);
    }

    @Override
    public QueryResponse query(SolrParams params, SolrRequest.METHOD method) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.query(params, method);
    }

    @Override
    public QueryResponse queryAndStreamResponse(String collection, SolrParams params, StreamingResponseCallback callback) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.queryAndStreamResponse(collection, params, callback);
    }

    @Override
    public QueryResponse queryAndStreamResponse(String collection, SolrParams params, FastStreamingDocsCallback callback) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.queryAndStreamResponse(collection, params, callback);
    }

    @Override
    public QueryResponse queryAndStreamResponse(SolrParams params, StreamingResponseCallback callback) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.queryAndStreamResponse(params, callback);
    }

    @Override
    public SolrDocument getById(String collection, String id) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.getById(collection, id);
    }

    @Override
    public SolrDocument getById(String id) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.getById(id);
    }

    @Override
    public SolrDocument getById(String collection, String id, SolrParams params) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.getById(collection, id, params);
    }

    @Override
    public SolrDocument getById(String id, SolrParams params) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.getById(id, params);
    }

    @Override
    public SolrDocumentList getById(String collection, Collection<String> ids) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.getById(collection, ids);
    }

    @Override
    public SolrDocumentList getById(Collection<String> ids) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.getById(ids);
    }

    @Override
    public SolrDocumentList getById(String collection, Collection<String> ids, SolrParams params) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.getById(collection, ids, params);
    }

    @Override
    public SolrDocumentList getById(Collection<String> ids, SolrParams params) throws SolrServerException, IOException {
        return  this.embeddedSolrServer.getById(ids, params);
    }

    @Override
    public DocumentObjectBinder getBinder() {
        return  this.embeddedSolrServer.getBinder();
    }
}
