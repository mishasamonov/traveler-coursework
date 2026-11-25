package ua.edu.sumdu.traveler.core.domain;

/** Categories of registered users. A guest has no role at all. */
public enum Role {
  /** Traveller: manages only own plans and routes. */
  USER("Мандрівник"),
  /** Administrator: full control over all plans and user accounts. */
  ADMIN("Адміністратор");

  private final String label;

  Role(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
