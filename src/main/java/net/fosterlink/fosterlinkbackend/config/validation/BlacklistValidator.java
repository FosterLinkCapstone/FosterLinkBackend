package net.fosterlink.fosterlinkbackend.config.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

public class BlacklistValidator implements ConstraintValidator<Blacklist, String> {

    private String[] blacklist;
    private BlacklistMatchBy matchBy;
    private boolean ignoreCase;

    @Override
    public void initialize(Blacklist constraintAnnotation) {
        this.blacklist = constraintAnnotation.value();
        this.matchBy = constraintAnnotation.matchBy();
        this.ignoreCase = constraintAnnotation.ignoreCase();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if (s == null) {
            return true; // null is valid; use @NotNull/@NotBlank for null checks
        }
        String input = ignoreCase ? s.toLowerCase() : s;
        return switch (matchBy) {
            case FULL -> Arrays.stream(blacklist)
                    .noneMatch(entry -> ignoreCase ? entry.equalsIgnoreCase(s) : entry.equals(s));
            case STARTS_WITH -> Arrays.stream(blacklist)
                    .noneMatch(entry -> input.startsWith(ignoreCase ? entry.toLowerCase() : entry));
            case ENDS_WITH -> Arrays.stream(blacklist)
                    .noneMatch(entry -> input.endsWith(ignoreCase ? entry.toLowerCase() : entry));
            default -> true;
        };
    }
}
