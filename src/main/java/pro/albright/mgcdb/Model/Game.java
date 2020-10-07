package pro.albright.mgcdb.Model;

import pro.albright.mgcdb.SteamAPIModel.GetAppListApp;
import pro.albright.mgcdb.Util.DBCXN;
import pro.albright.mgcdb.Util.StatusCodes;
import pro.albright.mgcdb.Util.SteamCxn;

import java.sql.*;
import java.util.Date;

/**
 * Bean class to encapsulate a game.
 */
public class Game implements java.io.Serializable {

  enum GamePropStatus {
    UNCHECKED(0),
    NO(1),
    YES(2);

    private int value;
    GamePropStatus(int i) {
      this.value = i;
    }
  }
  /**
   * The MGCDB game ID.
   */
  private int gameId;

  /**
   * The Steam ID of the game.
   */
  private int steamId;

  /**
   * The game title.
   *
   * @TODO Support titles in multiple languages?
   */
  private String title;

  /**
   * Whether the game is Mac compatible according to Steam.
   */
  private Game.GamePropStatus mac;

  /**
   * Whether the game is 64-bit (Catalina) compatible according to Steam.
   */
  private Game.GamePropStatus sixtyFour;

  /**
   * Whether the game is Apple Silicon-compatible according to Steam.
   */
  private Game.GamePropStatus silicon;

   /**
   * When the record for this game was created.
   *
   * Note that this is *not* the creation/release date of the game itself.
   */
  private Date created;

  /**
   * When the record for this game was last updated.
   */
  private Date updated;

  /**
   * When the record was last updated from the Steam API. Note we don't count
   * the record's creation as an "update" from the API because we don't get
   * full game details when first creating it.
   */
  private Date steamUpdated;

  /**
   * Create a game from a SteamApp SteamAPIModel bean.
   * @param app
   * @return
   */
  public static Game createFromSteamAppBean(GetAppListApp app) {
    Game game = new Game();
    game.setSteamId(app.getAppid());
    game.setTitle(app.getName());
    return game;
  }

  public int getGameId() {
    return gameId;
  }

  public int getSteamId() {
    return steamId;
  }

  public void setSteamId(int steamId) {
    this.steamId = steamId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public GamePropStatus getMac() {
    return mac;
  }

  public void setMac(GamePropStatus mac) {
    this.mac = mac;
  }

  public GamePropStatus getSixtyFour() {
    return sixtyFour;
  }

  public void setSixtyFour(GamePropStatus sixtyFour) {
    this.sixtyFour = sixtyFour;
  }

  public GamePropStatus getSilicon() {
    return silicon;
  }

  public void setSilicon(GamePropStatus silicon) {
    this.silicon = silicon;
  }

  public Date getCreated() {
    return created;
  }

  public Date getUpdated() {
    return updated;
  }

  public Date getSteamUpdated() {
    return steamUpdated;
  }

  /**
   * Get the Steam ID of the most recently-created game in the database.
   *
   * This is useful for checking for new games on Steam we don't know about,
   * especially when initially populating the database, which is done in small
   * batches from oldest to newest; but also generally for periodically check-
   * ing for new games.
   *
   * @return The Steam ID of the most recently-created game in the DB, or -1 if
   * there are no games in the DB currently.
   */
  public static int getNewestGameSteamId() {
    Connection cxn = DBCXN.getCxn();
    int maxSteamId = -1;
    try {
      Statement stmt = cxn.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT MAX(steam_id) AS max FROM games");
      if (rs.next()) {
        // Note that we have to use a named column here - rs.getInt(0) crashes
        // if the column value is null. With a named column, we get 0 instead,
        // which is fine for this case.
        maxSteamId = rs.getInt("max");
      }
    }
    catch (SQLException e) {
      System.exit(StatusCodes.GENERAL_SQL_ERROR);
    }
    return maxSteamId;
  }

  /**
   * Get new games from the Steam DB.
   *
   * @param limit Max number of new games to fetch.
   * @returnb Array of Games.
   */
  public static Game[] getNewGamesFromSteam(int limit) {
    SteamCxn steamCxn = new SteamCxn();
    int lastAppId = Game.getNewestGameSteamId();
    Game[] newGames = steamCxn.getNewGames(lastAppId, limit);
    return newGames;
 }

  /**
   * Check if we have a game in the DB by the Steam ID.
   *
   * @param steamId The game's Steam ID.
   * @return True if the game exists; false otherwise.
   */
  public static boolean existsBySteamId(int steamId) {
    Connection cxn = DBCXN.getCxn();
    boolean gameExists = false;
    try {
      PreparedStatement stmt = cxn.prepareStatement("SELECT COUNT(*) AS count FROM games WHERE steam_id = ?");
      stmt.setInt(1, steamId);
      ResultSet result = stmt.executeQuery();
      result.next();
      gameExists = result.getInt("count") == 1;
    }
    catch (SQLException e) {
      System.err.println("SQL error when querying for existence of game");
      System.exit(StatusCodes.GENERAL_SQL_ERROR);
    }
    return gameExists;
  }

  /**
   * Save a game in the DB.
   */
  public void save() {
    Connection cxn = DBCXN.getCxn();
    PreparedStatement stmt;
    try {
      if (gameId == 0) {
        // This hasn't been inserted yet
        stmt = cxn.prepareStatement("INSERT INTO games (steam_id, title) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
      }
      else {
        stmt = cxn.prepareStatement("UPDATE games SET steam_id = ?, title = ? WHERE game_id = ?");
        stmt.setInt(3, gameId);
      }
      stmt.setInt(1, steamId);
      stmt.setString(2, title);
      stmt.executeUpdate();
      if (gameId == 0) {
        // Set the game ID to the one just created
        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) {
          gameId = rs.getInt(1);
        }
      }
      stmt.close();
      cxn.commit();
    }
    catch (SQLException e) {
      System.err.println("Error when saving game.");
      System.exit(StatusCodes.GENERAL_SQL_ERROR);
    }
  }
}
