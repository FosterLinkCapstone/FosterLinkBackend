package net.fosterlink.fosterlinkbackend.config.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.text.Normalizer;
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
        final String normalized = Normalizer.normalize(s, Normalizer.Form.NFKC);
        String input = ignoreCase ? normalized.toLowerCase() : normalized;
        return switch (matchBy) {
            case FULL -> Arrays.stream(blacklist)
                    .noneMatch(entry -> {
                        String normalizedEntry = Normalizer.normalize(entry, Normalizer.Form.NFKC);
                        return ignoreCase ? normalizedEntry.equalsIgnoreCase(normalized) : normalizedEntry.equals(normalized);
                    });
            case STARTS_WITH -> Arrays.stream(blacklist)
                    .noneMatch(entry -> {
                        String normalizedEntry = Normalizer.normalize(entry, Normalizer.Form.NFKC);
                        return input.startsWith(ignoreCase ? normalizedEntry.toLowerCase() : normalizedEntry);
                    });
            case ENDS_WITH -> Arrays.stream(blacklist)
                    .noneMatch(entry -> {
                        String normalizedEntry = Normalizer.normalize(entry, Normalizer.Form.NFKC);
                        return input.endsWith(ignoreCase ? normalizedEntry.toLowerCase() : normalizedEntry);
                    });
            default -> throw new IllegalStateException("Unsupported matchBy: " + matchBy);
        };
    }
}
