package pro.albright.mgcdb.SteamAPIModel;

import java.io.Serializable;

public class GetOwnedGamesResponseWrapper implements Serializable {
  private GetOwnedGamesResponse response;

  public GetOwnedGamesResponse getResponse() {
    return response;
  }

  public void setResponse(GetOwnedGamesResponse response) {
    this.response = response;
  }
}
