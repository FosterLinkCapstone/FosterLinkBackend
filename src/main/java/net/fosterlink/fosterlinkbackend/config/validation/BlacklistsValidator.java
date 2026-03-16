package net.fosterlink.fosterlinkbackend.config.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BlacklistsValidator implements ConstraintValidator<Blacklists, String> {

    private Blacklist[] blacklists;

    @Override
    public void initialize(Blacklists constraintAnnotation) {
        this.blacklists = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        for (Blacklist blacklist : blacklists) {
            BlacklistValidator delegate = new BlacklistValidator();
            delegate.initialize(blacklist);
            if (!delegate.isValid(value, context)) {
                return false;
            }
        }
        return true;
    }
}
