package pro.albright.mgcdb.Controllers;

import org.apache.http.HttpStatus;
import org.openid4java.consumer.*;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.ParameterList;
import pro.albright.mgcdb.Util.Config;
import pro.albright.mgcdb.Util.StatusCodes;
import spark.Request;
import spark.Response;
import spark.Session;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    // TODO don't let the user continue if they're already logged in

    try {
      List discoveries = mgr.discover("https://steamcommunity.com/openid");
      DiscoveryInformation discoInfo = mgr.associate(discoveries);
      session.attribute("openid-discoveries", discoInfo);
      String url = Config.get("url");
      AuthRequest authReq = mgr.authenticate(discoveries, url + "/user/authenticated");
      System.out.println(authReq.getDestinationUrl(true));
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
        req.session().attribute("steam-id", steamId);
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
    long steamId = (long) req.session().attribute("steam-id");
    Map<String, Object> model = new HashMap<>();
    model.put("steamId", steamId);
    return render(model, "user.vm");
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
}
