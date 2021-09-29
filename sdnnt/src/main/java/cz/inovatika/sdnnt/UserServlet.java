/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.indexer.models.Zadost;
import cz.inovatika.sdnnt.rights.RightsResolver;
import cz.inovatika.sdnnt.rights.exceptions.NotAuthorizedException;
import cz.inovatika.sdnnt.rights.impl.predicates.MustBeLogged;
import cz.inovatika.sdnnt.rights.impl.predicates.UserMustBeInRole;
import cz.inovatika.sdnnt.services.UserControler;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerExpiredTokenException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerInvalidPwdTokenException;
import cz.inovatika.sdnnt.services.impl.MailServiceImpl;
import cz.inovatika.sdnnt.services.impl.UserControlerImpl;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static cz.inovatika.sdnnt.rights.Role.admin;
import static cz.inovatika.sdnnt.utils.ServletsSupport.*;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;

/**
 *
 * @author alberto
 */
@WebServlet(name = "UserServlet", urlPatterns = {"/user/*"})
public class UserServlet extends HttpServlet {
  
  public static final Logger LOGGER = Logger.getLogger(UserServlet.class.getName()); 

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

    LOGIN {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        try {
          UserControler controler = new UserControlerImpl(req);
          User login = controler.login();
          if (login != null) {
            JSONObject retVal = login.toJSONObject();
            List<Zadost> zadost = controler.getZadost(login.username);
            if (zadost != null && !zadost.isEmpty()) {
              JSONArray jsonArray = new JSONArray();
              zadost.stream().forEach(z-> {
                jsonArray.put(z.toJSON());
              });
              retVal.put("zadost", jsonArray);
            }

          }
          return login.toJSONObject();
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



    // posle link uzivateli ze si ma vygenerovat heslo
    FORGOT_PWD {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        try {
          String resetPwdToken = new UserControlerImpl(req, new MailServiceImpl()).forgotPwd(readInputJSON(req));
          JSONObject object = new JSONObject();
          object.put("token", resetPwdToken);
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
          User pswd = new UserControlerImpl(req).changePwdUser(readInputJSON(req).optString("pswd"));
          return pswd != null ? pswd.toJSONObject() : new JSONObject();
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
        } catch (IOException | UserControlerException|UserControlerInvalidPwdTokenException|UserControlerExpiredTokenException e) {
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
          retvalue.put("valid",false);
        }
        return retvalue;
      }
    },


    // mail o resetovanem hesle - admin rozhrani
    ADMIN_RESET_PWD {
      @Override 
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole( admin)).permit()) {
          User user = new UserControlerImpl(req, new MailServiceImpl()).resetPwd(readInputJSON(req));
          return user.toJSONObject();
        } else {
          return errorJson(response, SC_FORBIDDEN, "not allowed");
        }
      }
    },


    SAVE {
      @Override 
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {

        if (new RightsResolver(req, new MustBeLogged()).permit()) {
          User sender = new UserControlerImpl(req).getUser();
          JSONObject savingUser = readInputJSON(req);
          if (sender.username.equals(savingUser.optString("username"))) {
            // ok
            return new UserControlerImpl(req).userSave(User.fromJSON(savingUser.toString())).toJSONObject();
          } else {
            // must be admin
            if (new RightsResolver(req, new UserMustBeInRole(admin)).permit()) {
              // must be load first and then
              return new UserControlerImpl(req).userSave(User.fromJSON(savingUser.toString())).toJSONObject();
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
