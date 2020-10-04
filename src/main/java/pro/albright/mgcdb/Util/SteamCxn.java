package pro.albright.mgcdb.Util;

import com.fasterxml.jackson.jr.ob.JSON;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import pro.albright.mgcdb.Model.Game;
import pro.albright.mgcdb.SteamAPIModel.GetAppListResponseWrapper;
import pro.albright.mgcdb.SteamAPIModel.SteamApp;
import spark.utils.IOUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

public class SteamCxn {

  private String steamKey;

  public SteamCxn() {
    steamKey = Config.get("steam_key");
  }

  /**
   * Get new games from the Steam API.
   *
   * @param lastAppId The Steam ID of the newest game in the DB.
   * @param limit Max number of new games to fetch.
   *
   * https://steamapi.xpaw.me/#IStoreService/GetAppList
   */
  public Game[] getNewGames(int lastAppId, int limit) {
    ArrayList<Game> games = new ArrayList<Game>();
    HashMap<String, String> params = new HashMap<>();
    params.put("last_appid", String.valueOf(lastAppId));
    params.put("max_results", String.valueOf(limit));
    URI uri = buildUri("IStoreService", "GetAppList", params);
    HttpResponse response;
    int statusCode = -1;
    String json = "";
    try {
      response = Request.Get(uri).execute().returnResponse();
      statusCode = response.getStatusLine().getStatusCode();
      json = IOUtils.toString(response.getEntity().getContent());
    }
    catch (Exception e) {
      System.err.printf("Exception while fetching game: " + e.getMessage());
      System.exit(StatusCodes.GENERAL_OUTGOING_NETWORK_ERROR);
    }
    if (statusCode < 200 || statusCode >= 300) {
      System.err.printf("Unexpected status code %s while fetching new games.", statusCode);
      return new Game[] {};
    }

    GetAppListResponseWrapper responseBean;
    try {
      responseBean = JSON.std.beanFrom(GetAppListResponseWrapper.class, json);
    }
    catch (IOException e) {
      System.err.print("JSON parsing error");
      return new Game[] {};
    }

    for (SteamApp app : responseBean.getResponse().getApps()) {
      // Double check that we don't already have the game in the DB.
      if (!Game.existsBySteamId(app.getAppid())) {
        Game game = Game.createFromSteamAppBean(app);
        games.add(game);
      }
    }
    return games.toArray(new Game[games.size()]);
  }

  /**
   * Build a Steam API URL.
   *
   * @param iface The interface the method to call belongs to.
   * @param method The method to call.
   * @param params Additional URL parameters.
   * @return The generated URI.
   */
  private URI buildUri(String iface, String method, HashMap<String, String> params) {
    String initialString = "https://api.steampowered.com/" +
      iface + "/" +
      method + "/" +
      "v1?key=" + steamKey;
    URI built = null;
    try {
      URIBuilder ub = new URIBuilder(initialString);
      params.forEach((k, v) -> ub.addParameter(k, v));
      built = ub.build();
    }
    catch (URISyntaxException e) {
      System.err.printf("Exception when building URI: %s, reason: %s", e.getMessage(), e.getReason());
      System.exit(StatusCodes.URI_BUILDING_FAILED);
    }
    return built;
  }
}
