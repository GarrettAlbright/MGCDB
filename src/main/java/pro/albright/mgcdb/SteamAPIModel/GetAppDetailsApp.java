package pro.albright.mgcdb.SteamAPIModel;

import java.util.Map;

public class GetAppDetailsApp {
  private String name;
  private Map<String, Boolean> platforms;

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
}
