package pro.albright.mgcdb.Model;

import pro.albright.mgcdb.SteamAPIModel.PlayerSummary;
import pro.albright.mgcdb.Util.DBCXN;
import pro.albright.mgcdb.Util.PagedQueryResult;
import pro.albright.mgcdb.Util.StatusCodes;
import pro.albright.mgcdb.Util.SteamCxn;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class User implements Serializable {

  /**
   * The user ID in the local database.
   */
  private int userId;

  /**
   * The user ID in Steam.
   */
  private long steamId;

  /**
   * The Steam nickname of the user.
   */
  private String nickname;

  /**
   * The hash of the user's avatar image in Steam.
   */
  private String avatarHash;

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }

  public long getSteamId() {
    return steamId;
  }

  public void setSteamId(long steamId) {
    this.steamId = steamId;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getAvatarHash() {
    return avatarHash;
  }

  public void setAvatarHash(String avatarHash) {
    this.avatarHash = avatarHash;
  }

  /**
   * Authenticate a local user using their Steam ID, possibly creating the user
   * if it doesn't exist.
   *
   * "Authenticate" in this sense means to search for the user object and bump
   * the user's last auth date. It's expected one of this function's overloads
   * will be used to load/create the user after they log in with Steam.
   *
   * @param steamId The user's Steam ID.
   * @param createIfNotFound Whether to create a new user if it doesn't exist.
   * @return The user object.
   */
  public static User authWithSteamId(long steamId, boolean createIfNotFound) {
    User user = User.authWithSteamId(steamId);
    if (user == null && createIfNotFound) {
      // Create a new user object.
      // First, try to get details on the user from Steam.
      SteamCxn scxn = new SteamCxn();
      PlayerSummary ps = scxn.getUserInfo(steamId);
      if (ps == null) {
        // Couldn't get user details from Steam. Give up.
        return null;
      }
      user = new User();
      user.setSteamId(ps.getSteamid());
      user.setAvatarHash(ps.getAvatarhash());
      user.setNickname(ps.getPersonaname());
      user.save();
    }
    return user;
  }

  /**
   * Authenticate a local user using their Steam ID and bump their last auth
   * date.
   *
   * "Authenticate" in this sense means to search for the user object and bump
   * the user's last auth date. It's expected one of this function's overloads
   * will be used to load/create the user after they log in with Steam.
   *
   * @param steamId The user's Steam ID.
   * @return The user object.
   */
  public static User authWithSteamId(long steamId) {
    User user = User.getBySteamId(steamId);
    if (user != null) {
      user.bumpAuthDate();
    }
    return user;
  }

  /**
   * Load a user by Steam ID.
   *
   * This method should be called directly to get a user's info when they are
   * not logging in and we don't want to bump their last auth date.
   *
   * @param steamId The user's Steam ID.
   * @return The user object.
   */
  public static User getBySteamId(long steamId) {
    User user = null;
    Map<Integer, Object> params = new HashMap<>();
    params.put(1, steamId);
    ResultSet rs = DBCXN.doSelectQuery("SELECT * FROM users WHERE steam_user_id = ?", params);
    try {
      if (rs.next()) {
        user = User.createFromResultSet(rs);
      }
    }
    catch (SQLException throwables) {
      throwables.printStackTrace();
      System.exit(StatusCodes.GENERAL_SQL_ERROR);
    }
    return user;
  }

  /**
   * Load a user by userId.
   *
   * @param userId The userId.
   * @return The user.
   */
  public static User getById(int userId) {
    User user = null;
    Map<Integer, Object> params = new HashMap<>();
    params.put(1, userId);
    ResultSet rs = DBCXN.doSelectQuery("SELECT * FROM users WHERE user_id = ?", params);
    try {
      if (rs.next()) {
        user = User.createFromResultSet(rs);
      }
    }
    catch (SQLException throwables) {
      throwables.printStackTrace();
      System.exit(StatusCodes.GENERAL_SQL_ERROR);
    }
    return user;
  }

  /**
   * Create a user from a result set from a query.
   *
   * @param rs The result set.
   * @return The User object.
   */
  public static User createFromResultSet(ResultSet rs) {
    User user = new User();
    try {
      user.setUserId(rs.getInt("user_id"));
      user.setSteamId(rs.getLong("steam_user_id"));
      user.setNickname(rs.getString("nickname"));
      user.setAvatarHash(rs.getString("avatar_hash"));
      return user;
    }
    catch (SQLException throwables) {
      throwables.printStackTrace();
      System.exit(StatusCodes.GENERAL_SQL_ERROR);
    }
    return null;
  }

  /**
   * Build and return a URL for the Steam avatar URL for this user.
   *
   * This returns the URL for the smallest version of the avatar image. It's
   * created dynamically (inefficiently?) from the avatar hash value retrieved
   * from Steam for the user.
   *
   * @return The URL of the user's avatar image.
   */
  public String getAvatarUrl() {
    String firstTwo = avatarHash.substring(0, 2);
    return String.format("https://steamcdn-a.akamaihd.net/steamcommunity/public/images/avatars/%s/%s.jpg", firstTwo, avatarHash);
  }

  /**
   * Update the user's last authenticated value in the DB to the current time.
   */
  public void bumpAuthDate() {
    Map<Integer, Object> params = new HashMap<>();
    params.put(1, userId);
    DBCXN.doUpdateQuery("UPDATE users SET last_auth = CURRENT_TIMESTAMP where user_id = ?", params);
  }

  /**
   * Update the list of games that this user owns.
   *
   * Note that we don't instantiate the Ownership or Game instances and just
   * deal with the respective IDs since it's possible that a user will own
   * several hundred games.
   */
  public void updateOwnedGames() {
    SteamCxn steamCxn = new SteamCxn();
    int[] steamGameIds = steamCxn.getOwnedGamesInSteam(steamId);
    // Convert array to list so we can use .contains because apparently Java
    // doesn't have something similar for regular arrays… oof
    // https://stackoverflow.com/a/34428978/11023
    List<Integer> steamOwnedGameIds = Arrays.stream(Game.getGameIdsBySteamIds(steamGameIds)).boxed().collect(Collectors.toUnmodifiableList());
    List<Integer> dbOwnedGameIds = Arrays.stream(Ownership.getOwnedGamesInDb(userId)).boxed().collect(Collectors.toUnmodifiableList());

    // Find Steam owned games that aren't in the DB yet
    for (Integer steamOwnedGameId : steamOwnedGameIds) {
      if (!dbOwnedGameIds.contains(steamOwnedGameId)) {
        new Ownership(userId, steamOwnedGameId).save();
      }
    }

    // Now find games owned in the DB which weren't found in Steam
    for (Integer dbOwnedGameId : dbOwnedGameIds) {
      if (!steamOwnedGameIds.contains(dbOwnedGameId)) {
        Ownership.delete(userId, dbOwnedGameId);
      }
    }
  }

  public PagedQueryResult<Game> getOwnedGames(int page) {
    String selectQuery = "SELECT g.* FROM ownership o INNER JOIN games g ON o.game_id = g.game_id WHERE o.user_id = ? ORDER BY g.steam_release DESC LIMIT ? OFFSET ?";
    String countQuery = "SELECT COUNT(*) FROM ownership o INNER JOIN games g ON o.game_id = g.game_id WHERE o.user_id = ?";
    Map<Integer, Object> params = new HashMap<>();
    params.put(1, userId);
    int count = DBCXN.getSingleIntResult(countQuery, params);

    params.put(2, Game.perPage);
    params.put(3, page * Game.perPage);
    ResultSet rs = DBCXN.doSelectQuery(selectQuery, params);
    Game[] games = Game.createFromResultSet(rs);
    return new PagedQueryResult<>(games, count, Game.perPage, page);
  }

  /**
   * Save the user's information to the database.
   */
  public void save() {
    Map<Integer, Object> params = new HashMap<>();
    String query;

    if (userId == 0) {
      // This is a new user.
      query = "INSERT INTO users (steam_user_id, nickname, avatar_hash) VALUES (?, ?, ?)";
      params.put(1, steamId);
      params.put(2, nickname);
      params.put(3, avatarHash);
      userId = DBCXN.doInsertQuery(query, params);
    }
    else {
      // This is an existing user that we're updating.
      // I can't anticipate a case where we have to update a user's Steam ID
      query = "UPDATE users SET nickname = ?, avatar_hash = ? WHERE user_id = ?";
      params.put(1, nickname);
      params.put(2, userId);
      params.put(3, avatarHash);
      DBCXN.doUpdateQuery(query, params);
    }
  }
}
