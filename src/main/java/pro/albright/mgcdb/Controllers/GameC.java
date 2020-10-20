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
    String pageStr = req.params(":page");
    int page = pageStr == null ? 0 : Integer.parseInt(pageStr) - 1;
    PagedQueryResult<Game> gameResult = Game.getByReleaseDate(page);
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
}
