package net.fosterlink.fosterlinkbackend.config.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MaxNewlinesValidator implements ConstraintValidator<MaxNewlines, String> {

    private int max;

    @Override
    public void initialize(MaxNewlines constraintAnnotation) {
        this.max = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext context) {
        if (s == null) {
            return true; // null is valid; use @NotNull/@NotBlank for null checks
        }
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                if (++count > max) return false;
            }
        }
        return true;
    }
}
