package pro.albright.mgcdb.Util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DBCXN {

  private static Connection cxn;
  private static String path;

  /**
   * Init the connection.
   */
  public static void init() {
    ensurePath();
    try {
      cxn = DriverManager.getConnection("jdbc:sqlite:" + path);
      cxn.setAutoCommit(false);
    }
    catch (SQLException e) {
      System.err.println("Error when opening database file at " + path + ": " + e.getMessage());
      System.exit(StatusCodes.NO_DB_FILE);
    }
  }

  /**
   * Return the connection.
   *
   * @return The database connection.
   */
  public static Connection getCxn() {
    try {
      if (cxn == null || !cxn.isValid(5)) {
        init();
      }
    }
    catch (SQLException e) {
      System.err.println("Error when validating connection to file at " + path + ": " + e.getMessage());
      System.exit(StatusCodes.NO_DB_FILE);
    }
    return cxn;
  }

  /**
   * Create the database file if it doesn't exist; optionally delete it first
   * if it does.
   *
   * @param deleteIfExists true if the database file should be deleted.
   * @throws SQLException If a SQL-related error occurred.
   */
  public static void createIfNotExists(boolean deleteIfExists) throws SQLException {
    ensurePath();
    File dbFile = new File(path);
    if (dbFile.exists() && deleteIfExists) {
      if (cxn != null && !cxn.isClosed()) {
        cxn.close();
      }
      dbFile.delete();
    }
    if (!dbFile.exists()) {
      try {
        dbFile.createNewFile();
      }
      catch (IOException e) {
        System.err.println("Error when creating database file at " + path + ": " + e.getMessage());
        System.exit(StatusCodes.NO_DB_FILE);
      }
    }
  }

  /**
   * Ensure the path field is set.
   */
  public static void ensurePath() {
    if (path == null) {
      // https://stackoverflow.com/a/7163455/11023
      String untildedPath = Config.get("db_location");
      path = untildedPath.replaceFirst("^~", System.getProperty("user.home"));
    }
  }

  /**
   * Parse a timestamp.
   *
   * Resultset's .getDate() method doesn't work, apparently because SQLite
   * doesn't have real date/datetime field types.
   *
   * @param timestamp The timestamp.
   * @return A Date object.
   */
  public static Date parseTimestamp(String timestamp) {
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date date = null;
    try {
      date = format.parse(timestamp);
    }
    catch (ParseException e) {
      System.err.printf("Error parsing timestamp %s%n", timestamp);
    }
    return date;
  }
}
