package com.google.opensesame.auth;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/auth")
public class AuthServlet extends HttpServlet {

  public static final String AUTH_URL_REDIRECT = "/";

  public Gson gson = new Gson();

  /**
   * Gets the currently authorized user. Returns null if no user is currently authorized.
   * @return Returns the currently authorized user or null if there is no user currently authorized.
   */
  public static User getAuthorizedUser() {
    UserService userService = UserServiceFactory.getUserService();
    if (!userService.isUserLoggedIn()) {
      return null;
    }

    return userService.getCurrentUser();
  }

  /**
   * Responds with the URLs for auth login and logout, along with information about the currently
   * authorized user if a user is logged in.
   * @param request
   * @param response
   * @throws IOException
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {    
    JsonObject responseObject = new JsonObject();

    UserService userService = UserServiceFactory.getUserService();
    String loginUrl = userService.createLoginURL(AUTH_URL_REDIRECT);
    String logoutUrl = userService.createLogoutURL(AUTH_URL_REDIRECT);

    responseObject.addProperty("loginUrl", loginUrl);
    responseObject.addProperty("logoutUrl", logoutUrl);

    if (userService.isUserLoggedIn()) {
      responseObject.addProperty("authorized", true);

      // User is logged in; add user data to the response 
      JsonObject userData = new JsonObject();
      User currentUser = userService.getCurrentUser();
      userData.addProperty("email", currentUser.getEmail());
      userData.addProperty("id", currentUser.getUserId());

      responseObject.add("user", userData);
    } else {
      responseObject.addProperty("authorized", false);
    }

    String responseJson = gson.toJson(responseObject);
    
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json;");
    response.getWriter().println(responseJson);
  }
}