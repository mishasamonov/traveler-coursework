package ua.edu.sumdu.traveler.web.view;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Ukrainian formatting of amounts, dates and counts for the templates: ${@fmt.money(...)}.
 * Times are shown in the zone of the system (Europe/Kyiv by default).
 */
@Component("fmt")
public class ViewFormat {

  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

  private final ZoneId zone;

  public ViewFormat(ZoneId travelerZone) {
    this.zone = travelerZone;
  }

  /** 18000.00, UAH -> "18 000 UAH"; 24.50, EUR -> "24,50 EUR". */
  public String money(BigDecimal amount, String currency) {
    if (amount == null) return "—";
    return number(amount) + (currency == null ? "" : " " + currency);
  }

  public String number(BigDecimal amount) {
    if (amount == null) return "—";
    DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    symbols.setGroupingSeparator(' ');
    symbols.setDecimalSeparator(',');
    boolean whole = amount.stripTrailingZeros().scale() <= 0;
    DecimalFormat format = new DecimalFormat(whole ? "#,##0" : "#,##0.00", symbols);
    format.setRoundingMode(RoundingMode.HALF_UP);
    return format.format(amount);
  }

  public String date(LocalDate date) {
    return date == null ? "—" : DATE.format(date);
  }

  public String period(LocalDate start, LocalDate end) {
    if (start == null) return "Дати не визначено";
    if (end == null || end.equals(start)) return DATE.format(start);
    return DATE.format(start) + " — " + DATE.format(end);
  }

  public String dateTime(OffsetDateTime time) {
    return time == null ? "—" : DATE_TIME.format(time.atZoneSameInstant(zone));
  }

  public String instant(Instant time) {
    return time == null ? "—" : DATE_TIME.format(time.atZone(zone));
  }

  /** Visit time of a location: one day -> "01.05.2027, 11:00–13:00". */
  public String visit(OffsetDateTime arrival, OffsetDateTime departure) {
    if (arrival == null && departure == null) return "Час не визначено";
    if (arrival == null) return "до " + dateTime(departure);
    if (departure == null) return "з " + dateTime(arrival);
    ZonedDateTime a = arrival.atZoneSameInstant(zone);
    ZonedDateTime d = departure.atZoneSameInstant(zone);
    if (a.toLocalDate().equals(d.toLocalDate())) {
      return DATE.format(a) + ", " + TIME.format(a) + "–" + TIME.format(d);
    }
    return DATE_TIME.format(a) + " — " + DATE_TIME.format(d);
  }

  public String km(BigDecimal km) {
    if (km == null) return "—";
    DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    symbols.setGroupingSeparator(' ');
    symbols.setDecimalSeparator(',');
    return new DecimalFormat("#,##0.0", symbols).format(km) + " км";
  }

  public String days(Long days) {
    return days == null ? "—" : days + " " + plural(days, "день", "дні", "днів");
  }

  public String locations(long count) {
    return count + " " + plural(count, "локація", "локації", "локацій");
  }

  /** A number with a noun in the right form: count(3, "маршрут", "маршрути", "маршрутів"). */
  public String count(long n, String one, String few, String many) {
    return n + " " + plural(n, one, few, many);
  }

  /** Ukrainian plural: 1 день, 2 дні, 5 днів, 11 днів, 21 день. */
  public static String plural(long n, String one, String few, String many) {
    long mod100 = Math.abs(n) % 100;
    long mod10 = Math.abs(n) % 10;
    if (mod100 >= 11 && mod100 <= 14) return many;
    if (mod10 == 1) return one;
    if (mod10 >= 2 && mod10 <= 4) return few;
    return many;
  }

  public String percent(Integer value) {
    return value == null ? "—" : value + "%";
  }

  /** Width of a progress bar, at most 100%. */
  public int bar(Integer value) {
    return value == null ? 0 : Math.min(100, Math.max(0, value));
  }
}
