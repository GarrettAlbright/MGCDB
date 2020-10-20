package pro.albright.mgcdb.Controllers;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import pro.albright.mgcdb.Model.Game;
import pro.albright.mgcdb.Util.PagedQueryResult;
import spark.Request;
import spark.Response;

import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static spark.Spark.notFound;

/**
 * Controller for game-related routes.
 */
public class GameC {

  /**
   * Show games by release date.
   * @param req
   * @param res
   * @return
   */
  public static Object gamesByRelease(Request req, Response res)  {
    Properties p = new Properties();
    Path templatePath = Paths.get("templates");
    p.put("file.resource.loader.path", templatePath.toAbsolutePath().toString());
    Velocity.init(p);
    Template t = null;
    VelocityContext context = new VelocityContext();
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

    context.put("games", gameResult.getResults());
    context.put("totalGames", gameResult.getTotalResults());
    context.put("totalPages", gameResult.getTotalPages());
    context.put("currentPage", gameResult.getCurrentPageZeroBased() + 1);
    context.put("template", "gamesByRelease.vm");

    StringWriter sw = new StringWriter();
    try {
      t = Velocity.getTemplate("skeleton.vm");
      t.merge(context, sw);
    }
    catch (ResourceNotFoundException e) {
      System.err.printf("Template %s could not be found%n", "hello-world.vm");
    }
    catch (ParseErrorException e) {
      System.err.printf("Parse error for template %s: %s%n", "hello-world.vm", e.getMessage());
    }
    catch (MethodInvocationException e) {
      System.err.printf("MethodInvocationException for template %s: %s%n", "hello-world.vm", e.getMessage());
    }
    catch (Exception e) {
      System.err.printf("Exception for template %s: %s%n", "hello-world.vm", e.getMessage());
    }

    return sw.toString();
  }

  /**
   * Show a 404 page.
   *
   * Put this in a parent class once I have more than one controller
   */
  private static void fourOhFour() {
    StringWriter sw = new StringWriter();
    Template t = null;
    VelocityContext vc = new VelocityContext();
    try {
      t = Velocity.getTemplate("404.vm");
      t.merge(vc, sw);
      notFound(sw.toString());
    }
    catch (Exception e) {
      notFound("<html><body><h1>404 Not Found</h1><p>Also there was a failure loading the normal 404 template. Oof.</p></body></html>");
    }
  }
}
