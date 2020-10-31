package pro.albright.mgcdb.Model;

import pro.albright.mgcdb.Util.DBCXN;
import pro.albright.mgcdb.Util.StatusCodes;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to represent an instance of a User owning a Game.
 *
 * Note that for performance reasons, we don't really instantiate these much, or
 * instantiate Game objects, when we're dealing with ownership. That's because
 * it's very likely that a user will own hundreds of Steam games. So for
 * performance reasons, we typically just deal with user and game IDs directly.
 */
public class Ownership implements java.io.Serializable {

  private int ownershipId;
  private int userId;
  private int gameId;

  public int getOwnershipId() {
    return ownershipId;
  }

  public void setOwnershipId(int ownershipId) {
    this.ownershipId = ownershipId;
  }

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }

  public int getGameId() {
    return gameId;
  }

  public void setGameId(int gameId) {
    this.gameId = gameId;
  }

  public Ownership(int userId, int gameId) {
    this.userId = userId;
    this.gameId = gameId;
  }

  /**
   * Get all gameIds of games owned by a user by user ID in the DB.
   */
  static public int[] getOwnedGamesInDb(int userId) {
//    String query = "SELECT games.* FROM ownership " +
//      "INNER JOIN games ON game.game_id = ownership.game_id " +
//      "WHERE user_id = ?";
    String query = "SELECT game_id FROM ownership WHERE user_id = ?";
    Map<Integer, Object> params = new HashMap<>();
    params.put(1, userId);
    ResultSet rs = DBCXN.doSelectQuery(query, params);

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
   * Delete an Ownership instance by userId and gameId.
   *
   * @param userId
   * @param gameId
   */
  static public void delete(int userId, int gameId) {
    String query = "DELETE FROM ownership WHERE user_id = ? AND game_id = ?";
    Map<Integer, Object> params = new HashMap<>();
    params.put(1, userId);
    params.put(2, gameId);
    DBCXN.doUpdateQuery(query, params);
  }

  /**
   * Save an Ownership.
   *
   * Note that at least currently we don't anticipate an ownership instance
   * being edited. Either User X owns Game Y or they do not. So we don't do
   * the "if we have a game ID, do an update query" thing like we do in the
   * save() method on our other entities.
   */
  public void save() {
    String query = "INSERT INTO ownership (user_id, game_id) VALUES (?, ?)";
    Map<Integer, Object> params = new HashMap<>();
    params.put(1, userId);
    params.put(2, gameId);
    ownershipId = DBCXN.doInsertQuery(query, params);
  }
}
