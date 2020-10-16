package pro.albright.mgcdb.SteamAPIModel;

import java.util.Map;

public class GetAppDetailsApp {
  private String name;
  private Map<String, Boolean> platforms;
  private GetAppDetailsReleaseDate release_date;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<String, Boolean> getPlatforms() {
    return platforms;
  }

  public void setPlatforms(Map<String, Boolean> platforms) {
    this.platforms = platforms;
  }

  public GetAppDetailsReleaseDate getRelease_date() {
    return release_date;
  }

  public void setRelease_date(GetAppDetailsReleaseDate release_date) {
    this.release_date = release_date;
  }
}
