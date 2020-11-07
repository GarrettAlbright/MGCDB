package pro.albright.mgcdb.Util;

/**
 * A class for holding information used for listings of results expected to
 * go across several pages.
 *
 * @param <T> The object type in the results.
 */
public class PagedQueryResult<T> {
  /**
   * The number of total results (eg, the result of a SELECT COUNT(*) query
   * with the same parameters as the "normal" query but with no LIMIT/OFFSET
   * values).
   */
  private int totalResults;
  /**
   * The number of results we're showing per page.
   */
  private int perPage;
  /**
   * The current page that the visitor is browsing, starting from 0 (the
   * visitor will see this as page 1, so this should be offset before showing
   * to the user).
   */
  private int currentPageZeroBased;
  /**
   * The actual results from teh query.
   */
  private T[] results;

  public PagedQueryResult(T[] results, int totalResults, int perPage, int currentPageZeroBased) {
    this.results = results;
    this.totalResults = totalResults;
    this.perPage = perPage;
    this.currentPageZeroBased = currentPageZeroBased;
  }

  public int getTotalResults() {
    return totalResults;
  }

  public int getPerPage() {
    return perPage;
  }

  public void setPerPage(int perPage) {
    this.perPage = perPage;
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

  /**
   * The total number of pages this query could generate.
   *
   * @return The number of pages.
   */
  public int getTotalPages() {
    // Gotta cast the params to float becase Java is one of those lovely
    // languages which will always return an int when you divide with ints.
    return (int) Math.ceil((float) totalResults / (float) perPage);
  }
}
