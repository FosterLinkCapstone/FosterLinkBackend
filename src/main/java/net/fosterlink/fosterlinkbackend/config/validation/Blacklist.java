package net.fosterlink.fosterlinkbackend.config.validation;

import jakarta.validation.Constraint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = BlacklistValidator.class)
@Repeatable(Blacklists.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Blacklist {

    String message() default "This value is not allowed.";

    Class<?>[] groups() default {};

    Class<? extends jakarta.validation.Payload>[] payload() default {};

    /** How to match: "full", "startsWith", or "endsWith" */
    BlacklistMatchBy matchBy() default BlacklistMatchBy.FULL;

    String[] value() default {};

    boolean ignoreCase() default true;
}
