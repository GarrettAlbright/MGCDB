package pro.albright.mgcdb.SteamAPIModel;

public class GetPlayerSummariesResponse {

  private PlayerSummary[] players;

  public PlayerSummary[] getPlayers() {
    return players;
  }

  public void setPlayers(PlayerSummary[] players) {
    this.players = players;
  }
}
