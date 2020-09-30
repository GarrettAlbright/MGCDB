package pro.albright.mgcdb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.File;

public class DBCXN {

  private static Connection cxn;
  private static String path;

  /**
   * Set the path of the DB file (creating it if necessary) and init connection.
   *
   * @param pathParam The path to the database file.
   */
  public static void initWithPath(String pathParam) {
    try {
      // https://stackoverflow.com/a/7163455/11023
      path = pathParam.replaceFirst("^~", System.getProperty("user.home"));
      createIfNotExists();
      initConnection();
    }
    catch (SQLException e) {
      System.err.println("Error when opening database file at " + path + ": " + e.getMessage());
      System.exit(StatusCodes.NO_DB_FILE);
    }
  }

  /**
   * Init the connection.
   */
  public static void initConnection() {
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
   * @throws SQLException
   */
  public static Connection getCxn() throws SQLException {
    if (cxn == null || !cxn.isValid(5)) {
      initWithPath(path);
    }
    return cxn;
  }

  /**
   * Create the database file if it doesn't already exist.
   *
   * @throws SQLException
   */
  public static void createIfNotExists() throws SQLException {
    createIfNotExists(false);
  }

  /**
   * Create the database file if it doesn't exist; optionally delete it first
   * if it does.
   *
   * @param deleteIfExists true if the database file should be deleted.
   * @throws SQLException
   */
  public static void createIfNotExists(boolean deleteIfExists) throws SQLException {
    File dbFile = new File(path);
    if (dbFile.exists() && deleteIfExists) {
      cxn.close();
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
}
