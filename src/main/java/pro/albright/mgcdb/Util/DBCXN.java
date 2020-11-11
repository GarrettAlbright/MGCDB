package pro.albright.mgcdb.Util;

import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.sql.*;
import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

public class DBCXN {

  private static Connection cxn;
  private static String path;
  private static Connection readOnlyCxn;

  /**
   * Init the writable connection.
   *
   * The writable connection is inefficiently closed after every query, but
   * this allows for other processes (such as the `sqlite` CLI tool to access
   * the database without it being locked.
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
      config.enforceForeignKeys(true);
      cxn = config.createConnection("jdbc:sqlite:" + path);
      cxn.setAutoCommit(false);
    }
    catch (SQLException e) {
      System.err.println("Error when opening database file at " + path + ": " + e.getMessage());
      System.exit(StatusCodes.NO_DB_FILE);
    }
  }

  /**
   * Init the read-only connection.
   *
   * Using this connection for read-only queries avoids the database being
   * locked.
   */
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
   * Return the writable connection.
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

  /**
   * Return the read-only connection.
   *
   * @return The database connection.
   */
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

  /**
   * Perform an insert query and return the ID of the inserted row.
   *
   * @param query The query.
   * @param parameters Parameters. See prepareStatement() for more.
   * @return The ID of the inserted row.
   */
  public static int doInsertQuery(String query, Map<Integer, Object> parameters) {
    return doInsertOrUpdateQuery(query, parameters, true);
  }

  /**
   * Perform an update query.
   *
   * @param query The query.
   * @param parameters Parameters. See prepareStatement() for more.
   */
  public static void doUpdateQuery(String query, Map<Integer, Object> parameters) {
    doInsertOrUpdateQuery(query, parameters, false);
  }

  /**
   * Perform a select query and return the resulting ResultSet.
   *
   * @param query The query.
   * @param parameters Parameters. See prepareStatement() for more.
   * @return The resulting ResultSet.
   */
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

  /**
   * Perform a select query which is expected to have a result of a single int;
   * for example, a SELECT COUNT(*)… query.
   *
   * @param query The query.
   * @param parameters Parameters. See prepareStatement() for more.
   * @return The resulting integer.
   */
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

  /**
   * Generalized method to do an insert or update query.
   *
   * @param query The query.
   * @param parameters Parameters. See prepareStatement() for more.
   * @param returnGeneratedKey If true, the ID of the inserted row will be
   *                           retrieved after the query and returned.
   * @return The ID of the inserted row if requested; 0 otherwise.
   */
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
      // The changes will not be readable by the read-only connection until it
      // is closed.
      getReadOnlyCxn().close();
    }
    catch (SQLException throwables) {
      throwables.printStackTrace();
      System.exit(StatusCodes.GENERAL_SQL_ERROR);
    }
    return returnId;
  }

  /**
   * Prepare a query.
   *
   * @param query The query.
   * @param parameters A Map<Integer, Object> of parameters for the query.
   *                   Currently we expect Object to be one of the following:
   *                   - int
   *                   - long
   *                   - String
   * @param cxn The connection to prepare the query against.
   * @return A PreparedStatement generated from the query and parameters.
   */
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
          Class paramClass = value.getClass();
          // Apparently we can't do a switch statement with a class.
          if (paramClass == Integer.class) {
            stmt.setInt(index, (Integer) value);
          }
          else if (paramClass == Long.class) {
            stmt.setLong(index, (Long) value);
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
