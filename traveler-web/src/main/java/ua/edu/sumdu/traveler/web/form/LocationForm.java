package ua.edu.sumdu.traveler.web.form;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.format.annotation.DateTimeFormat;
import ua.edu.sumdu.traveler.core.command.LocationCommand;
import ua.edu.sumdu.traveler.core.domain.Location;
import ua.edu.sumdu.traveler.web.validation.DateRange;

/**
 * Form of a route location. Times are entered in the local time zone of the system
 * (traveler.zone) through datetime-local inputs and stored as TIMESTAMPTZ.
 */
@DateRange(start = "arrivalDate", end = "departureDate",
    message = "Час відбуття не може передувати часу прибуття")
public class LocationForm {

  static final String INPUT_PATTERN = "yyyy-MM-dd'T'HH:mm";

  @NotBlank(message = "Вкажіть назву локації")
  @Size(max = 200, message = "Назва — не більше 200 символів")
  private String name;

  @Size(max = 1000, message = "Адреса — не більше 1000 символів")
  private String address;

  @DecimalMin(value = "-90", message = "Широта — від -90 до 90")
  @DecimalMax(value = "90", message = "Широта — від -90 до 90")
  @Digits(integer = 2, fraction = 6, message = "Широта — до 6 знаків після коми")
  private BigDecimal latitude;

  @DecimalMin(value = "-180", message = "Довгота — від -180 до 180")
  @DecimalMax(value = "180", message = "Довгота — від -180 до 180")
  @Digits(integer = 3, fraction = 6, message = "Довгота — до 6 знаків після коми")
  private BigDecimal longitude;

  @DateTimeFormat(pattern = INPUT_PATTERN)
  private LocalDateTime arrivalDate;

  @DateTimeFormat(pattern = INPUT_PATTERN)
  private LocalDateTime departureDate;

  @DecimalMin(value = "0.00", message = "Витрати не можуть бути від'ємними")
  @Digits(integer = 8, fraction = 2, message = "Витрати — до 8 цифр і 2 знаки після коми")
  private BigDecimal budget;

  @Size(max = 2000, message = "Примітки — не більше 2000 символів")
  private String notes;

  /** Position in the route; only the edit form shows it. */
  @Min(value = 1, message = "Позиція — ціле число від 1")
  private Integer visitOrder;

  private Integer version;

  public static LocationForm of(Location location, ZoneId zone) {
    LocationForm form = new LocationForm();
    form.name = location.name();
    form.address = location.address();
    form.latitude = location.latitude() == null ? null : location.latitude().stripTrailingZeros();
    form.longitude = location.longitude() == null ? null : location.longitude().stripTrailingZeros();
    form.arrivalDate = local(location.arrivalDate(), zone);
    form.departureDate = local(location.departureDate(), zone);
    form.budget = location.budget();
    form.notes = location.notes();
    form.visitOrder = location.visitOrder();
    form.version = location.version();
    return form;
  }

  public LocationCommand toCommand(ZoneId zone) {
    return new LocationCommand(name, address, latitude, longitude, offset(arrivalDate, zone),
        offset(departureDate, zone), budget, notes, visitOrder, version);
  }

  private static LocalDateTime local(OffsetDateTime time, ZoneId zone) {
    return time == null ? null : time.atZoneSameInstant(zone).toLocalDateTime();
  }

  private static OffsetDateTime offset(LocalDateTime time, ZoneId zone) {
    return time == null ? null : time.atZone(zone).toOffsetDateTime();
  }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }
  public BigDecimal getLatitude() { return latitude; }
  public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
  public BigDecimal getLongitude() { return longitude; }
  public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
  public LocalDateTime getArrivalDate() { return arrivalDate; }
  public void setArrivalDate(LocalDateTime arrivalDate) { this.arrivalDate = arrivalDate; }
  public LocalDateTime getDepartureDate() { return departureDate; }
  public void setDepartureDate(LocalDateTime departureDate) { this.departureDate = departureDate; }
  public BigDecimal getBudget() { return budget; }
  public void setBudget(BigDecimal budget) { this.budget = budget; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public Integer getVisitOrder() { return visitOrder; }
  public void setVisitOrder(Integer visitOrder) { this.visitOrder = visitOrder; }
  public Integer getVersion() { return version; }
  public void setVersion(Integer version) { this.version = version; }
}
