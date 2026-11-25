package ua.edu.sumdu.traveler.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level Bean Validation constraint: the value of {@code end} is not earlier than the value
 * of {@code start} (both optional). The error is attached to the {@code end} field of the form.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
public @interface DateRange {

  String start();

  String end();

  String message() default "Кінець періоду не може передувати його початку";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
