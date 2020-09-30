package pro.albright.mgcdb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.File;

public class DBCXN {

  private static Connection cxn;
  private static String path;
  
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

  public static Connection getCxn() throws SQLException {
    if (cxn == null || !cxn.isValid(5)) {
      initWithPath(path);
    }
    return cxn;
  }

  public static void createIfNotExists() throws SQLException {
    createIfNotExists(false);
  }

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
