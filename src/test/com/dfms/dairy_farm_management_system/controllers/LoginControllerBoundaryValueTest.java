package com.dfms.dairy_farm_management_system.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LoginControllerBoundaryValueTest {

    private LoginController controller;

    @BeforeEach
    void setUp() {
        controller = new LoginController();
    }

    @Test
    void shouldReturnFalse_whenAttemptsBelowZero() {
        assertFalse(controller.isValidLoginAttempts(-1));
    }

    @Test
    void shouldReturnTrue_whenAttemptsIsZero() {
        assertTrue(controller.isValidLoginAttempts(0));
    }

    @Test
    void shouldReturnTrue_whenAttemptsIsOne() {
        assertTrue(controller.isValidLoginAttempts(1));
    }

    @Test
    void shouldReturnTrue_whenAttemptsIsFour() {
        assertTrue(controller.isValidLoginAttempts(4));
    }

    @Test
    void shouldReturnTrue_whenAttemptsIsFive() {
        assertTrue(controller.isValidLoginAttempts(5));
    }

    @Test
    void shouldReturnFalse_whenAttemptsAboveFive() {
        assertFalse(controller.isValidLoginAttempts(6));
    }
}
