package pro.albright.mgcdb;

import static spark.Spark.get;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlParseError;
import pro.albright.mgcdb.Util.DBCXN;
import pro.albright.mgcdb.Util.StatusCodes;
import pro.albright.mgcdb.Util.Tasks;

/**
 * Initialize the server or run a task.
 */
public class App {
  public static void main( String[] args ) throws SQLException {
    // Load settings
    List<String> settingsPaths = new ArrayList<>();

    // Check in /etc first
    settingsPaths.add("/etc/mcgdb/conf.toml");

    // Then check for the in-app one
    Path tomlPath = Paths.get("conf.toml");
    String localConfPath = tomlPath.toAbsolutePath().toString();
    settingsPaths.add(localConfPath);

    // TODO Also allow a config file to be specified as an argument

    // Does a valid file actually exist in one of these places?
    boolean confLoaded = false;
    TomlParseResult config = null;
    for (String settingsPathStr : settingsPaths) {
      Path settingsPath = Path.of(settingsPathStr);
      String settingsPathAbs = settingsPath.toAbsolutePath().toString();
      File settingsFile = new File(settingsPathAbs);
      if (settingsFile.isFile()) {
        System.out.println("Using config file at path " + settingsPathAbs);
        try {
          config = Toml.parse(settingsFile.toPath());
          List<TomlParseError> errors = config.errors();
          if (errors.size() > 0) {
            System.err.println("Error parsing config file at path" + settingsPathAbs);
            errors.forEach(err -> System.err.println(err.toString()));
            // TODO non-zero exit codes
            System.exit(StatusCodes.CONFIG_FILE_CANT_BE_PARSED);
          }
          else {
            confLoaded = true;
            break;
          }
        }
        catch (IOException e) {
          System.err.println("Exception parsing config file at path " + settingsPathAbs);
          System.exit(StatusCodes.CONFIG_FILE_CANT_BE_PARSED);
        }
      }
    }

    if (!confLoaded) {
      System.err.println("Could not find config file. Giving up.");
      // TODO non-zero exit codes
      System.exit(StatusCodes.NO_CONFIG_FILE);
    }

    DBCXN.initWithPath(config.getString("db_location"));

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
