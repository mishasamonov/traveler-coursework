package ua.edu.sumdu.traveler.web.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import ua.edu.sumdu.traveler.core.command.PlanCommand;
import ua.edu.sumdu.traveler.core.domain.TravelPlan;
import ua.edu.sumdu.traveler.web.validation.DateRange;

/** Form of a travel plan. The constraints mirror the CHECK constraints of travel_plans. */
@DateRange(start = "startDate", end = "endDate",
    message = "Дата завершення не може передувати даті початку подорожі")
public class PlanForm {

  @NotBlank(message = "Вкажіть назву подорожі")
  @Size(max = 200, message = "Назва — не більше 200 символів")
  private String title;

  @Size(max = 5000, message = "Опис — не більше 5000 символів")
  private String description;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate endDate;

  @DecimalMin(value = "0.00", message = "Бюджет не може бути від'ємним")
  @Digits(integer = 8, fraction = 2, message = "Бюджет — до 8 цифр і 2 знаки після коми")
  private BigDecimal budget;

  @NotBlank(message = "Оберіть валюту")
  @Pattern(regexp = "[A-Z]{3}", message = "Валюта — три великі латинські літери")
  private String currency = "UAH";

  private boolean publicPlan;

  /** Version of the record when the form was opened (optimistic locking). */
  private Integer version;

  public static PlanForm of(TravelPlan plan) {
    PlanForm form = new PlanForm();
    form.title = plan.title();
    form.description = plan.description();
    form.startDate = plan.startDate();
    form.endDate = plan.endDate();
    form.budget = plan.budget();
    form.currency = plan.currency();
    form.publicPlan = plan.isPublic();
    form.version = plan.version();
    return form;
  }

  public PlanCommand toCommand() {
    return new PlanCommand(title, description, startDate, endDate, budget, currency, publicPlan,
        version);
  }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public LocalDate getStartDate() { return startDate; }
  public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
  public LocalDate getEndDate() { return endDate; }
  public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
  public BigDecimal getBudget() { return budget; }
  public void setBudget(BigDecimal budget) { this.budget = budget; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public boolean isPublicPlan() { return publicPlan; }
  public void setPublicPlan(boolean publicPlan) { this.publicPlan = publicPlan; }
  public Integer getVersion() { return version; }
  public void setVersion(Integer version) { this.version = version; }
}
