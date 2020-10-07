package pro.albright.mgcdb.SteamAPIModel;

import java.io.Serializable;

public class GetAppListApp implements Serializable {
  private int appid;
  private String name;
  private int last_modified;
  private int price_change_number;

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

  public int getLast_modified() {
    return last_modified;
  }

  public void setLast_modified(int last_modified) {
    this.last_modified = last_modified;
  }

  public int getPrice_change_number() {
    return price_change_number;
  }

  public void setPrice_change_number(int price_change_number) {
    this.price_change_number = price_change_number;
  }
}
