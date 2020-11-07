package pro.albright.mgcdb;

import java.sql.SQLException;

import pro.albright.mgcdb.Controllers.GameC;
import pro.albright.mgcdb.Controllers.UserC;
import pro.albright.mgcdb.Model.User;
import pro.albright.mgcdb.Util.Tasks;
import spark.Session;

import static spark.Spark.*;

/**
 * Initialize the server or run a task.
 */
public class App {
  public static void main( String[] args ) throws SQLException {
    if (args.length == 0) {
      // Start server
      System.out.println("Starting server.");

      // Load a User object for later use if the user is authenticated.
      before("*", (req, res) -> {
        // If we have a user ID in the session, load the user
        Session session = req.session(false);
        if (session != null) {
          // session.attribute throws an exception if the attribute can't be
          // found rather than doing something sensible like returning null or
          // zero
          try {
            int userId = session.attribute("user-id");
            User user = User.getById(userId);
            if (user != null) {
              req.attribute("user", user);
            }
          }
          catch (Exception e) {
            // Oh well
          }
        }
      });

      get("/games", GameC::gamesByRelease);
      get("/games/:filter", GameC::gamesByRelease);
      get("/games/:filter/:page", GameC::gamesByRelease);

      // There doesn't seem to be a way to define a handler for just "/foo"
      // inside a path section for "/foo", and the "before" handler for "/*"
      // inside the path section for "/foo" won't fire the handler for just
      // "/foo". So this ugliness.
      before("/auth", UserC::ensureNotAuthenticated);
      get("/auth", UserC::authenticate);
      path("/auth", () -> {
        before("/*", UserC::ensureNotAuthenticated);
        get("/authenticated", UserC::authenticated);
      });

      before("/user", UserC::ensureAuthenticated);
      get("/user", UserC::userPage);
      path("/user", () -> {
        before("/*", UserC::ensureAuthenticated);
        get("/log-out", UserC::logOut);
        get("/games", UserC::userGames);
        get("/games/:page", UserC::userGames);
      });
    }
    else {
      // Run an administrative task.
      String task = args[0];
      String[] taskArgs = new String[args.length - 1];
      for (int argIdx = 1; argIdx < args.length; argIdx++) {
        taskArgs[argIdx - 1] = args[argIdx];
      }
      Tasks.invoke(task, taskArgs);
    }
  }
}
