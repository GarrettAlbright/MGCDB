package pro.albright.mgcdb.SteamAPIModel;

import java.io.Serializable;

public class GetAppListResponseWrapper implements Serializable {
  private GetAppListResponse response;

  public GetAppListResponse getResponse() {
    return response;
  }

  public void setResponse(GetAppListResponse response) {
    this.response = response;
  }
}
