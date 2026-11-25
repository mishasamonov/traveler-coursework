package ua.edu.sumdu.traveler.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Class-level constraint: two fields are equal (password and its confirmation). */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FieldsMatchValidator.class)
public @interface FieldsMatch {

  String field();

  String confirmation();

  String message() default "Значення не збігаються";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
