package ua.edu.sumdu.traveler.core.error;

/**
 * A business rule is broken. field names the form field the message belongs to, or is null when
 * the message concerns the whole form.
 */
public class ValidationException extends DomainException {

  private final String field;

  public ValidationException(String field, String message) {
    super(message);
    this.field = field;
  }

  public ValidationException(String message) {
    this(null, message);
  }

  public String getField() {
    return field;
  }
}
