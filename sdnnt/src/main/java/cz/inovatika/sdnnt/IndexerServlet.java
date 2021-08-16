package cz.inovatika.sdnnt;

import cz.inovatika.sdnnt.index.DntAlephImporter;
import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.index.OAIHarvester;
import cz.inovatika.sdnnt.index.XMLImporterDistri;
import cz.inovatika.sdnnt.index.XMLImporterHeureka;
import cz.inovatika.sdnnt.index.XMLImporterKosmas;
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

import cz.inovatika.sdnnt.utils.ServletsSupport;
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
    // pro interni ucely
    REINDEX_ID {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        try {
          json.put("indexed", Indexer.reindexId(req.getParameter("id")));
          
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    // pro interni ucely 
    REINDEX {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        try {
          String dest = Options.getInstance().getString("solr.host");
          String collection = "catalog";
          if (req.getParameter("dest") != null) {
            dest = req.getParameter("dest"); 
          }
          if (req.getParameter("collection") != null) {
            collection = req.getParameter("collection"); 
          }
          json.put("indexed", Indexer.reindex(dest, req.getParameter("filter"), collection, Boolean.parseBoolean(req.getParameter("cleanStav"))));
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    // plny harvest  skc katalogu - api operace - dostupne pouze pro admin
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
                  Boolean.parseBoolean(req.getParameter("keepDNTFields")),
                  Boolean.parseBoolean(req.getParameter("allFields"))));
          
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    // pokracovani harvestu - posledni zaznam from until
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
                  true,
                  Boolean.parseBoolean(req.getParameter("allFields"))));

        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    // harvest s datum yyyy-MM-ddTHH:mm:ssZ 2021-01-31T10:19:09Z - posledni zaznam from until
    UPDATE_FROM {
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
          json.put("indexed", oai.updateFrom(set, core, 
                  req.getParameter("from"),
                  Boolean.parseBoolean(req.getParameter("merge")),
                  true,
                  Boolean.parseBoolean(req.getParameter("allFields"))));

        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    // import info o stavu z Alephu DNT set
    IMPORT_DNTSET {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        try {
          DntAlephImporter imp = new DntAlephImporter();
          json = imp.run(req.getParameter("from"));
          
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    // import zaznamu z kosmas - uzivatelske api
    IMPORT_KOSMAS {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        try {
          XMLImporterKosmas imp = new XMLImporterKosmas();
          // https://www.kosmas.cz/atl_shop/nkp.xml
          json = imp.fromFile(req.getParameter("url"), "kosmas");
          
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    // import zaznamu z distri.cz - uzivatelske api
    IMPORT_DISTRI {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        try {
          XMLImporterDistri imp = new XMLImporterDistri();
          // json.put("indexed", imp.fromFile("C:/Users/alberto/Projects/SDNNT/Docs/albatros.xml"));
          // json = imp.fromFile("C:/Users/alberto/Projects/SDNNT/Docs/XmlFeedFull.xml", "albatros");
          // "C:/Users/alberto/Projects/SDNNT/Docs/XmlFeedCompact.xml"
          json = imp.fromFile(req.getParameter("url"), "distri.cz");
          
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    // import zaznamu z palmknihy.cz - uzivatelske api
    IMPORT_HEUREKA {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        try {
          XMLImporterHeureka imp = new XMLImporterHeureka();
          // json = imp.fromFile("C:/Users/alberto/Projects/SDNNT/Docs/heureka.xml", "palmknihy", "SHOPITEM");
          // https://www.palmknihy.cz/heureka.xml
          json = imp.fromFile(req.getParameter("url"), "heureka");
          
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    // pro interni ucely  - porovnani dvou zaznamu
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
    // hleda a najde zaznam podle identifikatoru a vrati marc21
    // api
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
          JSONObject inputJs = ServletsSupport.readInputJSON(req);
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
