package pro.albright.mgcdb.Controllers;

import org.apache.velocity.app.VelocityEngine;
import spark.ModelAndView;
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
   *
   * TODO Put this in a parent class once I have more than one controller
   */
  protected static String render(Map<String, Object> model, String template) {
    model.put("template", template);
    return getTemplateEngine().render(new ModelAndView(model, "skeleton.vm"));
  }

  /**
   * Show a 404 page.
   *
   * TODO Put this in a parent class once I have more than one controller
   */
  protected static void fourOhFour() {
    Map<String, Object> model = new HashMap<>();
    model.put("foo", "bar");
    ModelAndView mav = new ModelAndView(model, "404.vm");
    String output = getTemplateEngine().render(mav);
    notFound(output);
  }

  /**
   * Get the Velocity tempalte engine instance, instantiating it first if
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
  }}
