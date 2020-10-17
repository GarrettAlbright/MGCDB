package pro.albright.mgcdb.Model;

import pro.albright.mgcdb.SteamAPIModel.GetAppDetailsApp;
import pro.albright.mgcdb.SteamAPIModel.GetAppListApp;
import pro.albright.mgcdb.Util.DBCXN;
import pro.albright.mgcdb.Util.PagedQueryResult;
import pro.albright.mgcdb.Util.StatusCodes;
import pro.albright.mgcdb.Util.SteamCxn;

import javax.xml.transform.Result;
import java.sql.*;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Bean class to encapsulate a game.
 */
public class Game implements java.io.Serializable {

  /**
   * The number of games to show per page on listings.
   */
  public static final int perPage = 25;

  /**
   * An enum for storing a "three-state boolean" for states of some game status
   * fields - a "yes" value, a "no" value, and a value indicating that we're
   * not sure of the value yet. In particular, this "unchecked" state means we
   * haven't got an answer from the Steam API for its value yet.
   */
  public enum GamePropStatus {
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

  /**
   * When the game was released on Steam.
   */
  private LocalDate steamReleaseDate;

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

  public LocalDate getSteamReleaseDate() {
    return steamReleaseDate;
  }

  public void setSteamReleaseDate(LocalDate steamReleaseDate) {
    this.steamReleaseDate = steamReleaseDate;
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
    return DBCXN.getSingleIntResult("SELECT MAX(steam_id) AS max FROM games", null);
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
    String query = "SELECT COUNT(*) AS count FROM games WHERE steam_id = ?";
    Map<Integer, Object> parameters = new HashMap<>();
    parameters.put(1, steamId);
    return DBCXN.getSingleIntResult(query, parameters) > 0;
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
    String query;
    Map<Integer, Object> parameters = new HashMap<>();

    if (gameId == 0) {
      query = "INSERT INTO games (steam_id, title) VALUES (?, ?)";
    }
    else {
      StringBuilder sb = new StringBuilder("UPDATE games SET steam_id = ?, title = ?, mac = ?, sixtyfour = ?, silicon = ?, steam_release = ?");
      if (withSteamUpdate) {
        sb.append(", steam_updated = CURRENT_TIMESTAMP");
      }
      sb.append(" WHERE game_id = ?");
      query = sb.toString();
      parameters.put(3, mac.value);
      parameters.put(4, sixtyFour.value);
      parameters.put(5, silicon.value);
      parameters.put(6, steamReleaseDate.toString());
      parameters.put(7, gameId);
    }

    parameters.put(1, steamId);
    parameters.put(2, title);

    if (gameId == 0) {
      gameId = DBCXN.doInsertQuery(query, parameters);
    }
    else {
      DBCXN.doUpdateQuery(query, parameters);
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
      // Getting updated info for this game failed, but we're still going to
      // update this game's "steam_updated" value so we're not constantly
      // trying to update this game.
      save(true);
      return false;
    }

    setTitle(updatedGame.getName());
    try {
      setSteamReleaseDate(updatedGame.getRelease_date().getDateAsLocalDate());
    }
    catch (ParseException e) {
      System.err.print("Error parsing date while updating game from Steam.");
      System.exit(StatusCodes.GENERAL_OUTGOING_NETWORK_ERROR);
    }
    Boolean mac = updatedGame.getPlatforms().get("mac");
    if (mac != null) {
      if (mac) {
        setMac(GamePropStatus.YES);
        setSixtyFour(steam.getCatalinaStatus(this));
      }
      else {
        setMac(GamePropStatus.NO);
        setSixtyFour(GamePropStatus.UNCHECKED);
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
    String query = "SELECT * FROM games WHERE steam_updated < datetime('now', '-1 day') ORDER BY steam_updated ASC LIMIT ?";
    Map<Integer, Object> params = new HashMap<>();
    params.put(1, limit);

    ResultSet rs =  DBCXN.doSelectQuery(query, params);

    return createFromResultSet(rs);
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
        game.setSteamReleaseDate(LocalDate.parse(rs.getString("steam_release")));
        games.add(game);
      }
    }
    catch (Exception e) {
      System.err.println("Error trying to load game from DB row");
      System.exit(StatusCodes.GENERAL_SQL_ERROR);
    }
    return games.toArray(new Game[0]);
  }

  /**
   * Get games by release date.
   *
   * @param page The current page of results to fetch (zero-based)
   * @return A PagedQueryResult<Game> with the results.
   */
  public static PagedQueryResult<Game> getByReleaseDate(int page) {
    int offset = perPage * page;
    String selectQuery = "SELECT * FROM games WHERE mac <> ? ORDER BY steam_release DESC LIMIT ? OFFSET ?";
    String countQuery = "SELECT COUNT(*) FROM games WHERE mac <> ?";
    Map<Integer, Object> params = new HashMap<>();
    params.put(1, GamePropStatus.UNCHECKED.value);
    int count = DBCXN.getSingleIntResult(countQuery, params);

    params.put(2, perPage);
    params.put(3, offset);
    ResultSet rs = DBCXN.doSelectQuery(selectQuery, params);
    Game[] games = createFromResultSet(rs);


    return new PagedQueryResult<Game>(games, count, perPage, page);
  }
}
