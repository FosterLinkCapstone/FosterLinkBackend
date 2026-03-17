package net.fosterlink.fosterlinkbackend.models.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Size(min = 12, max = 128)
@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$",
    message = "Password must be 12–128 characters and include uppercase, lowercase, a digit, and a special character (@$!%*?&)."
)
@Constraint(validatedBy = {})
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "Password must be 12–128 characters and include uppercase, lowercase, a digit, and a special character (@$!%*?&).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
