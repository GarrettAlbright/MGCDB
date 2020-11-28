package pro.albright.mgcdb.Controller;

import pro.albright.mgcdb.Model.Game;
import pro.albright.mgcdb.Util.Config;
import pro.albright.mgcdb.Util.PagedQueryResult;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller for game-related routes.
 */
public class GameC extends Controller {

  /**
   * A regular expression to extract separate search terms from a search query.
   * Terms may be grouped with double quotes; otherwise we split on space
   * characters. For example, a query such as `foo bar baz` should break into
   * the parts `["foo", "bar", "baz"]` and a query such as `"foo bar" baz`
   * should be `["foo bar", "baz"]`. Non-paired double quotes should be ignored,
   * so `foo "bar baz` is still `["foo", "bar", "baz"]`.
   */
    private Pattern queryPattern = Pattern.compile("(?:\"(.+?)\"|([^\\s\"]+))");

  /**
   * Initialize controller and inject dependencies.
   *
   * @param config
   */
  public GameC(Config config) {
    super(config);
  }

  /**
   * Show games by release date.
   * @param req
   * @param res
   * @return
   */
  public String gamesByRelease(Request req, Response res)  {
    Map<String, Object> model = getModelForGameList(req);
    model.put("isFrontPage", false);
    return render(req, model, "gamesByRelease.vm");
  }

  /**
   * Show games by release date, plus some additional information that should
   * only be on the front page.
   * @param req
   * @param res
   * @return
   */
  public String front(Request req, Response res) {
    Map<String, Object> model = getModelForGameList(req);
    model.put("isFrontPage", true);
    return render(req, model, "gamesByRelease.vm");
  }

  /**
   * Prepare the "model" for the master game list. Spun out into a separate
   * method for easy calling from within both gamesByRelease() and front().
   * @param req
   * @return
   */
  private HashMap<String, Object> getModelForGameList(Request req) {
    String filterStr = req.params(":filter");
    // If our filter param is numeric, it's actually a page number for the "all"
    // filter.
    int page = 0;
    String filter = "all";
    try {
      page = Integer.parseInt(filterStr) - 1;
    }
    catch (NumberFormatException e) {
      filter = filterStr == null ? "all" : filterStr;
      String pageStr = req.params(":page");
      if (pageStr != null) {
        try {
          page = Integer.parseInt(pageStr);
        }
        catch (NumberFormatException e2) {
          // Saw a second parameter which wasn't a page number.
          fourOhFour();
          return null;
        }
      }
    }

    // Throw out possible invalid values before continuing
    if (page < 0) {
      fourOhFour();
      return null;
    }

    Game.GameFilterMode filterMode = Game.GameFilterMode.ALL;
    if (filter.equals("mac")) {
      filterMode = Game.GameFilterMode.MAC;
    }
    else if (filter.equals("cat")) {
      filterMode = Game.GameFilterMode.CATALINA;
    }
    else if (!filter.equals("all")) {
      // Invalid property passed as the filter value
      fourOhFour();
      return null;
    }

    // Get the query parts if present
    ArrayList<String> queryParts = new ArrayList<>();
    String query = req.queryParams("query");
    if (query != null && !query.isEmpty()) {
      Matcher m = queryPattern.matcher(query);
      while (m.find()) {
        queryParts.add(m.group());
      }
    }

    PagedQueryResult<Game> gameResult = null;
    if (queryParts.size() > 0) {
      String[] queryPartsArray = queryParts.toArray(new String[queryParts.size()]);
      gameResult = Game.getByReleaseDate(0, filterMode, queryPartsArray);
    }
    else {
      gameResult = Game.getByReleaseDate(page, filterMode);
    }

    // If the user requested a page outside of the possible range, 404. Using
    // >= here since page is zero-based.
    if (page != 0 && page >= gameResult.getTotalPages()) {
      fourOhFour();
      return null;
    }

    HashMap<String, Object> model = new HashMap<>();
    model.put("games", gameResult);
    model.put("filter", filter);
    model.put("query", query);

    return model;
  }
}
