package pro.albright.mgcdb.Util;

import org.tomlj.Toml;
import org.tomlj.TomlParseError;
import org.tomlj.TomlParseResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to manage retrieving configuration parameters.
 */
public class Config {
  private static TomlParseResult config;
  private static String fileAbsPath;

  /**
   * Get a configuration value.
   * @param path The TOML path to the desired value.
   * @return The desired value as a String (other types not supported yet).
   */
  public static String get(String path) {
    if (config == null) {
      init();
    }

    return config.getString(path);
  }

  /**
   * Initialize by finding a config file and loading settings.
   */
  private static void init() {
    // Load settings
    List<String> settingsPaths = new ArrayList<>();

    // Check in /etc first
    settingsPaths.add("/etc/mcgdb/conf.toml");
    // Then ~/.config/mcgdb_conf.toml
    settingsPaths.add(System.getProperty("user.home") + "/.config/mgcdb_conf.toml");

    // Then check for the in-app one
    Path tomlPath = Paths.get("conf.toml");
    String localConfPath = tomlPath.toAbsolutePath().toString();
    settingsPaths.add(localConfPath);

    // TODO Also allow a config file to be specified as an argument

    // Does a valid file actually exist in one of these places?
    boolean confLoaded = false;
    for (String settingsPathStr : settingsPaths) {
      Path settingsPath = Path.of(settingsPathStr);
      String settingsPathAbs = settingsPath.toAbsolutePath().toString();
      File settingsFile = new File(settingsPathAbs);
      if (settingsFile.isFile()) {
        // Looks like we'll use this one.
        System.out.println("Using config file at path " + settingsPathAbs);
        fileAbsPath = settingsPathAbs;
        try {
          config = Toml.parse(settingsFile.toPath());
          List<TomlParseError> errors = config.errors();
          if (errors.size() > 0) {
            System.err.println("Error parsing config file at path" + settingsPathAbs);
            errors.forEach(err -> System.err.println(err.toString()));
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
      System.exit(StatusCodes.NO_CONFIG_FILE);
    }

    // Check for required parameters
    String[] requiredParams = {
      "db_location",
      "steam_key"
    };
    for (String requiredParam : requiredParams) {
      if (config.getString(requiredParam) == null) {
        System.err.printf("Required parameter %s not found in config file at path %s", requiredParam, fileAbsPath);
        System.exit(StatusCodes.REQUIRED_CONFIG_PARAM_MISSING);
      }
    }
  }
}
