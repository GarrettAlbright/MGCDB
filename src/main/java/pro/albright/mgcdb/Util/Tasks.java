package pro.albright.mgcdb.Util;

import pro.albright.mgcdb.Model.Game;
import pro.albright.mgcdb.Model.User;

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
        int gameLimit = params.length < 1 ? 100 : Integer.parseInt(params[0]);
        if (gameLimit > 50000) {
          System.err.println("Requested game count exceeds limits of Steam API (and human decency)");
          System.exit(StatusCodes.BAD_TASK_PARAM);
        }
        newGames(gameLimit);
        break;
      case "updategames":
        int updateGameLimit = params.length < 1 ? 100 : Integer.parseInt(params[0]);
        if (updateGameLimit > 200) {
          System.err.println("Steam will respond with errors if we try to update more than 200 games in a five-minute period, so refusing to attempt to do so.");
          System.exit(StatusCodes.BAD_TASK_PARAM);
        }
        updateGames(updateGameLimit);
        break;
      case "auth":
        testAuth();
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
      // Game.GamePropStatus enum - Mac compatibility
      "mac INTEGER NOT NULL DEFAULT 0, " +
      // Game.GamePropStatus enum - 64-bit Intel (Catalina) compatibility
      "sixtyfour INTEGER NOT NULL DEFAULT 0, " +
      // Game.GamePropStatus enum - Apple Silicon compatibility
      "silicon INTEGER NOT NULL DEFAULT 0, " +
      "created TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
      "updated TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
      "steam_release TEXT NOT NULL DEFAULT '0000-01-01', " +
      "steam_updated TEXT NOT NULL DEFAULT '0000-01-01 00:00:00')";
    String createGamesTriggerQuery = "CREATE TRIGGER update_games " +
      "AFTER UPDATE ON games FOR EACH ROW BEGIN " +
      "UPDATE games SET updated = CURRENT_TIMESTAMP WHERE game_id = OLD.game_id; " +
      "END;";
    stmt.addBatch(createGamesQuery);
    stmt.addBatch(createGamesTriggerQuery);

    String createUsersQuery = "CREATE TABLE users ( " +
      "user_id INTEGER PRIMARY KEY, " +
      "steam_user_id INTEGER UNIQUE, " +
      "nickname VARCHAR(255) NOT NULL DEFAULT '', " +
      "avatar_hash VARCHAR(255) NOT NULL DEFAULT '', " +
      "last_auth TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
      "created TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
      "updated TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)";
    String createUsersTriggerQuery = "CREATE TRIGGER update_users " +
      "AFTER UPDATE ON users FOR EACH ROW BEGIN " +
      "UPDATE users SET updated = CURRENT_TIMESTAMP WHERE user_id = OLD.user_id; " +
      "END;";
    stmt.addBatch(createUsersQuery);
    stmt.addBatch(createUsersTriggerQuery);

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
      System.out.printf("Saving new game %s (%d)%n", newGame.getTitle(), newGame.getSteamId());
      newGame.save();
    }
    System.out.println("Finished fetching new games.");
  }

  /**
   * Update not-recently-updated games from Steam.
   *
   * @param limit Max number of games to update.
   */
  public static void updateGames(int limit) {
    Game[] oldGames = Game.getGamesToUpdate(limit);
    for (Game oldGame : oldGames) {
      System.out.printf("Updating game %s (%d)%n", oldGame.getTitle(), oldGame.getSteamId());
      if (!oldGame.updateFromSteam()) {
        System.out.printf("/!\\ Game %s (%d) did not successfully update (API call failed?)%n", oldGame.getTitle(), oldGame.getSteamId());
      }
    }
  }

  public static void testAuth() {
//    User.getSession();
  }
}
