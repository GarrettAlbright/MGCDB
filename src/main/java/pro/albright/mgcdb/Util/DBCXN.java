package pro.albright.mgcdb.Util;

import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.sql.*;
import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class DBCXN {

  private static Connection cxn;
  private static String path;
  private static Connection readOnlyCxn;

  /**
   * Init the connection.
   */
  protected static void init() {
    ensurePath();
    try {
      SQLiteConfig config = new SQLiteConfig();
      // Setting this journal mode allows us to have both a writable and a
      // read-only connection at the same time without encountering locking
      // issues
      // https://www.sqlite.org/wal.html
      // 
      config.setJournalMode(SQLiteConfig.JournalMode.WAL);
      cxn = config.createConnection("jdbc:sqlite:" + path);
      cxn.setAutoCommit(false);
    }
    catch (SQLException e) {
      System.err.println("Error when opening database file at " + path + ": " + e.getMessage());
      System.exit(StatusCodes.NO_DB_FILE);
    }
  }

  protected static void initReadOnlyCxn() {
    ensurePath();
    try {
      if (readOnlyCxn != null && !readOnlyCxn.isClosed()) {
        readOnlyCxn.close();
      }
      SQLiteConfig config = new SQLiteConfig();
      config.setReadOnly(true);
      config.setJournalMode(SQLiteConfig.JournalMode.WAL);
      readOnlyCxn = config.createConnection("jdbc:sqlite:" + path);
    }
    catch (SQLException throwables) {
      throwables.printStackTrace();
      System.exit(StatusCodes.NO_DB_FILE);
    }
  }

  /**
   * Return the normal connection.
   *
   * @return The database connection.
   */
  protected static Connection getCxn() {
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

  protected static Connection getReadOnlyCxn() {
    try {
      if (readOnlyCxn == null || !readOnlyCxn.isValid(1)) {
        initReadOnlyCxn();
      }
    }
    catch (SQLException e) {
      System.err.println("Error when validating connection to file at " + path + ": " + e.getMessage());
      System.exit(StatusCodes.NO_DB_FILE);
    }
    return readOnlyCxn;
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
      if (readOnlyCxn != null && !readOnlyCxn.isClosed()) {
        readOnlyCxn.close();
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

  public static int doInsertQuery(String query, Map<Integer, Object> parameters) {
    return doInsertOrUpdateQuery(query, parameters, true);
  }

  public static void doUpdateQuery(String query, Map<Integer, Object> parameters) {
    doInsertOrUpdateQuery(query, parameters, false);
  }

  public static ResultSet doSelectQuery(String query, Map<Integer, Object> parameters) {
    Connection roCxn = getReadOnlyCxn();
    PreparedStatement stmt = prepareStatement(query, parameters, roCxn);
    ResultSet rs = null;
    try {
      rs = stmt.executeQuery();
    }
    catch (SQLException throwables) {
      throwables.printStackTrace();
      System.exit(StatusCodes.GENERAL_SQL_ERROR);
    }
    return rs;
  }

  public static int getSingleIntResult(String query, Map<Integer, Object> parameters) {
    ResultSet rs = doSelectQuery(query, parameters);
    int count = 0;
    try {
      if (rs.next()) {
        count = rs.getInt(1);
      }
    }
    catch (SQLException throwables) {
      throwables.printStackTrace();
      System.exit(StatusCodes.GENERAL_SQL_ERROR);
    }
    return count;
  }

  protected static int doInsertOrUpdateQuery(String query, Map<Integer, Object> parameters, boolean returnGeneratedKey) {
    Connection cxn = getCxn();
    PreparedStatement stmt = prepareStatement(query, parameters, cxn);
    ResultSet rs = null;
    int returnId = 0;
    try {
      stmt.executeUpdate();
      if (returnGeneratedKey) {
        rs = stmt.getGeneratedKeys();
        if (rs.next()) {
          returnId = rs.getInt(1);
        }
        rs.close();
      }
      stmt.close();
      cxn.commit();
      cxn.close();
    }
    catch (SQLException throwables) {
      throwables.printStackTrace();
      System.exit(StatusCodes.GENERAL_SQL_ERROR);
    }
    return returnId;
  }

//  public static <T extends CreatableFromResultSet> T[] getSelectResults(String query, Map<Integer, Object> parameters, Class<T> type) {
//    ResultSet rs = doSelectQuery(query, parameters);
//    return (T[]) T.createFromResultSet(rs);
//  }
//
//  public static <T extends CreatableFromResultSet> PagedQueryResult<T> getPagedQueryResult(String query, Map<Integer, Object> parameters, String countQuery, Class<T> type) {
//    T[] results = getSelectResults(query, parameters, type);
//    int count = getSingleIntResult(countQuery, parameters);
//    return new PagedQueryResult<T>(results, count);
//  }

  protected static PreparedStatement prepareStatement(String query, Map<Integer, Object> parameters, Connection cxn) {
    PreparedStatement stmt = null;
    try {
      if (query.toLowerCase().startsWith("insert ")) {
        stmt = cxn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
      }
      else {
        stmt = cxn.prepareStatement(query);
      }
      if (parameters != null) {
        for (Integer index : parameters.keySet()) {
          Object value = parameters.get(index);
          if (value.getClass() == Integer.class) {
            stmt.setInt(index, (Integer) value);
          }
          else {
            stmt.setString(index, value.toString());
          }
        }
      }
    }
    catch (SQLException throwables) {
      throwables.printStackTrace();
      System.exit(StatusCodes.GENERAL_SQL_ERROR);
    }
    return stmt;
  }
}
