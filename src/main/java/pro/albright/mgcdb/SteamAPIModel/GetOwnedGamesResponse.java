package pro.albright.mgcdb.SteamAPIModel;

public class GetOwnedGamesResponse {
  private int game_count;
  private OwnedGameInstance[] games;

  public int getGame_count() {
    return game_count;
  }

  public void setGame_count(int game_count) {
    this.game_count = game_count;
  }

  public OwnedGameInstance[] getGames() {
    return games;
  }

  public void setGames(OwnedGameInstance[] games) {
    this.games = games;
  }
}
