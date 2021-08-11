package cz.inovatika.sdnnt.index;

import cz.inovatika.sdnnt.indexer.models.User;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CursorMarkParams;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generic iteration support
 */
public class CatalogIterationSupport {

    public static final Logger LOGGER = Logger.getLogger(CatalogIterationSupport.class.getName());

    /**
     * One page iteration
     * @param rows Number of rows
     * @param cursorMark Cursor mark
     * @param req Hashmap represents request parameters
     * @param user User
     * @param plusFilter List of filters - positive
     * @param minusFilter  List of filters - negative
     * @param fields Returning fields
     * @param consumer Consumer closure
     */
    public void iterateOnePage(int rows, String cursorMark, Map<String, String> req, User user, List<String> plusFilter , List<String> minusFilter, List<String> fields, Consumer<QueryResponse> consumer) {
        SolrClient solr = Indexer.getClient();

        try {
            SolrQuery q = (new SolrQuery("*")).setRows(rows).setSort(SolrQuery.SortClause.asc("identifier"));

            plusFilter.stream().forEach(q::addFilterQuery);
            minusFilter.stream().map(it-> "NOT "+it).forEach(q::addFilterQuery);
            fields.stream().forEach(q::addField);

            q.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);

            QueryResponse rsp = solr.query("catalog",q);
            consumer.accept(rsp);

        } catch (SolrServerException | IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
        }
    }

    /**
     * Full index iteration
     * @param req Hashmap represents user's request
     * @param user Caller
     * @param plusFilter List of filters - positive
     * @param minusFilter List of filters - negative
     * @param fields Returning fields
     * @param consumer Consumer closure
     */
    public  void iterate(Map<String, String> req, User user, List<String> plusFilter , List<String> minusFilter,List<String> fields, Consumer<SolrDocument> consumer) {
        int rows = 3000;//opts.getClientConf().getInt("rows");
        if (req.containsKey("rows")) {
            rows = Integer.parseInt(req.get("rows"));
        }
        SolrClient solr = Indexer.getClient();

        try {
            SolrQuery q = (new SolrQuery("*")).setRows(rows).setSort(SolrQuery.SortClause.asc("identifier"));

            plusFilter.stream().forEach(q::addFilterQuery);
            minusFilter.stream().map(it-> "NOT "+it).forEach(q::addFilterQuery);

            fields.stream().forEach(q::addField);

            String cursorMark = CursorMarkParams.CURSOR_MARK_START;
            boolean done = false;
            while (! done) {
                q.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);

                QueryResponse rsp = solr.query("catalog",q);

                String nextCursorMark = rsp.getNextCursorMark();
                for (SolrDocument resultDoc: rsp.getResults()) {
                    consumer.accept(resultDoc);
                }

                if (cursorMark.equals(nextCursorMark)) {
                    done = true;
                }
                cursorMark = nextCursorMark;
            }
        } catch (SolrServerException| IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
        }
    }

}
