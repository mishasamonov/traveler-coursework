package ua.edu.sumdu.traveler.core.error;

/**
 * Optimistic locking: the record was changed after the user had opened it, so the user's version
 * is outdated and saving would silently overwrite somebody else's changes.
 */
public class ConflictException extends DomainException {

  private final Integer currentVersion;

  public ConflictException(String message, Integer currentVersion) {
    super(message);
    this.currentVersion = currentVersion;
  }

  public Integer getCurrentVersion() {
    return currentVersion;
  }
}
