package com.cosmocats.marketplace.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CosmicWordValidatorTest {

    private final CosmicWordValidator validator = new CosmicWordValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "star", "galaxy", "comet", "cosmic", "nebula", "orbit", "planet",
            "moon", "meteor", "asteroid", "nova", "quasar", "gravity", "space"
    })
    void acceptsEveryCosmicTerm(String term) {
        assertThat(validator.isValid(term, null)).isTrue();
        assertThat(validator.isValid(term.toUpperCase(), null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Star Mug",
            "GALAXY brush",
            "Anti-Gravity Yarn Ball",
            "Cosmic Milk",
            "Comet Tail Scratcher",
            "Moons of Jupiter",
            "deep-space radio"
    })
    void acceptsNamesWithWholeCosmicWord(String name) {
        assertThat(validator.isValid(name, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Plain Mug",
            "Starbucks Mug",
            "Moonshine",
            "Stardust",
            "Catnip"
    })
    void rejectsNamesWithoutWholeCosmicWord(String name) {
        assertThat(validator.isValid(name, null)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void treatsNullAndBlankAsValid(String name) {
        assertThat(validator.isValid(name, null)).isTrue();
    }

    @Test
    void defaultMessageHasNoTrailingPeriod() throws NoSuchMethodException {
        String message = (String) CosmicWordCheck.class.getMethod("message").getDefaultValue();

        assertThat(message).isEqualTo("must contain a cosmic term (e.g. star, galaxy, comet)");
    }
}