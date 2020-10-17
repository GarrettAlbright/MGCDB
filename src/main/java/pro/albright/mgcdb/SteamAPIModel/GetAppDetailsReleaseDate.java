package pro.albright.mgcdb.SteamAPIModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class GetAppDetailsReleaseDate {
  private String date;

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  /**
   * Because I can't find a way to set the date as a real LocalDate object when
   * the bean is being created from JSON.
   *
   * Using GregorianCalendar like this seems to be the best way to get a
   * LocalDate from a Date without associating a time zone to it first.
   */
  public LocalDate getDateAsLocalDate() throws ParseException {
    SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy");
    Date normalDate = fmt.parse(date);
    GregorianCalendar cal = new GregorianCalendar();
    cal.setTime(normalDate);
    // GregorianCalendar considers January to be Month 0 because nothing about
    // this is allowed to be easy or even sane
    LocalDate localDate = LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    return localDate;
  }
}
