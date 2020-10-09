package pro.albright.mgcdb.SteamAPIModel;

public class GetAppDetailsResponse {
  private boolean success;
  private GetAppDetailsApp data;

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public GetAppDetailsApp getData() {
    return data;
  }

  public void setData(GetAppDetailsApp data) {
    this.data = data;
  }
}
