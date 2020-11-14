package pro.albright.mgcdb.Util;

import org.apache.commons.lang.StringEscapeUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * A class to provide some functions to escape values in templates.
 */
public class Escape {

  /**
   * HTML escape a string.
   * @param string
   * @return The HTML-escaped string.
   */
  public static String html(String string) {
    return StringEscapeUtils.escapeHtml(string);
  }

  /**
   * URL escape a string.
   * @param string
   * @return
   */
  public static String url(String string) {
    try {
      return URLEncoder.encode(string, StandardCharsets.UTF_8.name());
    }
    catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      System.exit(StatusCodes.GENERAL_ESCAPING_ERROR);
    }
    return null;
  }
}
