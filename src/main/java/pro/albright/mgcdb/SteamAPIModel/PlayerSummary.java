package pro.albright.mgcdb.SteamAPIModel;

public class PlayerSummary {
  long steamid;
  String personaname;
  String avatarhash;

  public long getSteamid() {
    return steamid;
  }

  public void setSteamid(long steamid) {
    this.steamid = steamid;
  }

  public String getPersonaname() {
    return personaname;
  }

  public void setPersonaname(String personaname) {
    this.personaname = personaname;
  }

  public String getAvatarhash() {
    return avatarhash;
  }

  public void setAvatarhash(String avatarhash) {
    this.avatarhash = avatarhash;
  }
}
