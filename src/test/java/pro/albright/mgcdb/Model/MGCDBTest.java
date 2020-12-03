package pro.albright.mgcdb.Model;

import junit.framework.TestCase;
import pro.albright.mgcdb.Util.Config;
import pro.albright.mgcdb.Util.DBCxn;
import pro.albright.mgcdb.Util.TestSteamCxn;

import java.sql.SQLException;

public class MGCDBTest extends TestCase {

  /**
   * The number of games to get info on for testing.
   */
  protected int gameCount = 25;

  protected DBCxn dbCxn;
  protected TestSteamCxn steamCxn;
  protected Config config;

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

    Model.setDbCxn(dbCxn);
    Model.setSteamCxn(steamCxn);
  }

  /**
   * Add a test case that does nothing so Maven doesn't complain about no tests
   * being found in this class when running from CLI. (It does not seem possible
   * to tell JUnit 3 to ignore an entire class…?)
   */
  public void testNop() {

  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    dbCxn.delete();
  }
}
