package com.dfms.dairy_farm_management_system.controllers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DashboardControllerCoverageTest {

    private final DashboardController controller = new DashboardController();

    @Test
    void statementCoverage() {
        assertFalse(controller.isValidEarningsInput(null, false));
        assertTrue(controller.isValidEarningsInput(10.0, false));
    }

    @Test
    void branchCoverage() {
        assertFalse(controller.isValidEarningsInput(-1.0, true));
        assertTrue(controller.isValidEarningsInput(null, true));
    }

    @Test
    void conditionCoverage() {
        assertFalse(controller.isValidEarningsInput(null, false));

        assertTrue(controller.isValidEarningsInput(null, true));

        assertFalse(controller.isValidEarningsInput(-5.0, true));

        assertTrue(controller.isValidEarningsInput(5.0, false));
    }

    @Test
    void mcdc_allowNullIndependentlyAffectsDecision() {
        assertFalse(controller.isValidEarningsInput(null, false));
        assertTrue(controller.isValidEarningsInput(null, true));
    }

    @Test
    void mcdc_nullnessIndependentlyAffectsDecision() {
        assertFalse(controller.isValidEarningsInput(null, false));
        assertTrue(controller.isValidEarningsInput(10.0, false));
    }

    @Test
    void mcdc_negativityIndependentlyAffectsDecision() {
        assertFalse(controller.isValidEarningsInput(-1.0, true));
        assertTrue(controller.isValidEarningsInput(1.0, true));
    }
}
