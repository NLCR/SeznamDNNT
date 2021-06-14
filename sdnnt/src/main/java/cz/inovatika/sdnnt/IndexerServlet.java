package cz.inovatika.sdnnt;

import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.index.OAIHarvester;
import cz.inovatika.sdnnt.index.XMLImporter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@WebServlet(value = "/index/*")
public class IndexerServlet extends HttpServlet {

  public static final Logger LOGGER = Logger.getLogger(IndexerServlet.class.getName());
  public static final String ACTION_NAME = "action";

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
   * methods.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
    response.setHeader("Pragma", "no-cache"); // HTTP 1.0
    response.setDateHeader("Expires", 0); // Proxies.
    PrintWriter out = response.getWriter();
    try {
      String actionNameParam = request.getPathInfo().substring(1);
      if (actionNameParam != null) {
        Actions actionToDo = Actions.valueOf(actionNameParam.toUpperCase());
        JSONObject json = actionToDo.doPerform(request, response);
        out.println(json.toString(2));
      } else {

        out.print("actionNameParam -> " + actionNameParam);
      }
    } catch (IOException e1) {
      LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.toString());
      out.print(e1.toString());
    } catch (SecurityException e1) {
      LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    } catch (Exception e1) {
      LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.toString());
      out.print(e1.toString());
    }

  }

  enum Actions {
    REINDEX_ID {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        try {
          json.put("indexed", Indexer.reindex(req.getParameter("id")));
          
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    REINDEX_FILTER {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        try {
          json.put("indexed", Indexer.reindexFilter(req.getParameter("filter")));
          
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    FULL {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        try {
          OAIHarvester oai = new OAIHarvester();
          String set = "SKC";
          String core = "catalog";
          if (req.getParameter("set") != null) {
            set = req.getParameter("set");
          }
          if (req.getParameter("core") != null) {
            core = req.getParameter("core");
          }
          json.put("indexed", oai.full(set, core, 
                  Boolean.parseBoolean(req.getParameter("merge")),
                  Boolean.parseBoolean(req.getParameter("allFields"))));
          
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    UPDATE {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        try {
          OAIHarvester oai = new OAIHarvester();
          String set = "SKC";
          String core = "catalog";
          if (req.getParameter("set") != null) {
            set = req.getParameter("set");
          }
          if (req.getParameter("core") != null) {
            core = req.getParameter("core");
          }
          json.put("indexed", oai.update(set, core, 
                  Boolean.parseBoolean(req.getParameter("merge")),
                  Boolean.parseBoolean(req.getParameter("allFields"))));

        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    IMPORTXML {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        try {
          XMLImporter imp = new XMLImporter();
          json.put("indexed", imp.fromFile("C:/Users/alberto/Projects/SDNNT/Docs/albatros.xml"));
          
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    COMPARE {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        try {
          Indexer indexer = new Indexer();
          json = indexer.compare(req.getParameter("id"));
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    FINDID {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject ret = new JSONObject();
        try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host", "http://localhost:8983/solr/")).build()) {

          SolrQuery q = new SolrQuery("*").setRows(1)
                  .addFilterQuery("identifier:\"" + req.getParameter("id") + "\"")
                  .setFields("raw");

          SolrDocumentList docs = solr.query("sdnnt", q).getResults();
          if (docs.getNumFound() == 0) {
            ret.put("error", "Record not found in sdnnt");
            return ret;
          }
          SolrDocument docDnt = docs.get(0);
          String jsDnt = (String) docDnt.getFirstValue("raw");
          SolrDocumentList catDocs = Indexer.find(jsDnt);
          
          if (catDocs.getNumFound() == 0) {
            ret.put("error", "Record not found in catalog");
            return ret;
          } else if (catDocs.getNumFound() > 1) {
            ret.put("error", "Found " + catDocs.getNumFound() + " records in catalog");
            
            List<JSONObject> diffs = new ArrayList<>();
            for (SolrDocument doc : catDocs) {
              diffs.add(Indexer.compare(jsDnt, (String) doc.getFirstValue("raw")));
            }
            for (int i = 0; i < diffs.size()-1; i++) {
              ret.append("diffs", Indexer.compare(diffs.get(i).toString(), diffs.get(i+1).toString()));
            }
            ret.put("records", diffs);
          } else {
            ret.append("docs", catDocs);
          }
        } catch (Exception ex) { 
          LOGGER.log(Level.SEVERE, null, ex);
          ret.put("error", ex.toString());
        }
        return ret;
      }
    },
    MERGEID {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        try {
          Indexer indexer = new Indexer();
          json = indexer.mergeId("sdnnt", req.getParameter("id"), "testUser");
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    TEST {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        try {
          Indexer indexer = new Indexer();
          json = indexer.test("sdnnt", req.getParameter("id"));
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    MERGECORE {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        try {
          Indexer indexer = new Indexer();
          json = indexer.mergeCore("sdnnt", "testUser", req.getParameter("from"));
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    SAVE {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        JSONObject user = (JSONObject) req.getSession().getAttribute("user");
        if (user == null) {
          json.put("error", "Not logged");
          user = new JSONObject().put("name", "testUser");
          // return json;
        }
        try {
          Indexer indexer = new Indexer();
          JSONObject inputJs;
          if (req.getMethod().equals("POST")) {
            inputJs = new JSONObject(IOUtils.toString(req.getInputStream(), "UTF-8"));
          } else {
            inputJs = new JSONObject(req.getParameter("json"));
          }
          json = indexer.save(req.getParameter("id"), inputJs, user.getString("name"));
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    };

    abstract JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception;
  }

  // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
  /**
   * Handles the HTTP <code>GET</code> method.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    processRequest(request, response);
  }

  /**
   * Handles the HTTP <code>POST</code> method.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    processRequest(request, response);
  }

  /**
   * Returns a short description of the servlet.
   *
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo() {
    return "Short description";
  }// </editor-fold>

}
