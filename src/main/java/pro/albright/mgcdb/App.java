package pro.albright.mgcdb;

import static spark.Spark.get;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlParseError;

/**
 * Hello world!
 *
 */
public class App {
  public static void main( String[] args ) {
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
            System.exit(0);
          }
          else {
            confLoaded = true;
            break;
          }
        }
        catch (IOException e) {
          System.err.println("Exception parsing config file at path " + settingsPathAbs);
        }
      }
    }

    if (!confLoaded) {
      System.err.println("Could not find config file. Giving up.");
      // TODO non-zero exit codes
      System.exit(0);
    }

    if (args.length == 0) {
      // Start server
      System.out.println("Starting server. (Not really.)");
    }
    else if (args.length == 1) {
      switch (args[0]) {
//        case "taskName":
//          // ...
//          break;
        default:
          System.out.println("Unknown command: " + args[0]);
      }
    }
  }
}
