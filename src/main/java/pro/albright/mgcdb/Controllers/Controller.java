package pro.albright.mgcdb.Controllers;

import org.apache.velocity.app.VelocityEngine;
import pro.albright.mgcdb.Model.User;
import pro.albright.mgcdb.Util.Config;
import spark.ModelAndView;
import spark.Request;
import spark.Session;
import spark.template.velocity.VelocityTemplateEngine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.notFound;

public class Controller {
  private static VelocityTemplateEngine tple = null;

  /**
   * Render a page.
   *
   * Note that a "model" as Spark's template system calls it is not an actual
   * model but just a map of placeholder values.
   */
  protected static String render(Request req, Map<String, Object> model, String template) {
    // Load the user so we can put some user info in the "model."
    User user = (User) req.attribute("user");

    if (user != null) {
      model.put("authenticated", true);
      model.put("userSteamId", user.getSteamId());
      model.put("userSteamNickname", user.getNickname());
      model.put("userSteamAvatar", user.getAvatarUrl());
    }
    else {
      model.put("authenticated", false);
    }

    model.put("baseUrl", Config.get("url"));
    model.put("template", template);

    return getTemplateEngine().render(new ModelAndView(model, "skeleton.vm"));
  }

  /**
   * Show a 404 page.
   */
  protected static void fourOhFour() {
    Map<String, Object> model = new HashMap<>();
    ModelAndView mav = new ModelAndView(model, "404.vm");
    String output = getTemplateEngine().render(mav);
    notFound(output);
  }

  /**
   * Get the Velocity template engine instance, instantiating it first if
   * necessary.
   *
   * @return The VelocityTemplateEngine instance.
   */
  protected static VelocityTemplateEngine getTemplateEngine() {
    if (tple == null) {
      // TODO The path to templates should be changeable via config file
      Path templatePath = Paths.get("templates");
      VelocityEngine veloEngine = new VelocityEngine();
      veloEngine.setProperty(VelocityEngine.FILE_RESOURCE_LOADER_PATH, templatePath.toAbsolutePath().toString());
      veloEngine.init();
      tple = new VelocityTemplateEngine(veloEngine);
    }
    return tple;
  }
}
