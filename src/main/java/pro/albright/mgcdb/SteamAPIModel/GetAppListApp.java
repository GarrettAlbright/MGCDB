package pro.albright.mgcdb.SteamAPIModel;

import java.io.Serializable;

public class GetAppListApp implements Serializable {
  private int appid;
  private String name;

  public int getAppid() {
    return appid;
  }

  public void setAppid(int appid) {
    this.appid = appid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
