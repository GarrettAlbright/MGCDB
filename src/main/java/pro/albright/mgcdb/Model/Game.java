package pro.albright.mgcdb.Model;

import org.apache.commons.lang.StringUtils;
import pro.albright.mgcdb.SteamAPIModel.GetAppDetailsApp;
import pro.albright.mgcdb.SteamAPIModel.GetAppDetailsReleaseDate;
import pro.albright.mgcdb.SteamAPIModel.GetAppListApp;
import pro.albright.mgcdb.Util.DBCXN;
import pro.albright.mgcdb.Util.PagedQueryResult;
import pro.albright.mgcdb.Util.StatusCodes;
import pro.albright.mgcdb.Util.SteamCxn;

import java.sql.*;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.Date;

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
   * haven't got an answer from the Steam API for its value yet. This value is
   * stored in the database; hence the extra steps for defining absolute values
   * and instantiating from them.
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
   * An enum for stating how to filter a list of games. This is not stored in
   * the database so we don't care about absolute values.
   */
  public enum GameFilterMode {
    // All games
    ALL,
    // Only Mac games
    MAC,
    // Only Catalina-compatible (64-bit) Mac games
    CATALINA
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

  /**
   * The current user's ownership for this game.
   *
   * This is relevant for user-owned game lists. Outside that it will be null.
   */
  private Ownership ownership;

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

  public Ownership getOwnership() {
    return ownership;
  }

  public void setOwnership(Ownership ownership) {
    this.ownership = ownership;
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
      parameters.put(6, steamReleaseDate == null ? null : steamReleaseDate.toString());
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
      // This happens for 8980 (Borderlands Game of the Year) which has a blank
      // release date in the JSON for some reason.
      System.err.printf("Error parsing date for %d while updating game from Steam. Date as string is \"%s\"%n", this.getSteamId(), updatedGame.getRelease_date().getDate());
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
        String steamRelease = rs.getString("steam_release");
        if (!steamRelease.isEmpty()) {
          // The release date can be empty when the data from Steam did not have
          // a release date (the field in the JSON was an empty string).
          game.setSteamReleaseDate(LocalDate.parse(steamRelease));
        }

        // If ownership info was included in the result set, create that info.
        // Using exceptions for this is gross but other approaches are even
        // grosser.
        // https://stackoverflow.com/q/3599861/11023
        try {
          int ownershipId = rs.getInt("ownership_id");
          if (ownershipId != 0) {
            game.setOwnership(Ownership.createFromResultSet(rs));
          }
        }
        catch (SQLException e) {
          // Oh well
        }
        games.add(game);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(StatusCodes.GENERAL_SQL_ERROR);
    }
    return games.toArray(new Game[0]);
  }

  /**
   * Get our own gameId values for a given array of steamId values.
   *
   * @param steamIds An array of steamId values.
   * @return An array of gameIds.
   */
  public static int[] getGameIdsBySteamIds(int[] steamIds) {
    // The SQLite driver doesn't support passing an array of values for an IN()
    // query (I guess only Postgres's does?) so we build a string with a bunch
    // of question marks we bind into later.
    int steamIdsLength = steamIds.length;
    // https://stackoverflow.com/a/49065337/11023
    String questionMarks = "?, ".repeat(steamIdsLength).substring(0, (steamIdsLength * 3) - 2);
    String query = "SELECT game_id FROM games WHERE steam_id IN (" + questionMarks +")";
    Map<Integer, Object> params = new HashMap<>();
    int paramIdx = 1;
    for (int steamId : steamIds) {
      params.put(paramIdx++, steamId);
    }
    ResultSet rs = DBCXN.doSelectQuery(query, params);

    // Note that the SQLite driver doesn't let us do the cursor back and forth
    // by which we can get the count of rows in a ResultSet ("SQLite only
    // supports TYPE_FORWARD_ONLY cursors"). So… sigh… we'll use a List and
    // convert it later.
    ArrayList<Integer> gameIds = new ArrayList<>();
    try {
      while (rs.next()) {
        gameIds.add(rs.getInt("game_id"));
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(StatusCodes.GENERAL_SQL_ERROR);
    }

    // Java collections are just so much fun you know.
    int[] returnVals = new int[gameIds.size()];
    for (int idx = 0; idx < returnVals.length; idx++) {
      returnVals[idx] = gameIds.get(idx);
    }
    return returnVals;
  }

  /**
   * Get game by our game ID.
   * @param gameId The game id.
   * @return The Game.
   */
  public static Game getById(int gameId) {
    String query = "SELECT * FROM games WHERE game_id = ?";
    Map<Integer, Object> params = new HashMap<>();
    params.put(1, gameId);
    ResultSet rs = DBCXN.doSelectQuery(query, params);
    Game[] games =  Game.createFromResultSet(rs);
    return games.length > 0 ? games[0] : null;
  }

  /**
   * Get games by release date.
   *
   * @param page The current page of results to fetch (zero-based)
   * @return A PagedQueryResult<Game> with the results.
   */
  public static PagedQueryResult<Game> getByReleaseDate(int page, GameFilterMode filter) {
    int offset = perPage * page;
    Map<Integer, Object> params = new HashMap<>();
    StringBuilder paramsSb = new StringBuilder(" FROM games WHERE ");

    if (filter == GameFilterMode.MAC) {
      // Only Mac games
      paramsSb.append("mac = ? ");
      params.put(1, GamePropStatus.YES.value);
    }
    else if (filter == GameFilterMode.CATALINA) {
      // Only Catalina/64-bit games
      paramsSb.append("sixtyfour = ? ");
      params.put(1, GamePropStatus.YES.value);
    }
    else {
      // All games
      paramsSb.append("mac <> ? ");
      params.put(1, GamePropStatus.UNCHECKED.value);
    }

    String paramsPart = paramsSb.toString();

    String selectQuery = "SELECT *" + paramsPart + "ORDER BY steam_release DESC LIMIT ? OFFSET ?";
    String countQuery = "SELECT COUNT(*)" + paramsPart;
    int count = DBCXN.getSingleIntResult(countQuery, params);

    params.put(2, perPage);
    params.put(3, offset);
    ResultSet rs = DBCXN.doSelectQuery(selectQuery, params);
    Game[] games = createFromResultSet(rs);

    return new PagedQueryResult<Game>(games, count, perPage, page);
  }
}
