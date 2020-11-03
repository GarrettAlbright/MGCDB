package pro.albright.mgcdb.Controllers;

import org.apache.http.HttpStatus;
import org.openid4java.consumer.*;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.ParameterList;
import pro.albright.mgcdb.Model.User;
import pro.albright.mgcdb.Util.Config;
import pro.albright.mgcdb.Util.StatusCodes;
import spark.Request;
import spark.Response;
import spark.Session;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.halt;

/**
 * Controller for user-related routes.
 */
public class UserC extends Controller {

  /**
   * ConsuerManager for OpenID authentication
   */
  private static ConsumerManager consumerMgr = null;

  /**
   * Redirect the user to Steam to log in.
   *
   * @param req
   * @param res
   * @return
   */
  public static String authenticate(Request req, Response res) {
    ConsumerManager mgr = getConsumerMgr();
    Session session = req.session();
    try {
      List discoveries = mgr.discover("https://steamcommunity.com/openid");
      DiscoveryInformation discoInfo = mgr.associate(discoveries);
      session.attribute("openid-discoveries", discoInfo);
      String url = Config.get("url");
      AuthRequest authReq = mgr.authenticate(discoveries, url + "/auth/authenticated");
      res.redirect(authReq.getDestinationUrl(true), HttpStatus.SC_TEMPORARY_REDIRECT);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(StatusCodes.GENERAL_OPENID_ERROR);
    }
    return null;
  }

  /**
   * Validate the OpenID log in and start the user's session.
   * @param req
   * @param res
   * @return
   */
  public static String authenticated(Request req, Response res) {
    ParameterList params = new ParameterList(req.raw().getParameterMap());
    DiscoveryInformation discoInfo = (DiscoveryInformation) req.session().attribute("openid-discoveries");
    ConsumerManager mgr = getConsumerMgr();
    try {
      String url = Config.get("url");
      String verifyUrl = url + req.raw().getPathInfo();
      VerificationResult verResult = mgr.verify(verifyUrl, params, discoInfo);
      Identifier id = verResult.getVerifiedId();
      if (id != null) {
        req.session().removeAttribute("openid-discoveries");
        // The ID will be a URL like
        // https://steamcommunity.com/openid/id/76561198024933199
        // Strip off the numbers at the end.
        String[] slashParts = id.toString().split("/");
        long steamId = Long.parseLong(slashParts[slashParts.length - 1]);
        User user = User.authWithSteamId(steamId, true);

        req.session().attribute("user-id", user.getUserId());
        res.redirect(url + "/user", HttpStatus.SC_TEMPORARY_REDIRECT);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(StatusCodes.GENERAL_OPENID_ERROR);
    }
    return null;
  }

  /**
   * Show general info about the current user.
   * @param req
   * @param res
   * @return
   */
  public static String userPage(Request req, Response res) {
    Map<String, Object> model = new HashMap<>();
    return render(req, model, "user.vm");
  }

  /**
   * Log the user out and redirect them to the front page.
   * @param req
   * @param res
   * @return
   */
  public static String logOut(Request req, Response res) {
    Session session = req.session(false);
    session.invalidate();
    res.redirect("/?loggedOut=1", HttpStatus.SC_TEMPORARY_REDIRECT);
    return("");
  }

  /**
   * Get the ConsumerManager instance, instantiating it first if necessary.
   * @return
   */
  private static ConsumerManager getConsumerMgr() {
    if (consumerMgr == null) {
      consumerMgr = new ConsumerManager();
      consumerMgr.setAssociations(new InMemoryConsumerAssociationStore());
      consumerMgr.setNonceVerifier(new InMemoryNonceVerifier());
    }
    return consumerMgr;
  }

  /**
   * "Before" handler to ensure the user isn't authenticated. Redirects the user
   * to "/user" if they are already authenticated.
   * @param req
   * @param res
   */
  public static void ensureNotAuthenticated(Request req, Response res) {
   User user = req.attribute("user");
   if (user != null) {
     // The user is already authenticated. Don't try to authenticate them again.
     // Redirect to the user page.
     res.redirect("/user?alreadyAuthenticated=1", HttpStatus.SC_TEMPORARY_REDIRECT);
     halt();
   }
  }

  /**
   * "Before" handler to ensure the user is authenticated. Redirects the user to
   * "/auth" to start the authentication process if they are not.
   *
   * TODO A way to redirect the user to the page they were trying to get to
   * after they successfully authenticate.
   *
   * @param req
   * @param res
   */
  public static void ensureAuthenticated(Request req, Response res) {
    User user = req.attribute("user");
    if (user == null) {
      res.redirect("/auth", HttpStatus.SC_TEMPORARY_REDIRECT);
      halt();
    }
  }
}
