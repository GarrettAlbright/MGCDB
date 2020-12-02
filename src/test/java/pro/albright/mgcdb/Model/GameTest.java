package pro.albright.mgcdb.Model;

import junit.framework.Assert;
import junit.framework.TestCase;
import pro.albright.mgcdb.Util.Config;
import pro.albright.mgcdb.Util.DBCxn;
import pro.albright.mgcdb.Util.TestSteamCxn;

import java.sql.SQLException;
import java.time.LocalDate;

public class GameTest extends TestCase {

  protected DBCxn dbCxn;
  protected TestSteamCxn steamCxn;
  protected Config config;

  private int gameCount = 25;

  protected void setUp() throws SQLException {
    config = new Config();
    String testDbLocation = config.get("test_db_location");
    if (testDbLocation == null) {
      testDbLocation = config.get("db_location") + ".test";
    }

    dbCxn = new DBCxn(testDbLocation);
    dbCxn.createIfNotExists(true);
    dbCxn.initializeDb();

    steamCxn = new TestSteamCxn();

    Game.setDbCxn(dbCxn);
    Game.setSteamCxn(steamCxn);

    Game.getAndSaveNewGamesFromSteam(gameCount);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    dbCxn.delete();
  }

  public void testGetNewGamesFromSteam() {
    Assert.assertEquals(630, Game.getNewestGameSteamId());
  }

  public void testGetGamesToUpdate() {
    Game[] games = Game.getGamesToUpdateFromSteam(gameCount);
    Assert.assertEquals(gameCount, games.length);
    Assert.assertEquals(10, games[0].getSteamId());
    Assert.assertEquals(630, games[gameCount - 1].getSteamId());
  }

  public void testUpdateGamesFromSteam() {
    Game[] games = Game.updateGamesFromSteam(gameCount);
    int[] gamesToTestSteamIds = {10, 570, 630};
    int[] gamesToTestIds = Game.getGameIdsBySteamIds(gamesToTestSteamIds);
    if (gamesToTestIds.length < 3) {
      Assert.fail("Unexpected length to game IDs when trying to find by Steam IDs");
    }
    else {
      for (int gameId: gamesToTestIds) {
        Game game = Game.getById(gameId);
        if (game == null) {
          Assert.fail("Loading game by game ID failed");
        }
        else {
          switch (game.getSteamId()) {
            case 10:
              Assert.assertEquals("Counter-Strike", game.getTitle());
              Assert.assertEquals(Game.GamePropStatus.YES, game.getMac());
              Assert.assertEquals(Game.GamePropStatus.NO, game.getSixtyFour());
              Assert.assertEquals(LocalDate.parse("2000-11-01"), game.getSteamReleaseDate());
              break;
            case 570:
              Assert.assertEquals("Dota 2", game.getTitle());
              Assert.assertEquals(Game.GamePropStatus.YES, game.getMac());
              Assert.assertEquals(Game.GamePropStatus.YES, game.getSixtyFour());
              Assert.assertEquals(LocalDate.parse("2013-07-09"), game.getSteamReleaseDate());
              break;
            case 630:
              Assert.assertEquals("Alien Swarm", game.getTitle());
              Assert.assertEquals(Game.GamePropStatus.NO, game.getMac());
              Assert.assertEquals(Game.GamePropStatus.UNCHECKED, game.getSixtyFour());
              Assert.assertEquals(LocalDate.parse("2010-07-19"), game.getSteamReleaseDate());
              break;
            default:
              Assert.fail("Unexpected game loaded");
          }
        }
      }
    }
  }
}
