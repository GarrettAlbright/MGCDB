package pro.albright.mgcdb.Model;

import pro.albright.mgcdb.Util.DBCXN;
import pro.albright.mgcdb.Util.StatusCodes;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to represent an Ownership - an instance of a User owning a Game.
 *
 * Note that for performance reasons, we don't really instantiate these much, or
 * instantiate Game objects, when we're dealing with ownership. That's because
 * it's very likely that a user will own hundreds of Steam games. So for
 * performance reasons, we typically just deal with user and game IDs directly.
 */
public class Ownership implements java.io.Serializable {

  /**
   * Our ID for this Ownership.
   */
  private int ownershipId;

  /**
   * Our User ID for the user.
   */
  private int userId;

  /**
   * Our Game ID for the Game.
   */
  private int gameId;

  /**
   * If the user has voted on the game's 64-bit compatibility, that vote.
   */
  private Vote vote;

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

  public Vote getVote() {
    return vote;
  }

  public void setVote(Vote vote) {
    this.vote = vote;
  }

  public Ownership(int userId, int gameId) {
    this.userId = userId;
    this.gameId = gameId;
  }

  /**
   * Get an Ownership instance by ID.
   *
   * @param ownershipId
   * @return
   */
  public static Ownership getById(int ownershipId) {
    String query = "SELECT * FROM ownership WHERE ownership_id = ?";
    Map<Integer, Object> params = new HashMap<>();
    params.put(1, ownershipId);
    ResultSet rs = DBCXN.doSelectQuery(query, params);
    try {
      if (rs.next()) {
        return Ownership.createFromResultSet(rs);
      }
    }
    catch (SQLException e) {
      return null;
    }
    return null;
  }

  /**
   * Create an Ownership instance from a resultset.
   * @param rs
   * @return
   */
  public static Ownership createFromResultSet(ResultSet rs) {
    Ownership ownership = null;
    try {
      ownership = new Ownership(rs.getInt("user_id"), rs.getInt("game_id"));
      ownership.setOwnershipId(rs.getInt("ownership_id"));

      // Again, gross exception handling to see if vote info is in the result
      // set and add it if so
      // https://stackoverflow.com/q/3599861/11023
      try {
        int voteId = rs.getInt("vote_id");
        if (voteId != 0) {
          ownership.setVote(Vote.createFromResultSet(rs));
        }
      }
      catch (SQLException e) {
        // Oh well
      }
    }
    catch (Exception e) {
      return null;
    }
    return ownership;
  }

  /**
   * Get all gameIds of games owned by a user by user ID in the DB.
   *
   * Useful for cases where we just want to handle game IDs rather than full
   * Game objects for performance reasons.
   *
   * @param userId Our ID for the user.
   * @return An array of game IDs that the user owns.
   */
  static public int[] getOwnedGamesInDb(int userId) {
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
