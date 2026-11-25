package ua.edu.sumdu.traveler.core.error;

/** The record does not exist or the actor may not know that it exists (a private plan). */
public class NotFoundException extends DomainException {

  public NotFoundException(String message) {
    super(message);
  }
}
