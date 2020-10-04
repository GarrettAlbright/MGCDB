package pro.albright.mgcdb.SteamAPIModel;

import java.io.Serializable;

public class GetAppListResponse implements Serializable {
  private boolean have_more_results;
  private int last_appid;
  private SteamApp[] apps;

  public boolean isHave_more_results() {
    return have_more_results;
  }

  public void setHave_more_results(boolean have_more_results) {
    this.have_more_results = have_more_results;
  }

  public int getLast_appid() {
    return last_appid;
  }

  public void setLast_appid(int last_appid) {
    this.last_appid = last_appid;
  }

  public SteamApp[] getApps() {
    return apps;
  }

  public void setApps(SteamApp[] apps) {
    this.apps = apps;
  }
}
