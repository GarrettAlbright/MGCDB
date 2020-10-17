package pro.albright.mgcdb.Util;

public class PagedQueryResult<T> {

  private int totalResults;
  private int perPage;
  private int totalPages;
  private int currentPageZeroBased;
  private T[] results;

  public PagedQueryResult(T[] results, int totalResults, int perPage, int currentPageZeroBased) {
//    this(results, totalResults);
    this.results = results;
    this.totalResults = totalResults;
    this.perPage = perPage;
    this.currentPageZeroBased = currentPageZeroBased;

    totalPages = (int) Math.ceil(totalResults / perPage);
  }

//  public PagedQueryResult(T[] results, int totalResults) {
//    this.results = results;
//    this.totalResults = totalResults;
//  }

  public int getTotalResults() {
    return totalResults;
  }

  public int getPerPage() {
    return perPage;
  }

  public void setPerPage(int perPage) {
    this.perPage = perPage;
  }

  public int getTotalPages() {
    return totalPages;
  }

  public int getCurrentPageZeroBased() {
    return currentPageZeroBased;
  }

  public void setCurrentPageZeroBased(int currentPageZeroBased) {
    this.currentPageZeroBased = currentPageZeroBased;
  }

  public T[] getResults() {
    return results;
  }
}
