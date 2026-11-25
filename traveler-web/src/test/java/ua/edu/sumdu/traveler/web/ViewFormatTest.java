package ua.edu.sumdu.traveler.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.sumdu.traveler.web.view.ViewFormat;

class ViewFormatTest {

  private final ViewFormat fmt = new ViewFormat(ZoneId.of("Europe/Kyiv"));

  @Test
  @DisplayName("Українські форми множини: 1 день, 3 дні, 5 днів, 11 днів, 21 день")
  void plural() {
    assertThat(fmt.days(1L)).isEqualTo("1 день");
    assertThat(fmt.days(3L)).isEqualTo("3 дні");
    assertThat(fmt.days(5L)).isEqualTo("5 днів");
    assertThat(fmt.days(11L)).isEqualTo("11 днів");
    assertThat(fmt.days(21L)).isEqualTo("21 день");
    assertThat(fmt.locations(4)).isEqualTo("4 локації");
  }

  @Test
  @DisplayName("Суми з пробілом-розділювачем і комою, час у київському поясі")
  void moneyAndTime() {
    assertThat(fmt.money(new BigDecimal("18000.00"), "UAH")).isEqualTo("18 000 UAH");
    assertThat(fmt.money(new BigDecimal("24.5"), "EUR")).isEqualTo("24,50 EUR");
    OffsetDateTime utc = OffsetDateTime.of(2027, 5, 1, 8, 0, 0, 0, ZoneOffset.UTC);
    assertThat(fmt.visit(utc, utc.plusHours(2))).isEqualTo("01.05.2027, 11:00–13:00");
  }
}
