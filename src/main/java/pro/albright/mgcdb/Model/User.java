package pro.albright.mgcdb.Model;

import pro.albright.mgcdb.SteamAPIModel.PlayerSummary;
import pro.albright.mgcdb.Util.DBCXN;
import pro.albright.mgcdb.Util.StatusCodes;
import pro.albright.mgcdb.Util.SteamCxn;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class User implements Serializable {

  private int userId;
  private long steamId;
  private String nickname;
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

  public static User authWithSteamId(long steamId, boolean createIfNotFound) {
    User user = User.authWithSteamId(steamId);
    if (user == null && createIfNotFound) {
      SteamCxn scxn = new SteamCxn();
      PlayerSummary ps = scxn.getUserInfo(steamId);
      if (ps == null) {
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

  public static User authWithSteamId(long steamId) {
    User user = User.getBySteamId(steamId);
    if (user != null) {
      user.bumpAuthDate();
    }
    return user;
  }

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

  public String getAvatarUrl() {
    String firstTwo = avatarHash.substring(0, 2);
    return String.format("https://steamcdn-a.akamaihd.net/steamcommunity/public/images/avatars/%s/%s.jpg", firstTwo, avatarHash);
  }

  public void bumpAuthDate() {
    Map<Integer, Object> params = new HashMap<>();
    params.put(1, userId);
    DBCXN.doUpdateQuery("UPDATE users SET last_auth = CURRENT_TIMESTAMP where user_id = ?", params);
  }

  public void save() {
    Map<Integer, Object> params = new HashMap<>();
    String query;
    int paramIdx = 1;

    if (userId == 0) {
      query = "INSERT INTO users (steam_user_id, nickname, avatar_hash) VALUES (?, ?, ?)";
      params.put(1, steamId);
      params.put(2, nickname);
      params.put(3, avatarHash);
      userId = DBCXN.doInsertQuery(query, params);
    }
    else {
      // I can't anticipate a case where we have to update a user's Steam ID
      query = "UPDATE users SET nickname = ?, avatar_hash = ? WHERE user_id = ?";
      params.put(1, nickname);
      params.put(2, userId);
      params.put(3, avatarHash);
      DBCXN.doUpdateQuery(query, params);
    }
  }
}
