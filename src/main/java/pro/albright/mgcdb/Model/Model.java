package pro.albright.mgcdb.Model;

import pro.albright.mgcdb.Util.DBCxn;
import pro.albright.mgcdb.Util.SteamCxn;

public class Model {

  protected static DBCxn dbCxn;
  protected static SteamCxn steamCxn;

  public static void setDbCxn(DBCxn dbCxn) {
    Model.dbCxn = dbCxn;
  }

  public static void setSteamCxn(SteamCxn steamCxn) {
    Model.steamCxn = steamCxn;
  }
}
