package pro.albright.mgcdb.Model;

import junit.framework.Assert;

import java.sql.SQLException;

public class UserTest extends MGCDBTest {

  protected Long steamId = Long.parseLong("76561198024933199");

  protected void setUp() throws SQLException {
    super.setUp();
    User.authWithSteamId(steamId, true);
  }

  public void testGetBySteamId() {
    Assert.assertNotNull(User.getBySteamId(steamId));
  }

  public void testUpdateOwnedGames() {
    Game.getAndSaveNewGamesFromSteam(gameCount);
    Game[] games = Game.updateGamesFromSteam(gameCount);

    User me = User.getBySteamId(steamId);
    me.updateOwnedGames();
    int[] gameIds = Ownership.getOwnedGamesInDb(me.getUserId());
    Assert.assertEquals(9, gameIds.length);
    Assert.assertNotNull(Ownership.get(me.getUserId(), gameIds[0]));
  }

  public void testVotes() {
    int[] steamIds = {20, 360};
    int[] gameIds = Game.getGameIdsBySteamIds(steamIds);
    if (steamIds.length != 2) {
      Assert.fail("Unexpected number of gameIds found");
      return;
    }

    User me = User.getBySteamId(steamId);
    me.updateOwnedGames();

    int[] ownedIds = Ownership.getOwnedGamesInDb(me.getUserId());

    for (int gameId : gameIds) {
      Game game = Game.getById(gameId);
      if (game == null) {
        Assert.fail("Couldn't load a game for test");
        return;
      }

      Assert.assertEquals(0, game.getVoteCount());

      Ownership o = Ownership.get(me.getUserId(), game.getGameId());
      if (o == null) {
        Assert.fail("Couldn't load an ownership for test");
        return;
      }

      Vote v = new Vote();
      v.setOwnershipId(o.getOwnershipId());

      // Vote Yes on game 20, no on 360
      v.setVote(game.getSteamId() == 20);
      v.save();

      game.updateVoteCounts();

      Vote ov = Vote.getByOwnershipId(o.getOwnershipId());

      Assert.assertEquals(1, game.getVoteCount());
      if (game.getSteamId() == 20) {
        Assert.assertEquals(1, game.getYesVoteCount());
        Assert.assertEquals(100, game.getYesVoteAsPercentage());
        Assert.assertTrue(ov.getVote());
      }
      else {
        Assert.assertEquals(0, game.getYesVoteCount());
        Assert.assertEquals(0, game.getYesVoteAsPercentage());
        Assert.assertFalse(ov.getVote());

        // Try deleting the vote
        ov.delete();

        Vote ov2 = Vote.getByOwnershipId(o.getOwnershipId());
        Assert.assertNull(ov2);

        game.updateVoteCounts();
        Assert.assertEquals(0, game.getVoteCount());
      }
    }
  }
}
