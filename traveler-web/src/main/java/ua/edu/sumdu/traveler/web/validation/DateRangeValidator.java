package ua.edu.sumdu.traveler.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

/** Checks {@link DateRange} for LocalDate and LocalDateTime fields (any Comparable). */
public class DateRangeValidator implements ConstraintValidator<DateRange, Object> {

  private String start;
  private String end;

  @Override
  public void initialize(DateRange annotation) {
    start = annotation.start();
    end = annotation.end();
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public boolean isValid(Object form, ConstraintValidatorContext context) {
    BeanWrapperImpl bean = new BeanWrapperImpl(form);
    Object from = bean.getPropertyValue(start);
    Object to = bean.getPropertyValue(end);
    if (from == null || to == null) return true; // @NotNull is a separate rule
    if (((Comparable) to).compareTo(from) >= 0) return true;
    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
        .addPropertyNode(end).addConstraintViolation();
    return false;
  }
}
