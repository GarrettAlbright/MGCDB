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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;

/**
 * Class to manage making requests and handling responses with the Steam API.
 */
public class SteamCxn {

  /**
   * Directory to log all responses, for debugging purposes. If null, no
   * logging will be done.
   */
  private final String logDir = null;

  private final String steamKey;
  // Identify as a Mac so that Steam tells us if a game is Catalina-compatible
  private final String userAgentString = "MGCDB/0.1 (Macintosh; +https://github.com/GarrettAlbright/MGCDB)";

  public SteamCxn(String steamKey) {
    this.steamKey = steamKey;
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
    URI uri = buildApiUri("IStoreService", "GetAppList", "v1", params);

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

    String json = fetchRawResponseBody(uri);

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
   * Get the Catalina status for a game.
   *
   * Unfortunately this is not exposed via the API, so we resort to screen
   * scraping. Barf.
   */
  public Game.GamePropStatus getCatalinaStatus(Game game) {
    URI uri = URI.create("https://store.steampowered.com/app/" + game.getSteamId());
    String response = fetchRawResponseBody(uri);
    // We'll look for the ID of Steam's KB article about Catalina. It's
    // likely that that link will be there in the future even if there are
    // HTML changes in the future.
    return response.contains("1055-ISJM-8568") ? Game.GamePropStatus.NO : Game.GamePropStatus.YES;
  }

  public PlayerSummary getUserInfo(long steamId) {
    Map<String, String> params = new HashMap<>();
    params.put("steamids", String.valueOf(steamId));
    URI uri = buildApiUri("ISteamUser", "GetPlayerSummaries", "v0002", params);

    GetPlayerSummariesResponseWrapper gpsrw = makeRequestAndReturnBean(uri, GetPlayerSummariesResponseWrapper.class);

    for (PlayerSummary ps : gpsrw.getResponse().getPlayers()) {
      if (ps.getSteamid() == steamId) {
        return ps;
      }
    }

    return null;
  }

  /**
   * Get a list of games (by Steam IDs) that a user owns according to Steam.
   *
   * @param steamUserId
   * @return A list of Steam game IDs.
   */
  public int[] getOwnedGamesInSteam(long steamUserId) {
    Map<String, String> params = new HashMap<>();
    params.put("steamid", String.valueOf(steamUserId));
    params.put("include_played_free_games", "1");
    URI uri = buildApiUri("IPlayerService", "GetOwnedGames", "v1", params);

    GetOwnedGamesResponseWrapper gogrw = makeRequestAndReturnBean(uri, GetOwnedGamesResponseWrapper.class);
    GetOwnedGamesResponse gogr = gogrw.getResponse();

    int[] ownedGamesSteamIds = new int[gogr.getGame_count()];
    int index = 0;
    for (OwnedGameInstance ogi : gogr.getGames()) {
      ownedGamesSteamIds[index++] = ogi.getAppid();
    }
    return ownedGamesSteamIds;
  }

  /**
   * Build a Steam API URL.
   *
   * @param iface The interface the method to call belongs to.
   * @param method The method to call.
   * @param params Additional URL parameters.
   * @return The generated URI.
   */
  private URI buildApiUri(String iface, String method, String vNum, Map<String, String> params) {
    String initialString = "https://api.steampowered.com/" +
      iface + "/" +
      method + "/" +
      vNum +
      "?key=" + steamKey;
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
  protected <T> T makeRequestAndReturnBean(URI uri, Class<T> beanClass) {
    String json = fetchRawResponseBody(uri);

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
   * Fetch raw response body as a string.
   * @param uri The URI to make the request to.
   * @return The JSON as a String.
   */
  protected String fetchRawResponseBody(URI uri) {
    int statusCode = -1;
    String json = "";
    try {
      // Ignore cookies. This causes problems.
      RequestConfig config = RequestConfig.custom()
        .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
        .build();
      HttpClient client = HttpClients.custom().setDefaultRequestConfig(config).build();
      HttpGet get = new HttpGet(uri);
      get.setHeader("Accept-Language", "en-US");
      get.setHeader("User-Agent", userAgentString);

      HttpResponse response = client.execute(get);
      statusCode = response.getStatusLine().getStatusCode();
      json = IOUtils.toString(response.getEntity().getContent());
    }
    catch (Exception e) {
      System.err.println("Exception while making Steam request: " + e.getMessage());
      System.exit(StatusCodes.GENERAL_OUTGOING_NETWORK_ERROR);
    }
    if (statusCode < 200 || statusCode >= 300) {
      System.err.printf("Unexpected status code %s while making Steam request.%n", statusCode);
      System.exit(StatusCodes.GENERAL_OUTGOING_NETWORK_ERROR);
    }

    if (logDir != null) {
      // Log this response for later testing
      Path logDirPath = Path.of(logDir).toAbsolutePath();
      File logDirFile = logDirPath.toFile();
      if (logDirFile.isDirectory() && logDirFile.canWrite()) {
        // Format of filename is the URL (with path-invalid characters replaced)
        String filename = uriToLogFilename(uri);
        File output = new File(logDirFile, filename);
        try {
          FileWriter fw = new FileWriter(output);
          fw.write(json);
          fw.close();
        }
        catch (IOException e) {
          System.err.printf("Attempt to log output of request to %s failed as output file could not be written.%n", uri.toASCIIString());
        }
      }
      else {
        System.err.printf("Attempt to log output of request to %s failed as output directory %s is not writable.%n", uri.toASCIIString(), logDir);
      }
    }

    return json;
  }

  protected String uriToLogFilename(URI uri) {
    return uri.toASCIIString().replace("/", "-").replace(":", "-") + ".txt";
  }
}
