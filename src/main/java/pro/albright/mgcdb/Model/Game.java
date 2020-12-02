package pro.albright.mgcdb.Model;

import pro.albright.mgcdb.SteamAPIModel.GetAppDetailsApp;
import pro.albright.mgcdb.SteamAPIModel.GetAppListApp;
import pro.albright.mgcdb.Util.DBCxn;
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
public class Game extends Model implements java.io.Serializable {

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

  /**
   * The number of votes on 64-bit compatibility this game has received.
   *
   * This may be unset where irrelevant.
   */
  private int voteCount;

  /**
   * Of the number of votes this game has received, the number that were "yes"
   * votes.
   */
  private int yesVoteCount;

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

  public int getVoteCount() {
    return voteCount;
  }

  public void setVoteCount(int voteCount) {
    this.voteCount = voteCount;
  }

  public int getYesVoteCount() {
    return yesVoteCount;
  }

  public void setYesVoteCount(int yesVoteCount) {
    this.yesVoteCount = yesVoteCount;
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
    return dbCxn.getSingleIntResult("SELECT MAX(steam_id) AS max FROM games", null);
  }

  /**
   * Get new games from the Steam DB.
   *
   * @param limit Max number of new games to fetch.
   * @return Array of Games.
   */
  public static Game[] getAndSaveNewGamesFromSteam(int limit) {
    int lastAppId = Game.getNewestGameSteamId();
    Game[] games = steamCxn.getNewGames(lastAppId, limit);
    for (Game game : games) {
      game.save();
    }
    return games;
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
    return dbCxn.getSingleIntResult(query, parameters) > 0;
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
      gameId = dbCxn.doInsertQuery(query, parameters);
    }
    else {
      dbCxn.doUpdateQuery(query, parameters);
    }
  }

  /**
   * Update this game with information from Steam.
   *
   * @return True if the game was able to be successfully updated.
   */
  public boolean updateFromSteam() {
    GetAppDetailsApp updatedGame = steamCxn.getUpdatedGameDetails(this);
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
      return false;
    }
    Boolean mac = updatedGame.getPlatforms().get("mac");
    if (mac != null) {
      if (mac) {
        setMac(GamePropStatus.YES);
        setSixtyFour(steamCxn.getCatalinaStatus(this));
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
  public static Game[] getGamesToUpdateFromSteam(int limit) {
    String query = "SELECT * FROM games WHERE steam_updated < datetime('now', '-1 day') ORDER BY steam_updated ASC LIMIT ?";
    Map<Integer, Object> params = new HashMap<>();
    params.put(1, limit);

    ResultSet rs = dbCxn.doSelectQuery(query, params);

    return createFromResultSet(rs);
  }

  public static Game[] updateGamesFromSteam(int limit) {
    Game[] gamesToUpdate = getGamesToUpdateFromSteam(limit);
    for (Game game : gamesToUpdate) {
      game.updateFromSteam();
    }
    return gamesToUpdate;
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
        game.setCreated(dbCxn.parseTimestamp(rs.getString("created")));
        game.setUpdated(dbCxn.parseTimestamp(rs.getString("updated")));
        game.setSteamUpdated(dbCxn.parseTimestamp(rs.getString("steam_updated")));
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

        // Same as above with vote counts
        try {
          game.setVoteCount(rs.getInt("vote_count"));
          game.setYesVoteCount(rs.getInt("yes_vote_count"));
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
    ResultSet rs = dbCxn.doSelectQuery(query, params);

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
    ResultSet rs = dbCxn.doSelectQuery(query, params);
    Game[] games =  Game.createFromResultSet(rs);
    return games.length > 0 ? games[0] : null;
  }

  /**
   * Get games by release date.
   * @param page The current page of results to fetch (zero-based)
   * @param filter How to filter the list.
   * @return A PagedQueryResult<Game> with the results.
   */
  public static PagedQueryResult<Game> getByReleaseDate(int page, GameFilterMode filter) {
    return getByReleaseDate(page, filter, null);
  }

    /**
     * Get games by release date.
     *
     * @param page The current page of results to fetch (zero-based)
     * @param filter How to filter the list.
     * @param queryParts An array of game name query parts.
     * @return A PagedQueryResult<Game> with the results.
     */
  public static PagedQueryResult<Game> getByReleaseDate(int page, GameFilterMode filter, String[] queryParts) {
    int offset = perPage * page;
    Map<Integer, Object> params = new HashMap<>();
    StringBuilder where = new StringBuilder("");
    int paramsIdx = 1;

    if (filter == GameFilterMode.MAC) {
      // Only Mac games
      where.append(" AND g.mac = ? ");
      params.put(paramsIdx++, GamePropStatus.YES.value);
    }
    else if (filter == GameFilterMode.CATALINA) {
      // Only Catalina/64-bit games
      where.append(" AND g.sixtyfour = ? ");
      params.put(paramsIdx++, GamePropStatus.YES.value);
    }
    else {
      // All games
      where.append(" AND g.mac <> ? ");
      params.put(paramsIdx++, GamePropStatus.UNCHECKED.value);
    }

    if (queryParts != null && queryParts.length > 0) {
      int partsCount = Math.min(queryParts.length, 5);
      for (int pIdx = 0; pIdx < partsCount; pIdx++) {
        where.append(" AND g.title LIKE ? ");
        params.put(paramsIdx++, "%" + queryParts[pIdx] + "%");
      }
    }

    String countQuery = "SELECT COUNT(*) FROM games g WHERE g.steam_release <= CURRENT_TIMESTAMP" + where;
    int count = dbCxn.getSingleIntResult(countQuery, params);

    String selectQuery = "SELECT g.*, COUNT(v.vote_id) AS vote_count, SUM(v.vote) AS yes_vote_count " +
      "FROM games g LEFT JOIN ownership o USING (game_id) " +
      "LEFT JOIN votes v USING (ownership_id) " +
      "WHERE g.steam_release <= CURRENT_TIMESTAMP" + where +
      "GROUP BY 1 ORDER BY g.steam_release DESC LIMIT ? OFFSET ?";
    params.put(paramsIdx++, perPage);
    params.put(paramsIdx++, offset);
    ResultSet rs = dbCxn.doSelectQuery(selectQuery, params);
    Game[] games = createFromResultSet(rs);

    return new PagedQueryResult<Game>(games, count, perPage, page);
  }

  /**
   * Return the yes vote count for this game as a percentage of total votes.
   *
   * The percentage is rounded to the nearest integer.
   *
   * @return The percentage of yes votes.
   */
  public int getYesVoteAsPercentage() {
    // Avoid division by zero
    if (voteCount == 0) {
      return 0;
    }
    float ratio = (float) yesVoteCount / (float) voteCount;
    return Math.round(ratio * 100);
  }
}
