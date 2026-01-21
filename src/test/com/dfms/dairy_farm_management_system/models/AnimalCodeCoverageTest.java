package com.dfms.dairy_farm_management_system.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnimalCodeCoverageTest {

    private final Animal animal = new Animal();

    @Test
    void allValid_returnsTrue() {
        // Baseline: all conditions true
        assertTrue(animal.isValidAnimalRegistration("A-001", 5, 100.0));
    }

    @Test
    void tagIdNull_returnsFalse() {
        assertFalse(animal.isValidAnimalRegistration(null, 5, 100.0));
    }

    @Test
    void tagIdEmpty_returnsFalse() {
        assertFalse(animal.isValidAnimalRegistration("", 5, 100.0));
    }

    @Test
    void tagIdWhitespace_returnsFalse() {
        assertFalse(animal.isValidAnimalRegistration("   ", 5, 100.0));
    }

    @Test
    void ageNegative_returnsFalse() {
        // MC/DC: Flips only (age >= 0) from baseline
        assertFalse(animal.isValidAnimalRegistration("A-001", -1, 100.0));
    }

    @Test
    void ageAboveMax_returnsFalse() {
        // MC/DC: Flips only (age <= 30) from baseline
        assertFalse(animal.isValidAnimalRegistration("A-001", 31, 100.0));
    }

    @Test
    void weightZero_returnsFalse() {
        // MC/DC: Flips only (weightKg > 0) from baseline
        assertFalse(animal.isValidAnimalRegistration("A-001", 5, 0.0));
    }

    @Test
    void weightNegative_returnsFalse() {
        assertFalse(animal.isValidAnimalRegistration("A-001", 5, -10.0));
    }
}