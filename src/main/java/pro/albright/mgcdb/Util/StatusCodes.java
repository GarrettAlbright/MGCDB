package pro.albright.mgcdb.Util;

public class StatusCodes {
  // Can't find/open a configuration file.
  public static final int NO_CONFIG_FILE = 1;
  // Error occurred when trying to parse the configuration file.
  public static final int CONFIG_FILE_CANT_BE_PARSED = 2;
  // Can't open the database file.
  public static final int NO_DB_FILE = 3;
  // No handler for a task.
  public static final int NO_TASK_HANDLER = 4;
  // A required config param is not present in chosen config file.
  public static final int REQUIRED_CONFIG_PARAM_MISSING = 5;
}
