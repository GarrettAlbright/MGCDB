package pro.albright.mgcdb.Model;

import pro.albright.mgcdb.SteamAPIModel.GetAppDetailsApp;
import pro.albright.mgcdb.SteamAPIModel.GetAppListApp;
import pro.albright.mgcdb.Util.DBCXN;
import pro.albright.mgcdb.Util.StatusCodes;
import pro.albright.mgcdb.Util.SteamCxn;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;

/**
 * Bean class to encapsulate a game.
 */
public class Game implements java.io.Serializable {

  enum GamePropStatus {
    UNCHECKED(0),
    NO(1),
    YES(2);

    private final int value;
    GamePropStatus(int i) {
      this.value = i;
    }

    private static GamePropStatus fromValue(int i) throws Exception {
      for (GamePropStatus status : GamePropStatus.values()) {
        if (status.value == i) {
          return status;
        }
      }
      throw new Exception("Invalid GamePropStatus value.");
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
   * TODO Support titles in multiple languages?
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

  public int getGameId() {
    return gameId;
  }

  public void setGameId(int gameId) {
    this.gameId = gameId;
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

  public void setCreated(Date created) {
    this.created = created;
  }

  public Date getUpdated() {
    return updated;
  }

  public void setUpdated(Date updated) {
    this.updated = updated;
  }

  public Date getSteamUpdated() {
    return steamUpdated;
  }

  public void setSteamUpdated(Date steamUpdated) {
    this.steamUpdated = steamUpdated;
  }

  /**
   * Create a game from a SteamApp SteamAPIModel bean.
   * @param app A GetAppListApp bean instance.
   * @return A Game instance.
   */
  public static Game createFromSteamAppBean(GetAppListApp app) {
    Game game = new Game();
    game.setSteamId(app.getAppid());
    game.setTitle(app.getName());
    return game;
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
   * @return Array of Games.
   */
  public static Game[] getNewGamesFromSteam(int limit) {
    SteamCxn steamCxn = new SteamCxn();
    int lastAppId = Game.getNewestGameSteamId();
    return steamCxn.getNewGames(lastAppId, limit);
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
    save(false);
  }

  /**
   * Save this game in the DB.
   *
   * @param withSteamUpdate Whether to update the "steam_updated" timestamp.
   *                        Should be true if this save is happening as the
   *                        result of an update from the Steam API.
   */
  public void save(boolean withSteamUpdate) {
    Connection cxn = DBCXN.getCxn();
    PreparedStatement stmt;
    try {
      if (gameId == 0) {
        // This hasn't been inserted yet
        stmt = cxn.prepareStatement("INSERT INTO games (steam_id, title) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
      }
      else {
        StringBuilder sb = new StringBuilder("UPDATE games SET steam_id = ?, title = ?, mac = ?");
        if (withSteamUpdate) {
          sb.append(", steam_updated = CURRENT_TIMESTAMP");
        }
        sb.append(" WHERE game_id = ?");

        stmt = cxn.prepareStatement(sb.toString());
        stmt.setInt(3, mac.value);
        stmt.setInt(4, gameId);
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

  /**
   * Update this game with information from Steam.
   *
   * @return True if the game was able to be successfully updated.
   */
  public boolean updateFromSteam() {
    SteamCxn steam = new SteamCxn();
    GetAppDetailsApp updatedGame = steam.getUpdatedGameDetails(this);
    if (updatedGame == null) {
      return false;
    }
    setTitle(updatedGame.getName());
    Boolean mac = updatedGame.getPlatforms().get("mac");
    if (mac != null) {
      if (mac) {
        setMac(Game.GamePropStatus.YES);
      }
      else {
        setMac(Game.GamePropStatus.NO);
      }
    }
    save(true);
    return true;
  }

  /**
   * Get games in the DB which have gone the longest without a Steam update.
   *
   * @param limit The maximum number of games to load.
   * @return An array of Games.
   */
  public static Game[] getGamesToUpdate(int limit) {
    Connection cxn = DBCXN.getCxn();
    Game[] games = {};
    try {
      // Don't update games less than one day old
      PreparedStatement stmt = cxn.prepareStatement("SELECT * FROM games WHERE steam_updated < datetime('now', '-1 day') ORDER BY steam_updated ASC LIMIT ?");
      stmt.setInt(1, limit);
      ResultSet rs = stmt.executeQuery();
      games = Game.createFromResultSet(rs);
    }
    catch (SQLException e) {
      System.err.println("Error loading games to update from DB.");
    }
    return games;
  }

  /**
   * Create a game from a ResultSet from a query of the game table in the DB.
   *
   * @param rs A ResultSet.
   * @return A Game.
   */
  public static Game[] createFromResultSet(ResultSet rs) {
    ArrayList<Game> games = new ArrayList<>();
    try {
      while (rs.next()) {
        Game game = new Game();
        game.setGameId(rs.getInt("game_id"));
        game.setSteamId(rs.getInt("steam_id"));
        game.setTitle(rs.getString("title"));
        game.setMac(GamePropStatus.fromValue(rs.getInt("mac")));
        game.setSixtyFour(GamePropStatus.fromValue(rs.getInt("sixtyfour")));
        game.setSilicon(GamePropStatus.fromValue(rs.getInt("silicon")));
        game.setCreated(DBCXN.parseTimestamp(rs.getString("created")));
        game.setUpdated(DBCXN.parseTimestamp(rs.getString("updated")));
        game.setSteamUpdated(DBCXN.parseTimestamp(rs.getString("steam_updated")));
        games.add(game);
      }
    }
    catch (Exception e) {
      System.err.println("Error trying to load game from DB row");
      System.exit(StatusCodes.GENERAL_SQL_ERROR);
    }
    return games.toArray(new Game[0]);
  }

  public static Game[] getAllAlpha(int page) {
    int perPage = 20;
    Connection cxn = DBCXN.getCxn();
    Game[] games = {};
    int offset = page * perPage;
    try {
      PreparedStatement stmt = cxn.prepareStatement("SELECT * FROM games ORDER BY title ASC LIMIT ? OFFSET ?");
      stmt.setInt(1, perPage);
      stmt.setInt(2, offset);
      ResultSet rs = stmt.executeQuery();
      games = Game.createFromResultSet(rs);
    }
    catch (SQLException throwables) {
      System.err.println("Error loading games to update from DB.");
    }
    return games;
  }

}
