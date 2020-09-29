package pro.albright.mgcdb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBCXN {

  private static Connection cxn;
  
  public static void initWithPath(String path) {
    try {
      cxn = DriverManager.getConnection("jdbc:sqlite:" + path);
    }
    catch (SQLException e) {
      System.err.println("Error when opening database file at " + path + ": " + e.getMessage());
      // TODO Non-zero exit codes
      System.exit(0);
    }
  }
  
}
