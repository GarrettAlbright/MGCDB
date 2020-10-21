package pro.albright.mgcdb.Controllers;

import pro.albright.mgcdb.Model.Game;
import pro.albright.mgcdb.Util.PagedQueryResult;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for game-related routes.
 */
public class GameC extends Controller {

  /**
   * Show games by release date.
   * @param req
   * @param res
   * @return
   */
  public static String gamesByRelease(Request req, Response res)  {
    String filterStr = req.params(":filter");
    // If our filter param is numeric, it's actually a page number for the "all"
    // filter.
    int page = 0;
    String filter = "all";
    try {
      page = Integer.parseInt(filterStr) - 1;
    }
    catch (NumberFormatException e) {
      filter = filterStr;
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

    PagedQueryResult<Game> gameResult = Game.getByReleaseDate(page, filterMode);

    // If the user requested a page outside of the possible range, 404. Using
    // >= here since page is zero-based.
    if (page >= gameResult.getTotalPages()) {
      fourOhFour();
      return null;
    }

    Map<String, Object> model = new HashMap<>();
    model.put("games", gameResult.getResults());
    model.put("totalGames", gameResult.getTotalResults());
    model.put("totalPages", gameResult.getTotalPages());
    model.put("currentPage", gameResult.getCurrentPageZeroBased() + 1);

    return render(model, "gamesByRelease.vm");
  }
}
