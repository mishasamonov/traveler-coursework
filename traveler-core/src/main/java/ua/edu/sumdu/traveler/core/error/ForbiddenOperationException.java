package ua.edu.sumdu.traveler.core.error;

/** The actor sees the record but has no right to perform the operation on it. */
public class ForbiddenOperationException extends DomainException {

  public ForbiddenOperationException(String message) {
    super(message);
  }
}
