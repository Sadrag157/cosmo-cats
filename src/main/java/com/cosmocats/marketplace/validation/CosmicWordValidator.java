package com.cosmocats.marketplace.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class CosmicWordValidator implements ConstraintValidator<CosmicWordCheck, String> {

    private static final List<String> COSMIC_TERMS = List.of(
            "star", "galaxy", "comet", "cosmic", "nebula", "orbit", "planet",
            "moon", "meteor", "asteroid", "nova", "quasar", "gravity", "space");

    private static final Pattern NON_LETTERS = Pattern.compile("[^A-Za-z]+");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return COSMIC_TERMS.stream().anyMatch(normalized::contains);
    }

    private static boolean isCosmicWord(String word) {
        if (COSMIC_TERMS.contains(word)) {
            return true;
        }
        return word.endsWith("s") && COSMIC_TERMS.contains(word.substring(0, word.length() - 1));
    }
}