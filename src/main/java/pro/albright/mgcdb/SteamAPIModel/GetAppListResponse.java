package pro.albright.mgcdb.SteamAPIModel;

import java.io.Serializable;

public class GetAppListResponse implements Serializable {
  private GetAppListApp[] apps;

  public GetAppListApp[] getApps() {
    return apps;
  }

  public void setApps(GetAppListApp[] apps) {
    this.apps = apps;
  }
}
