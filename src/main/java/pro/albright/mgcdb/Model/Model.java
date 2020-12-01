package pro.albright.mgcdb.Model;

import pro.albright.mgcdb.Util.DBCxn;
import pro.albright.mgcdb.Util.SteamCxn;

public class Model {

  /**
   * An injected DBCxn utility class instance..
   */
  protected static DBCxn dbCxn;

  /**
   * An injected SteamCxn utility class instance.
   */
  protected static SteamCxn steamCxn;

  public static void setDbCxn(DBCxn dbCxn) {
    Model.dbCxn = dbCxn;
  }

  public static void setSteamCxn(SteamCxn steamCxn) {
    Model.steamCxn = steamCxn;
  }
}
