package pro.albright.mgcdb;

import static spark.Spark.get;
import java.sql.SQLException;

import pro.albright.mgcdb.Util.Tasks;

/**
 * Initialize the server or run a task.
 */
public class App {
  public static void main( String[] args ) throws SQLException {
    if (args.length == 0) {
      // Start server
      System.out.println("Starting server. (Not really.)");
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
