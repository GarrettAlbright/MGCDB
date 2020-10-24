package pro.albright.mgcdb.SteamAPIModel;

import java.io.Serializable;

public class GetPlayerSummariesResponseWrapper implements Serializable {

  private GetPlayerSummariesResponse response;

  public GetPlayerSummariesResponse getResponse() {
    return response;
  }

  public void setResponse(GetPlayerSummariesResponse response) {
    this.response = response;
  }
}
