package pro.albright.mgcdb.Controllers;

import org.openid4java.association.AssociationException;
import org.openid4java.consumer.*;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageException;
import org.openid4java.message.ParameterList;
import pro.albright.mgcdb.Util.Config;
import spark.Request;
import spark.Response;
import spark.Session;

import java.util.List;

public class UserC extends Controller {
  private static ConsumerManager consumerMgr = null;

  public static String authenticate(Request req, Response res) {
    ConsumerManager mgr = getConsumerMgr();
    Session session = req.session();

    try {
      List discoveries = mgr.discover("https://steamcommunity.com/openid");
      DiscoveryInformation discoInfo = mgr.associate(discoveries);
      session.attribute("openid-discoveries", discoInfo);
      String url = Config.get("url");
      AuthRequest authReq = mgr.authenticate(discoveries, url + "/user/authenticated");
      System.out.println(authReq.getDestinationUrl(true));
    }
    catch (DiscoveryException e) {
      e.printStackTrace();
    }
    catch (ConsumerException e) {
      e.printStackTrace();
    }
    catch (MessageException e) {
      e.printStackTrace();
    }
    return "";
  }

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
        AuthSuccess authSuccess = (AuthSuccess) verResult.getAuthResponse();
        System.out.printf("Success! ID is " + authSuccess.getIdentity());
      }
    }
    catch (MessageException e) {
      e.printStackTrace();
    }
    catch (DiscoveryException e) {
      e.printStackTrace();
    }
    catch (AssociationException e) {
      e.printStackTrace();
    }
    return "Success?";
  }


  private static ConsumerManager getConsumerMgr() {
    if (consumerMgr == null) {
      consumerMgr = new ConsumerManager();
      consumerMgr.setAssociations(new InMemoryConsumerAssociationStore());
      consumerMgr.setNonceVerifier(new InMemoryNonceVerifier());
    }
    return consumerMgr;
  }
}
