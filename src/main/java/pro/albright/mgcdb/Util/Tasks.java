package pro.albright.mgcdb.Util;

import pro.albright.mgcdb.Model.Game;
import pro.albright.mgcdb.Util.StatusCodes;

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
      case "newgames":
        int gameLimit = params.length < 1 ? 5 : Integer.parseInt(params[0]);
        if (gameLimit > 50000) {
          System.err.println("Requested game count exceeds limits of Steam API (and human decency)");
          System.exit(StatusCodes.BAD_TASK_PARAM);
        }
        newGames(gameLimit);
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
    String createGamesQuery = "CREATE TABLE games ( " +
      // Note that id has to be an INTEGER, not UNSIGNED INTEGER, in order for
      // it to be a proper alias for the SQLite rowid.
      // https://www.sqlite.org/lang_createtable.html#rowid
      "game_id INTEGER PRIMARY KEY, " +
      "steam_id INTEGER UNIQUE, " +
      // SQLite does not actually enforce field character lengths but I'm gonna
      // use them anyway
      "title VARCHAR(255) NOT NULL DEFAULT '', " +
      "created TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
      "updated TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)";
    String createGamesTriggerQuery = "CREATE TRIGGER update_games " +
      "AFTER UPDATE ON games FOR EACH ROW BEGIN " +
      "UPDATE games SET last_updated = CURRENT_TIMESTAMP WHERE game_id = OLD.game_id; " +
      "END;";
    stmt.addBatch(createGamesQuery);
    stmt.addBatch(createGamesTriggerQuery);
    stmt.executeBatch();
    cxn.commit();
    System.out.println("Database creation complete.");
  }

  /**
   * Fetch new games.
   *
   * @param limit Max number of new games to fetch.
   */
  private static void newGames(int limit) {
    Game[] newGames = Game.getNewGamesFromSteam(limit);
    for (Game newGame : newGames) {
      System.out.printf("Saving new game %s (%d) (not really)%n", newGame.getTitle(), newGame.getSteamId());
      newGame.save();
    }
    System.out.println("Finished fetching new games.");
  }
}
