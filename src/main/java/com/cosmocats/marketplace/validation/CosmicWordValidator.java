package com.cosmocats.marketplace.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.Locale;

public class CosmicWordValidator implements ConstraintValidator<CosmicWordCheck, String> {

    private static final List<String> COSMIC_TERMS = List.of(
            "star", "galaxy", "comet", "cosmic", "nebula", "orbit", "planet",
            "moon", "meteor", "asteroid", "nova", "quasar", "gravity", "space");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return COSMIC_TERMS.stream().anyMatch(normalized::contains);
    }
}