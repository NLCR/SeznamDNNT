package cz.inovatika.sdnnt;

import static cz.inovatika.sdnnt.rights.Role.admin;
import static cz.inovatika.sdnnt.rights.Role.kurator;
import static cz.inovatika.sdnnt.rights.Role.mainKurator;
import static cz.inovatika.sdnnt.utils.ServletsSupport.errorJson;
import static cz.inovatika.sdnnt.utils.ServletsSupport.errorMissingParameterJson;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.rights.RightsResolver;
import cz.inovatika.sdnnt.rights.impl.predicates.MustBeCalledFromLocalhost;
import cz.inovatika.sdnnt.rights.impl.predicates.MustBeLogged;
import cz.inovatika.sdnnt.rights.impl.predicates.UserMustBeInRole;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.ExportService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exports.ExportType;
import cz.inovatika.sdnnt.services.impl.AccountServiceImpl;
import cz.inovatika.sdnnt.services.impl.ExportServiceImpl;
import cz.inovatika.sdnnt.services.impl.ResourceBundleServiceImpl;
import cz.inovatika.sdnnt.services.impl.users.UserControlerImpl;



@WebServlet(value = "/iexports/*")
public class ExportServlet extends HttpServlet {
    
    public static final Logger LOGGER = Logger.getLogger(ExportServlet.class.getName());

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String actionNameParam = request.getPathInfo().substring(1);
            if (actionNameParam != null) {
                Actions actionToDo = Actions.valueOf(actionNameParam.toUpperCase());
                JSONObject json = actionToDo.doPerform(request, response);
                if (actionToDo.hasJSONResult()) {
                    jsonWriter(response).println(json.toString(2));
                }
            } else {
                jsonWriter(response).print("actionNameParam -> " + actionNameParam);
            }
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.toString());
            jsonWriter(response).print(e1.toString());
        } catch (SecurityException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.toString());
            jsonWriter(response).print(e1.toString());
        }

    }

    private PrintWriter jsonWriter(HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0
        response.setDateHeader("Expires", 0); // Proxies.
        PrintWriter out = response.getWriter();
        return out;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    public void init() throws ServletException {
        super.init();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }


    enum Actions {
        
        APPROVE_ITEM_EXPORT_IOCP {

            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(kurator, mainKurator)).permit()) {
                    String exportId = req.getParameter("exportname");
                    String item = req.getParameter("id");
                    if (exportId != null) {
                        UserControlerImpl uc = new UserControlerImpl(req);
                        ExportService ex = new ExportServiceImpl(uc, new ResourceBundleServiceImpl(req));
                        JSONObject approvedExport = ex.approveExportItemIOCP(exportId, item);
                        return approvedExport;
                    } else {
                        return errorMissingParameterJson(response, "exportname");
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
        },

        APPROVE_ITEM_EXPORT_UOCP {

            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(kurator, mainKurator)).permit()) {
                    String exportId = req.getParameter("exportname");
                    String item = req.getParameter("id");
                    if (exportId != null) {
                        UserControlerImpl uc = new UserControlerImpl(req);
                        ExportService ex = new ExportServiceImpl(uc, new ResourceBundleServiceImpl(req));
                        JSONObject approvedExport = ex.approveExportItemUOCP(exportId, item);
                        return approvedExport;
                    } else {
                        return errorMissingParameterJson(response, "exportname");
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
        },

        
        
        PROCESS_EXPORT {

            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(kurator, mainKurator)).permit()) {
                    String id = req.getParameter("exportname");
                    if (id != null) {
                        UserControlerImpl uc = new UserControlerImpl(req);
                        ExportService ex = new ExportServiceImpl(uc, new ResourceBundleServiceImpl(req));
                        return ex.setExportProcessed(id);
                    } else {
                        return errorMissingParameterJson(response, "exportname");
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
            
        },
        
        APPROVE_EXPORT_IOCP {

            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(kurator, mainKurator)).permit()) {
                    String id = req.getParameter("exportname");
                    if (id != null) {
                        UserControlerImpl uc = new UserControlerImpl(req);
                        ExportService ex = new ExportServiceImpl(uc, new ResourceBundleServiceImpl(req));
                        return ex.approveExportIOCP(id);
                    } else {
                        return errorMissingParameterJson(response, "exportname");
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
            
        },

        APPROVE_EXPORT_UOCP {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(kurator, mainKurator)).permit()) {
                    String id = req.getParameter("exportname");
                    if (id != null) {
                        UserControlerImpl uc = new UserControlerImpl(req);
                        ExportService ex = new ExportServiceImpl(uc, new ResourceBundleServiceImpl(req));
                        return ex.approveExportUOCP(id);
                    } else {
                        return errorMissingParameterJson(response, "exportname");
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
            
        },

        PREPARE_EXPORT {

            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req,  new MustBeCalledFromLocalhost()).permit()) {

                    String id = req.getParameter("id");
                    String numberOfDoc = req.getParameter("numberOfDoc");
                    String exportType = req.getParameter("export_type");

                    //String exportName = req.getParameter("export_name");
                    if (id != null && exportType != null) {
                        //ExportType exportType = ExportType.valueOf(exportName);
                        try {
                            UserControlerImpl uc = new UserControlerImpl(req);
                            ExportService ex = new ExportServiceImpl(uc, new ResourceBundleServiceImpl(req));
                            return ex.createExport(id, ExportType.valueOf(exportType), Integer.parseInt(numberOfDoc));
                        } catch (SolrServerException | IOException ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
                        }
                    } else {
                        
                        if (id == null) {
                            return errorMissingParameterJson(response, "id");
                        } else {
                            return errorMissingParameterJson(response, "export_type");
                        }
                    }
                    
                } else {
                    return errorJson(response, SC_FORBIDDEN, "not allowed");
                }
            }

            
        },
        
        SEARCH_EXPORT {

            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {


                    String q = req.getParameter("q");
                    String exportName = req.getParameter("exportname");
                    String page = req.getParameter("page");
                    page = page != null ? page : "0";
                    String rows = req.getParameter("rows");
                    rows = rows != null ? rows : "20";
                    
                    try {
                        UserControlerImpl uc = new UserControlerImpl(req);

                        // 
                        Map<String, String[]> parameterMap = new HashMap<>(req.getParameterMap());
                        parameterMap.remove("exportname");
                        
                        Map<String, String> resmap = new HashMap<>();
                        parameterMap.entrySet().stream().forEach(stringEntry -> {
                            resmap.put(stringEntry.getKey(), stringEntry.getValue()[0]);
                        });

                        ExportService ex = new ExportServiceImpl(uc, new ResourceBundleServiceImpl(req));
                        return ex.searchInExport(resmap,  exportName != null ? exportName : "-", q, Integer.parseInt(page), Integer.parseInt(rows));
                    } catch (SolrServerException | IOException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                        return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
            
        },
        
        SEARCH {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(),new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {


                    String q = req.getParameter("q");
                    String exportType = req.getParameter("export_type");
                    String page = req.getParameter("page");
                    page = page != null ? page : "0";
                    String rows = req.getParameter("rows");
                    rows = rows != null ? rows : "20";
                    
                    try {
                        UserControlerImpl uc = new UserControlerImpl(req);
                        ExportService ex = new ExportServiceImpl(uc, new ResourceBundleServiceImpl(req));
                        return ex.search(q, exportType != null ? ExportType.valueOf(exportType) : null,  Integer.parseInt(rows),Integer.parseInt(page));
                    } catch (SolrServerException | IOException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                        return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
        },
        
        EXPORT {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(),new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {

                    String exportName = req.getParameter("exportname");
                    if (exportName != null) {
                        try {
                            UserControlerImpl uc = new UserControlerImpl(req);
                            ExportService ex = new ExportServiceImpl(uc, new ResourceBundleServiceImpl(req));
                            
                            //ex.createExport(null, null, SC_NOT_FOUND)
                            
                            return ex.getExport(exportName);
                        } catch (SolrServerException | IOException ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
                        }
                    } else {
                        return errorMissingParameterJson(response, "exportname");
                        
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
        },

        EXPORTED_FILES_DESC {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(),new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {

                    String exportName = req.getParameter("exportname");
                    if (exportName != null) {
                        try {
                            UserControlerImpl uc = new UserControlerImpl(req);
                            ExportService ex = new ExportServiceImpl(uc, new ResourceBundleServiceImpl(req));
                            return ex.exportFiles(exportName);
                        } catch (SolrServerException | IOException ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
                        }
                    } else {
                        return errorMissingParameterJson(response, "exportname");
                        
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
        },
        
        
        
        EXPORTED_FILE {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(),new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {

                    String exportName = req.getParameter("exportname");
                    String exportFilePath = req.getParameter("path");
                    if (exportName != null &&  exportFilePath != null) {
                        try {
                            UserControlerImpl uc = new UserControlerImpl(req);
                            ExportService ex = new ExportServiceImpl(uc, new ResourceBundleServiceImpl(req));

                            byte[] data = ex.exportedFile(exportName, exportFilePath);
                            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                            
                            File file = new File(exportFilePath);
                            response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
                            try (OutputStream outputStream = response.getOutputStream()) {
                                outputStream.write(data);
                            }
                            return null;
                        } catch (IOException ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
                        }
                    } else {
                        if (exportName == null)  return errorMissingParameterJson(response, "exportname");
                        else return errorMissingParameterJson(response, "path");
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }

            @Override
            boolean hasJSONResult() {
                return false;
            }
            
        };
        
        abstract JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception;

        boolean hasJSONResult() {
            return true;
        }
        
        //abstract JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception;
}
    
}
