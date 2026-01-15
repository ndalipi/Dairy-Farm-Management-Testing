package com.dfms.dairy_farm_management_system.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnimalBoundaryTest {

    Animal animal = new Animal();

    @Test
    void ageBelowMinimum_shouldBeInvalid() {
        assertFalse(animal.isValidAge(-1));
    }

    @Test
    void ageAtMinimum_shouldBeValid() {
        assertTrue(animal.isValidAge(0));
    }

    @Test
    void ageJustAboveMinimum_shouldBeValid() {
        assertTrue(animal.isValidAge(1));
    }

    @Test
    void ageJustBelowMaximum_shouldBeValid() {
        assertTrue(animal.isValidAge(29));
    }

    @Test
    void ageAtMaximum_shouldBeValid() {
        assertTrue(animal.isValidAge(30));
    }

    @Test
    void ageAboveMaximum_shouldBeInvalid() {
        assertFalse(animal.isValidAge(31));
    }
}
