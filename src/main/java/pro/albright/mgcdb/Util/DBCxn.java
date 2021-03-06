package pro.albright.mgcdb.Util;

import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.sql.*;
import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DBCxn {

  private static Connection cxn;
  private static String path;
  private static Connection readOnlyCxn;

  public DBCxn(String path) {
    this.path = path.replaceFirst("^~", System.getProperty("user.home"));
  }

  /**
   * Init the writable connection.
   *
   * The writable connection is inefficiently closed after every query, but
   * this allows for other processes (such as the `sqlite` CLI tool to access
   * the database without it being locked.
   */
  protected void init() {
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
      e.printStackTrace();
      System.exit(StatusCodes.NO_DB_FILE);
    }
  }

  /**
   * Init the read-only connection.
   *
   * Using this connection for read-only queries avoids the database being
   * locked.
   */
  protected void initReadOnlyCxn() {
    try {
      if (readOnlyCxn != null && !readOnlyCxn.isClosed()) {
        readOnlyCxn.close();
      }
      SQLiteConfig sqLiteConfig = new SQLiteConfig();
      sqLiteConfig.setReadOnly(true);
      sqLiteConfig.setJournalMode(SQLiteConfig.JournalMode.WAL);
      readOnlyCxn = sqLiteConfig.createConnection("jdbc:sqlite:" + path);
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
  protected Connection getCxn() {
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
  protected Connection getReadOnlyCxn() {
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
  public void createIfNotExists(boolean deleteIfExists) throws SQLException {
    if (deleteIfExists) {
      delete();
    }

    File dbFile = new File(path);
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
   * Delete the database file.
   */
  public void delete() {
    try {
      if (cxn != null && !cxn.isClosed()) {
        cxn.close();
      }
      if (readOnlyCxn != null && !readOnlyCxn.isClosed()) {
        readOnlyCxn.close();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(StatusCodes.GENERAL_SQL_ERROR);
    }

    File dbFile = new File(path);
    if (dbFile.exists()) {
      dbFile.delete();
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
  public Date parseTimestamp(String timestamp) {
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
  public int doInsertQuery(String query, Map<Integer, Object> parameters) {
    return doInsertOrUpdateQuery(query, parameters, true);
  }

  /**
   * Perform an update query.
   *
   * @param query The query.
   * @param parameters Parameters. See prepareStatement() for more.
   */
  public void doUpdateQuery(String query, Map<Integer, Object> parameters) {
    doInsertOrUpdateQuery(query, parameters, false);
  }

  /**
   * Perform a select query and return the resulting ResultSet.
   *
   * @param query The query.
   * @param parameters Parameters. See prepareStatement() for more.
   * @return The resulting ResultSet.
   */
  public ResultSet doSelectQuery(String query, Map<Integer, Object> parameters) {
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
  public int getSingleIntResult(String query, Map<Integer, Object> parameters) {
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
  protected int doInsertOrUpdateQuery(String query, Map<Integer, Object> parameters, boolean returnGeneratedKey) {
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
  protected PreparedStatement prepareStatement(String query, Map<Integer, Object> parameters, Connection cxn) {
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

  /**
   * Initialize the database by executing the table creation queries.
   */
  public void initializeDb() {
    Statement stmt = null;
    try {
      Connection cxn = getCxn();
      stmt = cxn.createStatement();
      Map<String, String> commands = getCurrentCreateQueries();

      stmt.addBatch(commands.get("createGamesQuery"));
      stmt.addBatch(commands.get("createGamesTriggerQuery"));

      stmt.addBatch(commands.get("createUsersQuery"));
      stmt.addBatch(commands.get("createUsersTriggerQuery"));

      stmt.addBatch(commands.get("createOwnershipQuery"));

      stmt.addBatch(commands.get("createVotesQuery"));

      stmt.executeBatch();
      cxn.commit();
    }
    catch (SQLException throwables) {
      throwables.printStackTrace();
      System.exit(StatusCodes.GENERAL_SQL_ERROR);
    }
  }

  /**
   * Returns SQL commands to create the current state of the DB tables.
   * @return SQL commands in a Map<String, String>.
   */
  public Map<String, String> getCurrentCreateQueries() {
    Map<String, String> commands = new HashMap<>();
    commands.put("createGamesQuery", "CREATE TABLE IF NOT EXISTS games ( " +
      // Note that id has to be an INTEGER, not UNSIGNED INTEGER, in order for
      // it to be a proper alias for the SQLite rowid.
      // https://www.sqlite.org/lang_createtable.html#rowid
      "game_id INTEGER PRIMARY KEY, " +
      "steam_id INTEGER UNIQUE, " +
      // SQLite does not actually enforce field character lengths but I'm gonna
      // use them anyway
      "title VARCHAR(255) NOT NULL DEFAULT '', " +
      // Game.GamePropStatus enum - Mac compatibility
      "mac INTEGER NOT NULL DEFAULT 0, " +
      // Game.GamePropStatus enum - 64-bit Intel (Catalina) compatibility
      "sixtyfour INTEGER NOT NULL DEFAULT 0, " +
      // Game.GamePropStatus enum - Apple Silicon compatibility
      "silicon INTEGER NOT NULL DEFAULT 0, " +
      "created TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
      "updated TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
      "steam_release TEXT DEFAULT '0000-01-01', " +
      "steam_updated TEXT NOT NULL DEFAULT '0000-01-01 00:00:00')");
    commands.put("createGamesTriggerQuery", "CREATE TRIGGER IF NOT EXISTS update_games " +
      "AFTER UPDATE ON games FOR EACH ROW BEGIN " +
      "UPDATE games SET updated = CURRENT_TIMESTAMP WHERE game_id = OLD.game_id; " +
      "END;");

    commands.put("createUsersQuery", "CREATE TABLE IF NOT EXISTS users ( " +
      "user_id INTEGER PRIMARY KEY, " +
      "steam_user_id INTEGER UNIQUE, " +
      "nickname VARCHAR(255) NOT NULL DEFAULT '', " +
      "avatar_hash VARCHAR(255) NOT NULL DEFAULT '', " +
      "last_auth TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
      "last_game_synch TEXT NOT NULL DEFAULT '0000-01-01 00:00:00', " +
      "created TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
      "updated TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
    commands.put("createUsersTriggerQuery", "CREATE TRIGGER IF NOT EXISTS update_users " +
      "AFTER UPDATE ON users FOR EACH ROW BEGIN " +
      "UPDATE users SET updated = CURRENT_TIMESTAMP WHERE user_id = OLD.user_id; " +
      "END;");

    commands.put("createOwnershipQuery", "CREATE TABLE IF NOT EXISTS ownership (" +
      "ownership_id INTEGER PRIMARY KEY, " +
      "user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE, " +
      "game_id INTEGER NOT NULL REFERENCES games(game_id) ON DELETE CASCADE, " +
      "created TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");

    // Note that we do not make ownership_id UNIQUE because in the future there
    // may be more than one type of vote (in which case a "type" column will
    // need to be added).
    commands.put("createVotesQuery", "CREATE TABLE IF NOT EXISTS votes (" +
      "vote_id INTEGER PRIMARY KEY, " +
      "ownership_id INTEGER NOT NULL REFERENCES ownership(ownership_id) ON DELETE CASCADE, " +
      "vote INTEGER NOT NULL, " +
      "created TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");

    return commands;
  }
}
