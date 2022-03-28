package cz.inovatika.sdnnt.servlets;

import static cz.inovatika.sdnnt.utils.ServletsSupport.errorJson;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.indexer.models.NotificationInterval;
import cz.inovatika.sdnnt.indexer.models.notifications.AbstractNotification;
import cz.inovatika.sdnnt.indexer.models.notifications.NotificationFactory;
import cz.inovatika.sdnnt.indexer.models.notifications.RuleNotification;
import cz.inovatika.sdnnt.indexer.models.notifications.AbstractNotification.TYPE;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.rights.RightsResolver;
import cz.inovatika.sdnnt.rights.impl.predicates.MustBeCalledFromLocalhost;
import cz.inovatika.sdnnt.rights.impl.predicates.MustBeLogged;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.MailService;
import cz.inovatika.sdnnt.services.NotificationsService;
import cz.inovatika.sdnnt.services.UserController;
import cz.inovatika.sdnnt.services.impl.DefaultApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.impl.MailServiceImpl;
import cz.inovatika.sdnnt.services.impl.NotificationServiceImpl;
import cz.inovatika.sdnnt.services.impl.shib.ShibUsersControllerImpl;
import cz.inovatika.sdnnt.services.impl.users.UserControlerImpl;
import cz.inovatika.sdnnt.utils.ServletsSupport;

@WebServlet(value = "/notifications/*")
public class NotificationServlet extends HttpServlet {

    public static final Logger LOGGER = Logger.getLogger(NotificationServlet.class.getName());

    
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        responseHeader(response);
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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.processRequest(req, resp);
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.processRequest(req, resp);
    }



    private void responseHeader(HttpServletResponse response) {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0
        response.setDateHeader("Expires", 0); // Proxies.
    }

    enum Actions {
        
        SAVE_NOTIFICATION_SETTINGS {

            @Override
            JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {
                if (new RightsResolver(request, new MustBeLogged()).permit()) {
                    JSONObject inputJs = ServletsSupport.readInputJSON(request);
                    
                    ApplicationUserLoginSupport login = new DefaultApplicationUserLoginSupport(request);

                    // user controller - 
                    UserController controller = null;
                    if (login.getUser().isThirdPartyUser()) {
                        controller = new ShibUsersControllerImpl();
                    } else {
                        controller = new UserControlerImpl(request);
                    }

                    MailService mailService = new MailServiceImpl();
                    // check third party user
                    if (inputJs.has("notification_interval")) {
                        String notificationInterval = inputJs.getString("notification_interval");
                        controller.changeIntervalForUser(login.getUser().getUsername(), NotificationInterval.valueOf(notificationInterval));
                    }
                        
                    NotificationsService service = new NotificationServiceImpl(controller, mailService);

                    if (inputJs.has("notifications")) {
                        List<RuleNotification> aNotification = new ArrayList<>();
                        inputJs.getJSONArray("notifications").forEach(notif-> {
                            AbstractNotification aNotif = NotificationFactory.fromJSON((JSONObject)notif);
                            if (aNotif.getType().equals(TYPE.rule.name())) {
                                aNotification.add((RuleNotification) aNotif);
                            }
                        });
                        
                        List<RuleNotification> storedRuleNotifications = service.findNotificationsByUser(login.getUser().getUsername(), TYPE.rule).stream().map(notif-> {
                            return (RuleNotification)notif;
                        }).collect(Collectors.toList());
                        if (!storedRuleNotifications.isEmpty()) {
                            service.deleteRuleNotifications(storedRuleNotifications);
                        }
                        for (RuleNotification aNotif : aNotification) {
                            service.saveNotificationRule(aNotif);
                        }
                    }
                    
                    // safra 
                    List<AbstractNotification> simple = service.findNotificationsByUser(login.getUser().getUsername(), TYPE.simple);
                    List<AbstractNotification> rules = service.findNotificationsByUser(login.getUser().getUsername(), TYPE.rule);
                    JSONArray jsonArray = new JSONArray();
                    if (!simple.isEmpty()) {
                        JSONObject fobj = new JSONObject();
                        fobj.put("name", "simple");
                        fobj.put("id", "simple");
                        jsonArray.put(fobj);
                        
                    }
                    
                    rules.stream().map(an-> {
                        RuleNotification rnotif = (RuleNotification) an;
                        JSONObject fobj = new JSONObject();
                        fobj.put("name", rnotif.getName());
                        fobj.put("id", rnotif.getId());
                        return fobj;
                    }).forEach(jsonArray::put);
                    
                    
                    JSONObject ret = new JSONObject();
                    ret.put("all", jsonArray);
                    
                    return ret;
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");

                }
            }
        },
        // get notifications for settings
        GET_RULE_NOTIFICATIONS {

            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse res) throws Exception {
                if (new RightsResolver(req, new MustBeLogged()).permit()) {
                    JSONObject jsonObject = new JSONObject();
                    
                    ApplicationUserLoginSupport login = new DefaultApplicationUserLoginSupport(req);
                    UserController controler = new UserControlerImpl(req);
                    MailService mailService = new MailServiceImpl();

                    NotificationsService service = new NotificationServiceImpl(controler, mailService);
                    List<AbstractNotification> findNotificationsByUser = service.findNotificationsByUser(login.getUser().getUsername(), TYPE.rule);
                    
                    
                    JSONArray jsonArray = new JSONArray();
                    List<JSONObject> collected = findNotificationsByUser.stream().map(AbstractNotification::toJSONObject).collect(Collectors.toList());
                    collected.stream().forEach(jsonArray::put);

                    jsonObject.put("docs",jsonArray);
                    
                    return jsonObject;
                    
                } else {
                    return errorJson(res, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
            
        },

        SAVE_RULE_NOTIFICATION {

            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged()).permit()) {
                    JSONObject inputJs = ServletsSupport.readInputJSON(req);
                    AbstractNotification notification = NotificationFactory.fromJSON(inputJs);
                    if (notification.getType() != null && notification.getType().equals(TYPE.rule.name())) {
                            
                        ApplicationUserLoginSupport login = new DefaultApplicationUserLoginSupport(req);
                        UserController controler = new UserControlerImpl(req);
                        
                        MailService mailService = new MailServiceImpl();
                        notification.setUser(login.getUser().getUsername());
                        
                        NotificationsService service = new NotificationServiceImpl(controler, mailService);
                        RuleNotification saved = service.saveNotificationRule((RuleNotification) notification);

                        
                        List<AbstractNotification> simple = service.findNotificationsByUser(login.getUser().getUsername(), TYPE.simple);
                        List<AbstractNotification> rules = service.findNotificationsByUser(login.getUser().getUsername(), TYPE.rule);
                        JSONArray jsonArray = new JSONArray();
                        if (!simple.isEmpty()) {
                            JSONObject fobj = new JSONObject();
                            fobj.put("name", "simple");
                            fobj.put("id", "simple");
                            jsonArray.put(fobj);
                            
                        }
                        
                        rules.stream().map(an-> {
                            RuleNotification rnotif = (RuleNotification) an;
                            JSONObject fobj = new JSONObject();
                            fobj.put("name", rnotif.getName());
                            fobj.put("id", rnotif.getId());
                            return fobj;
                        }).forEach(jsonArray::put);
                        
                        
                        JSONObject ret = new JSONObject();
                        ret.put("all", jsonArray);

                        return ret;
                        
                    } else {
                        return errorJson(response, HttpServletResponse.SC_BAD_REQUEST, "illegal.type.of.notification", "illegal.type.of.notification");
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
                
            }
        
        },
        
        RULE_NOTIFICATIONS {

            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                JSONObject retval = new JSONObject();
                retval.put("docs", new JSONArray());
                if (new RightsResolver(req, new MustBeLogged()).permit()) {

                    ApplicationUserLoginSupport login = new DefaultApplicationUserLoginSupport(req);
                    UserController controler = new UserControlerImpl(req);
                    MailService mailService = new MailServiceImpl();

                    NotificationsService service = new NotificationServiceImpl(controler, mailService);

                    List<AbstractNotification> notifications = service
                            .findNotificationsByUser(login.getUser().getUsername(), TYPE.rule);
                    List<JSONObject> collect = notifications.stream().map(AbstractNotification::toJSONObject)
                            .collect(Collectors.toList());
                    collect.stream().forEach(retval.getJSONArray("docs")::put);
                }
                return retval;
            }
        };

        abstract JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception;
    }
}
