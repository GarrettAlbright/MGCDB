package pro.albright.mgcdb.Model;

import pro.albright.mgcdb.Util.DBCXN;
import pro.albright.mgcdb.Util.StatusCodes;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * A class representing a vote on Catalina compatibility.
 */
public class Vote {

  /**
   * The ID of the vote.
   */
  private int voteId;

  /**
   * The ownership instance matching the user/game pair that this vote
   * represents. We don't let users vote on games they don't actually own.
   */
  private int ownershipId;

  /**
   * The vote. True for yes, and false for no.
   */
  private boolean vote;

  public int getVoteId() {
    return voteId;
  }

  public void setVoteId(int voteId) {
    this.voteId = voteId;
  }

  public int getOwnershipId() {
    return ownershipId;
  }

  public void setOwnershipId(int ownershipId) {
    this.ownershipId = ownershipId;
  }

  public boolean isVote() {
    return vote;
  }

  public void setVote(boolean vote) {
    this.vote = vote;
  }

  /**
   * Returns a vote by ownership ID.
   *
   * Note that this will have to be adapted if/when we allow more than one vote
   * type on a game/ownership in the future.
   *
   * @param ownershipId
   * @return
   */
  public static Vote getByOwnershipId(int ownershipId) {
    String query = "SELECT * FROM votes WHERE ownership_id = ?";
    Map<Integer, Object> params = new HashMap<>();
    params.put(1, ownershipId);
    ResultSet rs = DBCXN.doSelectQuery(query, params);
    return Vote.createFromResultSet(rs);
  }

  /**
   * Create a Vote instance from a result set.
   * @param rs
   * @return
   */
  public static Vote createFromResultSet(ResultSet rs) {
    Vote vote = null;
    try {
      vote = new Vote();
      vote.setVoteId(rs.getInt("vote_id"));
      vote.setOwnershipId(rs.getInt("ownership_id"));
      vote.setVote(rs.getBoolean("vote"));
    }
    catch (SQLException throwables) {
      return null;
    }
    return vote;
  }

  /**
   * Delete this Vote.
   */
  public void delete() {
    if (voteId == 0) {
      return;
    }
    String query = "DELETE FROM votes WHERE vote_id = ?";
    Map<Integer, Object> params = new HashMap<>();
    params.put(1, voteId);
    DBCXN.doUpdateQuery(query, params);
  }

  /**
   * Save this Vote.
   */
  public void save() {
    Map<Integer, Object> params = new HashMap<>();
    String query;

    if (voteId == 0) {
      query = "INSERT INTO votes (ownership_id, vote) VALUES (?, ?)";
      params.put(1, ownershipId);
      params.put(2, vote ? 1 : 0);
      voteId = DBCXN.doInsertQuery(query, params);
    }
    else {
      // I don't see the case in which the ownership ID of an existing vote
      // could be changed.
      query = "UPDATE votes SET vote = ? WHERE vote_id = ?";
      params.put(1, vote ? 1 : 0);
      params.put(2, voteId);
      DBCXN.doUpdateQuery(query, params);
    }
  }
}
