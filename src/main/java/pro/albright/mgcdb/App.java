package pro.albright.mgcdb;

import static spark.Spark.get;

import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import pro.albright.mgcdb.Model.Game;
import pro.albright.mgcdb.Util.Tasks;

/**
 * Initialize the server or run a task.
 */
public class App {
  public static void main( String[] args ) throws SQLException {
    if (args.length == 0) {
      // Start server
      System.out.println("Starting server. (Not really.)");
      get("/hello", (req, res) -> {
        Properties p = new Properties();
        Path templatePath = Paths.get("templates");
        p.put("file.resource.loader.path", templatePath.toAbsolutePath().toString());
        Velocity.init(p);
        Template t = null;
        VelocityContext context = new VelocityContext();
        context.put("games", Game.getAllAlpha(0));
        StringWriter sw = new StringWriter();
        try {
          t = Velocity.getTemplate("hello-world.vm");
          t.merge(context, sw);
        }
        catch (ResourceNotFoundException e) {
          System.err.printf("Template %s could not be found%n", "hello-world.vm");
        }
        catch (ParseErrorException e) {
          System.err.printf("Parse error for template %s: %s%n", "hello-world.vm", e.getMessage());
        }
        catch (MethodInvocationException e) {
          System.err.printf("MethodInvicationException for template %s: %s%n", "hello-world.vm", e.getMessage());
        }
        catch (Exception e) {
          System.err.printf("Exception for template %s: %s%n", "hello-world.vm", e.getMessage());
        }

        return sw.toString();
      });
    }
    else {
      // Run an administrative task.
      String task = args[0];
      String[] taskArgs = new String[args.length - 1];
      for (int argIdx = 1; argIdx < args.length; argIdx++) {
        taskArgs[argIdx - 1] = args[argIdx];
      }
      Tasks.invoke(task, taskArgs);
    }
  }
}
