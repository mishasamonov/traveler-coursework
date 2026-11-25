package ua.edu.sumdu.traveler.core.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import ua.edu.sumdu.traveler.core.error.ValidationException;

/**
 * Business rules for the values that come from users. They repeat the CHECK constraints of the
 * database, so a wrong value gets a clear message instead of an SQL error.
 */
final class InputRules {

  /** NUMERIC(10,2): at most 8 digits before the point. */
  static final BigDecimal MAX_MONEY = new BigDecimal("99999999.99");

  private InputRules() {}

  /** Trims the value; a blank value becomes null. */
  static String clean(String value) {
    if (value == null) return null;
    String trimmed = value.strip();
    return trimmed.isEmpty() ? null : trimmed;
  }

  static String required(String value, String field, String label, int maxLength) {
    String text = clean(value);
    if (text == null) throw new ValidationException(field, label + " — обов'язкове поле");
    return limit(text, field, label, maxLength);
  }

  static String optional(String value, String field, String label, int maxLength) {
    String text = clean(value);
    return text == null ? null : limit(text, field, label, maxLength);
  }

  private static String limit(String text, String field, String label, int maxLength) {
    if (text.length() > maxLength)
      throw new ValidationException(field, label + " — не більше " + maxLength + " символів");
    return text;
  }

  /** Money amount: not negative, at most two decimal places, fits NUMERIC(10,2). */
  static BigDecimal money(BigDecimal value, String field, String label) {
    if (value == null) return null;
    if (value.signum() < 0) throw new ValidationException(field, label + " не може бути від'ємним");
    if (value.stripTrailingZeros().scale() > 2)
      throw new ValidationException(field, label + " — не більше двох знаків після коми");
    if (value.compareTo(MAX_MONEY) > 0)
      throw new ValidationException(field, label + " — не більше 99 999 999,99");
    return value.setScale(2, RoundingMode.UNNECESSARY);
  }

  static BigDecimal coordinate(BigDecimal value, String field, String label, int maxAbs) {
    if (value == null) return null;
    if (value.abs().compareTo(BigDecimal.valueOf(maxAbs)) > 0)
      throw new ValidationException(field, label + " має бути в межах від -" + maxAbs + " до " + maxAbs);
    if (value.stripTrailingZeros().scale() > 6)
      throw new ValidationException(field, label + " — не більше шести знаків після коми");
    return value.setScale(6, RoundingMode.UNNECESSARY);
  }

  static String truncate(String text, int maxLength) {
    return text.length() <= maxLength ? text : text.substring(0, maxLength);
  }
}
