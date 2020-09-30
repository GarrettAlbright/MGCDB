package pro.albright.mgcdb;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Tasks {

  /**
   * Invoke a task.
   *
   * @param task The task name
   * @param params Parameters which the task may need, from the CLI invocation.
   * @throws SQLException An SQL-related error occurred.
   */
  public static void invoke(String task, String[] params) throws SQLException {
    switch (task) {
      case "initdb":
        // Initialize the database
        initDb();
        break;
      default:
        System.err.printf("Handler for task %s not found.", task);
        System.exit(StatusCodes.NO_TASK_HANDLER);
    }
  }

  /**
   * Initialize the database by creating the needed tables.
   *
   * @throws SQLException An SQL-related error occurred.
   */
  private static void initDb() throws SQLException {
    DBCXN.createIfNotExists(true);
    Connection cxn = DBCXN.getCxn();
    Statement stmt = cxn.createStatement();
    // TODO delete database file
    String createGamesQuery = "CREATE TABLE games ( " +
      // Note that id has to be an INTEGER, not UNSIGNED INTEGER, in order for
      // it to be a proper alias for the SQLite rowid.
      // https://www.sqlite.org/lang_createtable.html#rowid
      "game_id INTEGER PRIMARY KEY, " +
      "steam_id INTEGER UNIQUE, " +
      // SQLite does not actually enforce field character lengths but I'm gonna
      // use them anyway
      "title VARCHAR(255) NOT NULL DEFAULT '', " +
      "created TEXT NOT NULL DEFAULT CURRENT_DATETIME, " +
      "updated TEXT NOT NULL DEFAULT CURRENT_DATETIME)";
    String createGamesTriggerQuery = "CREATE TRIGGER update_games " +
      "AFTER UPDATE ON games FOR EACH ROW BEGIN " +
      "UPDATE games SET last_updated = CURRENT_DATETIME WHERE game_id = OLD.game_id; " +
      "END;";
    stmt.addBatch(createGamesQuery);
    stmt.addBatch(createGamesTriggerQuery);
    stmt.executeBatch();
    cxn.commit();
    System.out.println("Database creation complete.");
  }
}
