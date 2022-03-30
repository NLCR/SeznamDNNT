/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.index.UsersIterationSupport;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.rights.RightsResolver;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.rights.exceptions.NotAuthorizedException;
import cz.inovatika.sdnnt.rights.impl.predicates.MustBeCalledFromLocalhost;
import cz.inovatika.sdnnt.rights.impl.predicates.MustBeLogged;
import cz.inovatika.sdnnt.rights.impl.predicates.UserMustBeInRole;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.NotificationsService;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerExpiredTokenException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerInvalidPwdTokenException;
import cz.inovatika.sdnnt.services.impl.MailServiceImpl;
import cz.inovatika.sdnnt.services.impl.NotificationServiceImpl;
import cz.inovatika.sdnnt.services.impl.users.UserControlerImpl;
import cz.inovatika.sdnnt.services.impl.users.UserValidation;
import cz.inovatika.sdnnt.services.impl.users.UsersUtils;
import cz.inovatika.sdnnt.services.impl.users.validations.EmailValidation;
import cz.inovatika.sdnnt.services.impl.users.validations.EmptyFieldsValidation;
import cz.inovatika.sdnnt.services.impl.users.validations.UserValidationResult;
import cz.inovatika.sdnnt.tracking.TrackSessionUtils;
import cz.inovatika.sdnnt.tracking.TrackingFilter;
import cz.inovatika.sdnnt.utils.PureHTTPSolrUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import static cz.inovatika.sdnnt.rights.Role.admin;
import static cz.inovatika.sdnnt.rights.Role.mainKurator;
import static cz.inovatika.sdnnt.services.ApplicationUserLoginSupport.AUTHENTICATED_USER;
import static cz.inovatika.sdnnt.utils.ServletsSupport.*;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;

/**
 * @author alberto
 */
@WebServlet(name = "UserServlet", urlPatterns = {"/user/*"})
public class UserServlet extends HttpServlet {

    public static final Logger LOGGER = Logger.getLogger(UserServlet.class.getName());

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
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
                if (json != null) {
                    out.println(json.toString(2));
                }
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

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
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

    enum Actions {


        TOUCH {

            private static final int LIMIT = 1000;

            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeCalledFromLocalhost()).permit()) {
                    UsersIterationSupport support = new UsersIterationSupport();
                    try {
                        JSONArray jsonArray = new JSONArray();
                        AtomicInteger number = new AtomicInteger(0);
                        Map<String, String> reqMap = new HashMap<>();
                        reqMap.put("rows", "" + LIMIT);

                        List<String> bulk = new ArrayList<>();
                        support.iterate(reqMap, null, new ArrayList<String>(), new ArrayList<String>(), Arrays.asList("username"), (rsp) -> {
                            Object identifier = rsp.getFieldValue("username");

                            bulk.add(identifier.toString());
                            if (bulk.size() >= LIMIT) {
                                number.addAndGet(bulk.size());
                                LOGGER.info(String.format("Bulk update %d", number.get()));
                                JSONObject returnFromPost = PureHTTPSolrUtils.touchBulk(bulk, "username", support.getCollection());
                                jsonArray.put(returnFromPost);
                                bulk.clear();
                            }
                        }, "username");
                        if (!bulk.isEmpty()) {
                            number.addAndGet(bulk.size());
                            JSONObject returnFromPost = PureHTTPSolrUtils.touchBulk(bulk, "username", support.getCollection());
                            bulk.clear();
                            jsonArray.put(returnFromPost);
                        }

                        JSONObject object = new JSONObject();
                        object.put("numberOfObjects", number.get());
                        object.put("bulkResults", jsonArray);
                        return object;
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                        return errorJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.toString());
                    } finally {
                        PureHTTPSolrUtils.commit(support.getCollection());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "not allowed");
                }
            }

        },

        LOGIN {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                try {
                    UserControlerImpl controler = new UserControlerImpl(req);
                    NotificationsService service = new NotificationServiceImpl(controler, null);
                    User login = controler.login();
                    if (login != null) {
                        return UsersUtils.prepareUserLoggedObject(controler, service, login);
                    } else {
                        return new JSONObject();
                    }
                } catch (UserControlerException e) {
                    return errorJson(e.getMessage());
                }
            }
        },

        LOGOUT {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                try {
                    User logout = new UserControlerImpl(req).logout();
                    return logout != null ? logout.toJSONObject() : new JSONObject();
                } catch (UserControlerException e) {
                    return errorJson(e.getMessage());
                }
            }
        },

        /**
         * Redirect to IdentityProvider or wayfling
         */
        SHIB_LOGIN_REDIRECT {
            @Override
            JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {
                String shibboleth = Options.getInstance().getString("shibiplink");
                if (shibboleth != null) {
                    response.sendRedirect(shibboleth);
                } else {
                    LOGGER.warning("No link to shibboleth");
                }
                return null;
            }
        },

        /**
         * Callback from IP
         */
        SHIB_LOGIN_CALLBACK {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                User user = (User) req.getSession(true).getAttribute(AUTHENTICATED_USER);
                req.getSession(true).setAttribute(AUTHENTICATED_USER, user);
                TrackSessionUtils.touchSession(req.getSession());
                // landing page must be aware that user info is needed - no login is here
                response.sendRedirect("/shibboleth-landing");
                return null;
            }
        },

        /* User info; regular users */
        USER_INFO {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                User logged = (User) req.getSession(true).getAttribute(AUTHENTICATED_USER);
                if (!logged.isThirdPartyUser()) {
                    JSONObject jsonObject = logged.toJSONObject();
                    return jsonObject;
                } else return new JSONObject();
            }
        },

        /* User info; shibboleth users */
        SHIB_USER_INFO {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                User logged = (User) req.getSession(true).getAttribute(AUTHENTICATED_USER);
                if (logged.isThirdPartyUser()) {
                    JSONObject jsonObject = logged.toJSONObject();
                    return jsonObject;
                } else return new JSONObject();
            }
        },

        PING {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                JSONObject retVal = new JSONObject();
                if (new UserControlerImpl(req).getUser() != null) {
                    retVal.put("pinginguser", new UserControlerImpl(req).getUser().getUsername());
                }
                if (req.getSession() != null && req.getSession().getAttribute(TrackingFilter.REMAINING_TIME) != null) {
                    retVal.put("remainingtime", req.getSession().getAttribute(TrackingFilter.REMAINING_TIME));
                }
                return retVal;
            }
        },

        PONG {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                JSONObject retVal = new JSONObject();
                if (new UserControlerImpl(req).getUser() != null) {
                    retVal.put("poinginguser", new UserControlerImpl(req).getUser().getUsername());
                }
                return retVal;
            }
        },

        // posle link uzivateli ze si ma vygenerovat heslo
        FORGOT_PWD {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                try {
                    new UserControlerImpl(req, new MailServiceImpl()).forgotPwd(readInputJSON(req));
                    JSONObject object = new JSONObject();
                    object.put("result", "token has been sent");
                    return object;
                } catch (UserControlerException e) {
                    return errorJson(e.getMessage());
                }
            }
        },
        CHANGE_PWD_USER {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                try {
                    if (new RightsResolver(req, new MustBeLogged()).permit()) {
                        User pswd = new UserControlerImpl(req).changePwdUser(readInputJSON(req).optString("pswd"));
                        return pswd != null ? pswd.toJSONObject() : new JSONObject();
                    } else {
                        throw new NotAuthorizedException("not authorized");
                    }
                } catch (UserControlerException e) {
                    return errorJson(e.getMessage());
                } catch (NotAuthorizedException e) {
                    response.setStatus(SC_FORBIDDEN);
                    return errorJson(e.getMessage());
                } catch (IOException e) {
                    return errorJson(e.getMessage());
                }
            }
        },

        CHANGE_PWD_TOKEN {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                String token = null;
                String pswd = null;
                try {
                    JSONObject object = readInputJSON(req);
                    token = object.optString("resetPwdToken", "");
                    pswd = object.optString("pswd", "");
                    return new UserControlerImpl(req).changePwdToken(token, pswd).toJSONObject();
                } catch (IOException | UserControlerException | UserControlerInvalidPwdTokenException | UserControlerExpiredTokenException e) {
                    return errorJson(e.getMessage());
                }
            }
        },

        VALIDATE_PWD_TOKEN {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                String token = req.getParameter("token");
                JSONObject retvalue = new JSONObject();
                if (token != null) {
                    retvalue.put("valid", new UserControlerImpl(req).validatePwdToken(token));
                } else {
                    retvalue.put("valid", false);
                }
                return retvalue;
            }
        },


        // mail o resetovanem hesle - admin rozhrani
        ADMIN_RESET_PWD {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(admin)).permit()) {
                    User user = new UserControlerImpl(req, new MailServiceImpl()).resetPwd(readInputJSON(req));
                    return user.toJSONObject();
                } else {
                    return errorJson(response, SC_FORBIDDEN, "not allowed");
                }
            }
        },

        // save default;registred user
        SAVE {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {

                if (new RightsResolver(req, new MustBeLogged()).permit()) {
                    User sender = new UserControlerImpl(req).getUser();
                    JSONObject savingUser = readInputJSON(req);
                    if (sender.getUsername().equals(savingUser.optString("username"))) {
                        AtomicReference<String> errorMessage = new AtomicReference<>();
                        UsersUtils.userValidation(savingUser, (errorFields, validationId) -> {
                            if (validationId.equals(EmptyFieldsValidation.class.getName())) {
                                String message = String.format("Fields %s cannot be empty", errorFields);
                                errorMessage.set(message);
                            }
                            if (validationId.equals(EmailValidation.class.getName())) {
                                String message = String.format("email field %s is not valid %s", User.EMAIL_KEY, savingUser.getString(User.EMAIL_KEY));
                                errorMessage.set(message);
                            }
                        });

                        if (errorMessage.get() != null) {
                            return errorJson(response, SC_BAD_REQUEST, errorMessage.get());
                        } else {
                            return new UserControlerImpl(req).userSave(User.fromJSON(savingUser.toString())).toJSONObject();
                        }
                    } else {
                        // must be admin
                        if (new RightsResolver(req, new UserMustBeInRole(admin)).permit()) {
                            // must be load first and then

                            return new UserControlerImpl(req).adminSave(User.fromJSON(savingUser.toString())).toJSONObject();
                        } else {
                            return errorJson(response, SC_FORBIDDEN, "not allowed");
                        }
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "not allowed");

                }
            }
        },
        // registrace noveho uzivatele
        REGISTER {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                // TODO: MailService ?? Create? Inject ?
                try {
                    return new UserControlerImpl(req, new MailServiceImpl()).register(readInputJSON(req).toString()).toJSONObject();
                } catch (UserControlerException e) {
                    return errorJson(e.getMessage());
                } catch (IOException e) {
                    return errorJson(e.getMessage());
                }
            }
        },
        ALL {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(admin)).permit()) {
                    JSONObject retval = new JSONObject();
                    JSONArray docs = new JSONArray();
                    new UserControlerImpl(req).getAll().stream().map(User::toJSONObject).forEach(docs::put);
                    retval.put("docs", docs);
                    return retval;
                } else {
                    return errorJson(response, SC_FORBIDDEN, "not allowed");
                }
            }
        },

        USERS_BY_ROLE {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(admin, mainKurator)).permit()) {
                    JSONObject retval = new JSONObject();
                    JSONArray docs = new JSONArray();
                    String role = req.getParameter("role");
                    new UserControlerImpl(req).findUsersByRole(role != null ? Role.valueOf(role) : Role.user).stream().map(User::toJSONObject).forEach(docs::put);
                    retval.put("docs", docs);
                    return retval;
                } else {
                    return errorJson(response, SC_FORBIDDEN, "not allowed");
                }
            }
        },

        USERS_BY_PREFIX {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(admin, mainKurator)).permit()) {
                    JSONObject retval = new JSONObject();
                    JSONArray docs = new JSONArray();
                    String prefix = req.getParameter("prefix");
                    List<User> usersByPrefix = new UserControlerImpl(req).findUsersByPrefix(prefix);
                    usersByPrefix.stream().map(User::toJSONObject).forEach(docs::put);
                    retval.put("docs", docs);
                    return retval;
                } else {
                    return errorJson(response, SC_FORBIDDEN, "not allowed");
                }
            }
        },


        INSTITUTIONS {
            @Override
            JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {
                JSONArray institutions = Options.getInstance().getJSONArray("institutions");
                JSONObject retVal = new JSONObject();
                retVal.put("institutions", institutions != null ? institutions : new JSONArray());
                return retVal;
            }
        };

        abstract JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception;
    }

}
