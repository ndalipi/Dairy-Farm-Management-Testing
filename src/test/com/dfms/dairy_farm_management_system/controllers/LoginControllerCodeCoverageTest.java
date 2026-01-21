package com.dfms.dairy_farm_management_system.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginControllerCodeCoverageTest {

    private LoginController controller;

    @BeforeEach
    void setUp() {
        controller = new LoginController();
    }

    // Statement + Branch Coverage

    @Test
    void attemptsBelowZero_returnsFalse() {
        assertFalse(controller.isLoginAllowed(-1, false, true));
    }

    @Test
    void allConditionsValid_returnsTrue() {
        assertTrue(controller.isLoginAllowed(3, false, true));
    }

    @Test
    void attemptsAboveLimit_returnsFalse() {
        assertFalse(controller.isLoginAllowed(6, false, true));
    }

    @Test
    void accountLocked_returnsFalse() {
        assertFalse(controller.isLoginAllowed(3, true, true));
    }

    @Test
    void captchaNotVerified_returnsFalse() {
        assertFalse(controller.isLoginAllowed(3, false, false));
    }

    // MC/DC Coverage Tests

    // Condition 1: attempts <= 5
    @Test
    void mcdc_attemptsCondition_changesDecision() {
        // attempts <= 5  → TRUE
        assertTrue(controller.isLoginAllowed(5, false, true));

        // attempts <= 5  → FALSE
        assertFalse(controller.isLoginAllowed(6, false, true));
    }

    // Condition 2: !isAccountLocked
    @Test
    void mcdc_accountLockedCondition_changesDecision() {
        // !isAccountLocked → TRUE
        assertTrue(controller.isLoginAllowed(3, false, true));

        // !isAccountLocked → FALSE
        assertFalse(controller.isLoginAllowed(3, true, true));
    }

    // Condition 3: isCaptchaVerified
    @Test
    void mcdc_captchaCondition_changesDecision() {
        // isCaptchaVerified → TRUE
        assertTrue(controller.isLoginAllowed(3, false, true));

        // isCaptchaVerified → FALSE
        assertFalse(controller.isLoginAllowed(3, false, false));
    }
}
