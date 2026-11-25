package ua.edu.sumdu.traveler.web.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ua.edu.sumdu.traveler.core.command.RegistrationCommand;
import ua.edu.sumdu.traveler.web.validation.FieldsMatch;

@FieldsMatch(field = "password", confirmation = "confirmPassword", message = "Паролі не збігаються")
public class RegistrationForm {

  @NotBlank(message = "Вкажіть електронну пошту")
  @Email(message = "Некоректна адреса електронної пошти")
  @Size(max = 254, message = "Адреса — не більше 254 символів")
  private String email;

  @NotBlank(message = "Вкажіть ім'я")
  @Size(min = 2, max = 100, message = "Ім'я — від 2 до 100 символів")
  private String displayName;

  @Size(min = 8, max = 72, message = "Пароль — від 8 до 72 символів")
  @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$", message = "Пароль має містити літери й цифри")
  private String password;

  private String confirmPassword;

  public RegistrationCommand toCommand() {
    return new RegistrationCommand(email, displayName, password);
  }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getDisplayName() { return displayName; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }
  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
  public String getConfirmPassword() { return confirmPassword; }
  public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
