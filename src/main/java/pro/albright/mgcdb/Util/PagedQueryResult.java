package pro.albright.mgcdb.Util;

public class PagedQueryResult<T> {

  private int totalResults;
  private int perPage;
  private int totalPages;
  private int currentPageZeroBased;
  private T[] results;

  public PagedQueryResult(T[] results, int totalResults, int perPage, int currentPageZeroBased) {
    this.totalResults = totalResults;
    this.perPage = perPage;
    this.currentPageZeroBased = currentPageZeroBased;
    this.results = results;

    totalPages = (int) Math.ceil(totalResults / perPage);
  }

  public int getTotalResults() {
    return totalResults;
  }

  public int getPerPage() {
    return perPage;
  }

  public int getTotalPages() {
    return totalPages;
  }

  public int getCurrentPageZeroBased() {
    return currentPageZeroBased;
  }

  public T[] getResults() {
    return results;
  }
}
