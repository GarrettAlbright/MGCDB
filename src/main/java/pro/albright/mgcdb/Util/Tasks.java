package pro.albright.mgcdb.Util;

import pro.albright.mgcdb.Model.Game;
import pro.albright.mgcdb.Model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class Tasks {

  /**
   * Invoke a task.
   *
   * The code in each task should just validate parameter(s), then pass off to
   * a separate method.
   *
   * @param task The task name
   * @param params Parameters which the task may need, from the CLI invocation.
   * @throws SQLException An SQL-related error occurred.
   */
  public static void invoke(String task, String[] params) throws SQLException {
    switch (task) {
      case "initdb":
        // Initialize the database
        boolean deleteIfExists = params.length > 0 && params[0].equals("delete");
        initDb(deleteIfExists);
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
        int updateGameLimit = params.length < 1 ? 200 : Integer.parseInt(params[0]);
        if (updateGameLimit > 200) {
          System.err.println("Steam will respond with errors if we try to update more than 200 games in a five-minute period, so refusing to attempt to do so.");
          System.exit(StatusCodes.BAD_TASK_PARAM);
        }
        updateGames(updateGameLimit);
        break;
      case "updategame":
        int toUpdate = params.length < 1 ? 0 : Integer.parseInt(params[0]);
        if (toUpdate == 0) {
          System.err.println("Please specify the MGCDB ID of the game to update.");
          System.exit(StatusCodes.BAD_TASK_PARAM);
        }
        Game game = Game.getById(toUpdate);
        if (game == null) {
          System.err.printf("A game matching ID %d was not found.%n", toUpdate);
          System.exit(StatusCodes.BAD_TASK_PARAM);
        }
        updateGame(game);
        break;
      case "updatedb":
        int updateIndex = params.length < 1 ? 0 : Integer.parseInt(params[0]);
        updateDb(updateIndex);
        break;
      case "updateownership":
        updateOwnership(1);
        break;
      default:
        System.err.printf("Handler for task %s not found.%n", task);
        System.exit(StatusCodes.NO_TASK_HANDLER);
    }
  }

  /**
   * Initialize the database by creating the needed tables.
   *
   * @throws SQLException An SQL-related error occurred.
   */
  private static void initDb(boolean deleteIfExists) throws SQLException {
    if (deleteIfExists) {
      System.out.println("Any existing database will be deleted. Hope you meant to do that.");
    }
    else {
      System.out.println("Existing database (if any) will be used; non-existing tables will be created.");
      System.out.println("Pass `delete` parameter to delete any existing database.");
    }
    DBCXN.createIfNotExists(deleteIfExists);
    Connection cxn = DBCXN.getCxn();
    Statement stmt = cxn.createStatement();

    Map<String, String> commands =getCurrentCreateQueries();

    stmt.addBatch(commands.get("createGamesQuery"));
    stmt.addBatch(commands.get("createGamesTriggerQuery"));

    stmt.addBatch(commands.get("createUsersQuery"));
    stmt.addBatch(commands.get("createUsersTriggerQuery"));

    stmt.addBatch(commands.get("createOwnershipQuery"));

    stmt.addBatch(commands.get("createVotesQuery"));

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
      System.out.printf("Updating game %s (Our ID: %d, Steam ID: %d)%n", oldGame.getTitle(), oldGame.getGameId(), oldGame.getSteamId());
      if (!oldGame.updateFromSteam()) {
        System.out.printf("/!\\ Game %s (%d) did not successfully update (API call failed?)%n", oldGame.getTitle(), oldGame.getSteamId());
      }
    }
  }

  /**
   * Update data from Steam for a single game.
   * @param game The Game to update.
   */
  public static void updateGame(Game game) {
    System.out.printf("Updating game %s (Our ID: %d, Steam ID: %d)%n", game.getTitle(), game.getGameId(), game.getSteamId());
    game.updateFromSteam();
  }

  /**
   * Update the database schema.
   * @param updateIdx The ID of the update to run.
   */
  public static void updateDb(int updateIdx) {
    switch (updateIdx) {
      case 1:
        Connection cxn = DBCXN.getCxn();
        try {
          Statement stmt = cxn.createStatement();
          stmt.addBatch("ALTER TABLE users RENAME TO tempusers");

          Map<String, String> commands = getCurrentCreateQueries();
          stmt.addBatch(commands.get("createUsersQuery"));
          stmt.addBatch(commands.get("createUsersTriggerQuery"));

          stmt.addBatch("INSERT INTO users (user_id, steam_user_id, " +
            "nickname, avatar_hash, last_auth, created, updated) " +
            "SELECT * FROM tempusers;");
          stmt.addBatch("DROP TABLE tempusers");

          stmt.executeBatch();
          cxn.commit();

          break;
        }
        catch (SQLException throwables) {
          throwables.printStackTrace();
          System.exit(StatusCodes.GENERAL_SQL_ERROR);
        }
        break;
      default:
        System.out.printf("Database update %d not found.%n", updateIdx);
        System.exit(StatusCodes.BAD_TASK_PARAM);
    }
    System.out.println("Update complete.");
  }

  /**
   *
   * @param userId
   */
  public static void updateOwnership(int userId) {
    User me = User.getById(1);
    me.updateOwnedGames();
  }

  /**
   * Returns SQL commands to create the current state of the DB tables.
   * @return SQL commands in a Map<String, String>.
   */
  public static Map<String, String> getCurrentCreateQueries() {
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

