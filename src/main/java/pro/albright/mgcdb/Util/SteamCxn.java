package pro.albright.mgcdb.Util;

import com.fasterxml.jackson.jr.ob.JSON;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import pro.albright.mgcdb.Model.Game;
import pro.albright.mgcdb.SteamAPIModel.*;
import spark.utils.IOUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Class to manage making requests and handling responses with the Steam API.
 */
public class SteamCxn {

  private final String steamKey;

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
    ArrayList<Game> games = new ArrayList<>();
    HashMap<String, String> params = new HashMap<>();
    params.put("last_appid", String.valueOf(lastAppId));
    params.put("max_results", String.valueOf(limit));
    URI uri = buildApiUri("IStoreService", "GetAppList", params);

    GetAppListResponseWrapper response = makeRequestAndReturnBean(uri, GetAppListResponseWrapper.class);

    for (GetAppListApp app : response.getResponse().getApps()) {
      // Double check that we don't already have the game in the DB.
      if (!Game.existsBySteamId(app.getAppid())) {
        Game game = Game.createFromSteamAppBean(app);
        games.add(game);
      }
    }
    return games.toArray(new Game[0]);
  }

  /**
   * Get updated game details from Steam for a game.
   *
   * Note that this doesn't use the Steam API proper but instead a request
   * URI that the Steam front end uses. Thus this is a big fragile and could
   * change formats on Steam's whim.
   *
   * @param game The game to get updated details for.
   * @return Updated details as a GetAppDetailsApp bean.
   */
  public GetAppDetailsApp getUpdatedGameDetails(Game game) {
    String initialString = "https://store.steampowered.com/api/appdetails?appids=" + game.getSteamId();
    URI uri = URI.create(initialString);

    String json = fetchRawJson(uri);

    Map<String, GetAppDetailsResponse> gadrMap = null;

    try {
      gadrMap = JSON.std.mapOfFrom(GetAppDetailsResponse.class, json);
    }
    catch (IOException e) {
      System.err.printf("Error getting Steam API update details for %s (%s).%n", game.getTitle(), game.getSteamId());
      System.exit(StatusCodes.GENERAL_OUTGOING_NETWORK_ERROR);
    }
    Optional<GetAppDetailsResponse> gadrOpt = gadrMap.values().stream().findFirst();
    return gadrOpt.map(GetAppDetailsResponse::getData).orElse(null);
  }

  /**
   * Build a Steam API URL.
   *
   * @param iface The interface the method to call belongs to.
   * @param method The method to call.
   * @param params Additional URL parameters.
   * @return The generated URI.
   */
  private URI buildApiUri(String iface, String method, HashMap<String, String> params) {
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

  /**
   * Wrapper to make a Steam API request and return a single bean.
   * @param uri URI to request to
   * @param beanClass Class of bean to create
   * @param <T> Instance of the bean
   * @return A bean of the given type
   */
  private <T> T makeRequestAndReturnBean(URI uri, Class<T> beanClass) {
    String json = fetchRawJson(uri);

    T responseBean = null;
    try {
      responseBean = JSON.std.beanFrom(beanClass, json);
    }
    catch (IOException e) {
      System.err.print("JSON parsing error");
      System.exit(StatusCodes.GENERAL_OUTGOING_NETWORK_ERROR);
    }
    return responseBean;
  }

  /**
   * Fetch raw JSON (as a String).
   * @param uri The URI from which to get the JSON.
   * @return The JSON as a String.
   */
  private String fetchRawJson(URI uri) {
    int statusCode = -1;
    String json = "";
    try {
      // Ignore cookies
      RequestConfig config = RequestConfig.custom()
        .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
        .build();
      HttpClient client = HttpClients.custom().setDefaultRequestConfig(config).build();
      HttpGet get = new HttpGet(uri);

      HttpResponse response = client.execute(get);
      statusCode = response.getStatusLine().getStatusCode();
      json = IOUtils.toString(response.getEntity().getContent());
    }
    catch (Exception e) {
      System.err.println("Exception while making Steam request: " + e.getMessage());
      System.exit(StatusCodes.GENERAL_OUTGOING_NETWORK_ERROR);
    }
    if (statusCode < 200 || statusCode >= 300) {
      System.err.printf("Unexpected status code %s while making Steam request.", statusCode);
      System.exit(StatusCodes.GENERAL_OUTGOING_NETWORK_ERROR);
    }
    return json;
  }
}
