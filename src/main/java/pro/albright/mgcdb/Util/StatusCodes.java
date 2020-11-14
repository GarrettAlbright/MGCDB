package pro.albright.mgcdb.Util;

public class StatusCodes {
  /* Tasks & routing error codes */
  // Couldn't find a handler for the requested task
  public static final int NO_TASK_HANDLER = 1;
  // A parameter passed to a task was missing or out of bounds
  public static final int BAD_TASK_PARAM = 2;

  /* DB/SQL-related codes */
  // General SQL error
  public static final int GENERAL_SQL_ERROR = 10;
  // Can't open the database file.
  public static final int NO_DB_FILE = 11;

  /* Config related codes */
  // Can't find/open a configuration file.
  public static final int NO_CONFIG_FILE = 20;
  // Error occurred when trying to parse the configuration file.
  public static final int CONFIG_FILE_CANT_BE_PARSED = 21;
  // A required config param is not present in chosen config file.
  public static final int REQUIRED_CONFIG_PARAM_MISSING = 22;

  /* Outgoing network-related codes */
  // General request calamity.
  public static final int GENERAL_OUTGOING_NETWORK_ERROR = 30;
  // Building a URI failed.
  public static final int URI_BUILDING_FAILED = 31;

  /* OpenID issues */
  // General OpenID error
  public static final int GENERAL_OPENID_ERROR = 40;

  // Escaping error
  public static final int GENERAL_ESCAPING_ERROR = 50;
}
