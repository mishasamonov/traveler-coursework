package ua.edu.sumdu.traveler.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;
import org.springframework.beans.BeanWrapperImpl;

public class FieldsMatchValidator implements ConstraintValidator<FieldsMatch, Object> {

  private String field;
  private String confirmation;

  @Override
  public void initialize(FieldsMatch annotation) {
    field = annotation.field();
    confirmation = annotation.confirmation();
  }

  @Override
  public boolean isValid(Object form, ConstraintValidatorContext context) {
    BeanWrapperImpl bean = new BeanWrapperImpl(form);
    if (Objects.equals(bean.getPropertyValue(field), bean.getPropertyValue(confirmation))) {
      return true;
    }
    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
        .addPropertyNode(confirmation).addConstraintViolation();
    return false;
  }
}
