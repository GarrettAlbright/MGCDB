package pro.albright.mgcdb;

import java.sql.SQLException;

import pro.albright.mgcdb.Controller.GameC;
import pro.albright.mgcdb.Controller.UserC;
import pro.albright.mgcdb.Model.Model;
import pro.albright.mgcdb.Model.User;
import pro.albright.mgcdb.Util.Config;
import pro.albright.mgcdb.Util.DBCxn;
import pro.albright.mgcdb.Util.SteamCxn;
import pro.albright.mgcdb.Util.Tasks;
import spark.Session;

import static spark.Spark.*;

/**
 * Initialize the server or run a task.
 */
public class App {
  public static void main( String[] args ) throws SQLException {
    // Initialize dependencies
    Config config = new Config();
    SteamCxn steamCxn = new SteamCxn(config.get("steam_key"));
    DBCxn dbCxn = new DBCxn(config.get("db_location"));

    Model.setDbCxn(dbCxn);
    Model.setSteamCxn(steamCxn);

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

      // Initialize controllers
      GameC gameC = new GameC(config);
      UserC userC = new UserC(config);

      get("/", gameC::front);
      get("/games", gameC::gamesByRelease);
      get("/games/:filter", gameC::gamesByRelease);
      get("/games/:filter/:page", gameC::gamesByRelease);

      // There doesn't seem to be a way to define a handler for just "/foo"
      // inside a path section for "/foo", and the "before" handler for "/*"
      // inside the path section for "/foo" won't fire the handler for just
      // "/foo". So this ugliness.
      before("/auth", userC::ensureNotAuthenticated);
      get("/auth", userC::authenticate);
      path("/auth", () -> {
        before("/*", userC::ensureNotAuthenticated);
        get("/authenticated", userC::authenticated);
      });

      before("/user", userC::ensureAuthenticated);
      get("/user", userC::userPage);
      path("/user", () -> {
        before("/*", userC::ensureAuthenticated);
        get("/log-out", userC::logOut);
        get("/games", userC::userGames);
        get("/games/:page", userC::userGames);
        get("/vote/:ownership/:vote", userC::takeVote);
      });
    }
    else {
      // Run an administrative task.
      String task = args[0];
      String[] taskArgs = new String[args.length - 1];
      for (int argIdx = 1; argIdx < args.length; argIdx++) {
        taskArgs[argIdx - 1] = args[argIdx];
      }
      Tasks tasks = new Tasks(config, new DBCxn(config.get("db_location")));
      tasks.invoke(task, taskArgs);
    }
  }
}
