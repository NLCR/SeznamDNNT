/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cz.inovatika.sdnnt.services.impl.MailServiceImpl;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

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
        return UserController.login(req);
      }
    },
    LOGOUT {
      @Override 
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        return UserController.logout(req);
      }
    },
    // posle link uzivateli ze si ma vygenerovat heslo
    FORGOT_PWD {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        String inputJs;
        if (req.getMethod().equals("POST")) {
          inputJs = IOUtils.toString(req.getInputStream(), "UTF-8");
        } else {
          inputJs = req.getParameter("json");
        }
        return UserController.forgotPwd(new MailServiceImpl(),req, inputJs);
      }
    },
    // zruseni tokenu a generovani noveho hesla, poslani na
    ACTIVATE_PWD_TOKEN {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        String inputJs;
        if (req.getMethod().equals("POST")) {
          inputJs = IOUtils.toString(req.getInputStream(), "UTF-8");
        } else {
          inputJs = req.getParameter("json");
        }
        return UserController.checkResetPwdLink(new MailServiceImpl(),req, inputJs);
      }
    },

    // mail o resetovanem hesle - admin rozhrani
    RESET_PWD {
      @Override 
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        String inputJs;
        if (req.getMethod().equals("POST")) {
          inputJs = IOUtils.toString(req.getInputStream(), "UTF-8");
        } else {
          inputJs = req.getParameter("json");
        }
        return UserController.resetPwd(new MailServiceImpl(),req, inputJs);
      }
    },
    SAVE {
      @Override 
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        String inputJs;
          if (req.getMethod().equals("POST")) {
            inputJs = IOUtils.toString(req.getInputStream(), "UTF-8");
          } else {
            inputJs = req.getParameter("json");
          }
        return UserController.save(inputJs);
      }
    },
    // registrace noveho uzivatele
    REGISTER {
      @Override 
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        // TODO: MailService ?? Create? Inject ?
        String inputJs;
          if (req.getMethod().equals("POST")) {
            inputJs = IOUtils.toString(req.getInputStream(), "UTF-8");
          } else {
            inputJs = req.getParameter("json");
          }

        return UserController.register(new MailServiceImpl(), inputJs);
      }
    },
    ALL {
      @Override 
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        
        return UserController.getAll(req);
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
