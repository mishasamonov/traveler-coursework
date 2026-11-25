package ua.edu.sumdu.traveler.core.error;

/** Base class of the expected business errors; the web layer turns each type into a response. */
public abstract class DomainException extends RuntimeException {

  protected DomainException(String message) {
    super(message);
  }
}
