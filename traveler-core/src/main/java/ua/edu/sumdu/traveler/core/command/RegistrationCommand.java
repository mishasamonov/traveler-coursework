package ua.edu.sumdu.traveler.core.command;

/** Self-registration of a traveller. The password arrives in plain text and is hashed at once. */
public record RegistrationCommand(String email, String displayName, String password) {

  @Override
  public String toString() {
    return "RegistrationCommand[email=" + email + ", displayName=" + displayName + "]";
  }
}
