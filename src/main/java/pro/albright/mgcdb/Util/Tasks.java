package pro.albright.mgcdb.Util;

import pro.albright.mgcdb.Model.Game;
import pro.albright.mgcdb.Model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class Tasks {

  private Config config;
  private DBCxn dbCxn;

  public Tasks(Config config, DBCxn dbCxn) {
    this.config = config;
    this.dbCxn = dbCxn;
  }

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
  public void invoke(String task, String[] params) throws SQLException {
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
        updateOwnership();
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
  private void initDb(boolean deleteIfExists) throws SQLException {
    if (deleteIfExists) {
      System.out.println("Any existing database will be deleted. Hope you meant to do that.");
    }
    else {
      System.out.println("Existing database (if any) will be used; non-existing tables will be created.");
      System.out.println("Pass `delete` parameter to delete any existing database.");
    }
    dbCxn.createIfNotExists(deleteIfExists);

    dbCxn.initializeDb();

    System.out.println("Database creation complete.");
  }

  /**
   * Fetch new games.
   *
   * @param limit Max number of new games to fetch.
   */
  private static void newGames(int limit) {
    Game[] newGames = Game.getAndSaveNewGamesFromSteam(limit);
    for (Game newGame : newGames) {
      System.out.printf("Saved new game %s (%d)%n", newGame.getTitle(), newGame.getSteamId());
    }
    System.out.println("Finished fetching new games.");
  }

  /**
   * Update not-recently-updated games from Steam.
   *
   * @param limit Max number of games to update.
   */
  public static void updateGames(int limit) {
    Game[] games = Game.updateGamesFromSteam(limit);
    for (Game game : games) {
      System.out.printf("Updated game %s (Our ID: %d, Steam ID: %d)%n", game.getTitle(), game.getGameId(), game.getSteamId());
    }
  }

  /**
   * Update data from Steam for a single game.
   * @param game The Game to update.
   */
  public void updateGame(Game game) {
    System.out.printf("Updating game %s (Our ID: %d, Steam ID: %d)%n", game.getTitle(), game.getGameId(), game.getSteamId());
    game.updateFromSteam();
  }

  /**
   * Update the database schema.
   * @param updateIdx The ID of the update to run.
   */
  public void updateDb(int updateIdx) {
    switch (updateIdx) {
      case 1:
        Connection cxn = dbCxn.getCxn();
        try {
          Statement stmt = cxn.createStatement();
          stmt.execute("ALTER TABLE users ADD COLUMN last_game_synch TEXT NOT NULL DEFAULT '0000-01-01 00:00:00'");
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
   * Update owned games for users which have not had that updated recently.
   */
  public static void updateOwnership() {
    User[] users = User.getUsersNeedingOwnershipUpdate();
    for (User user : users) {
      System.out.printf("Updating ownership for user %d%n", user.getUserId());
      user.updateOwnedGames();
    }
  }
}
