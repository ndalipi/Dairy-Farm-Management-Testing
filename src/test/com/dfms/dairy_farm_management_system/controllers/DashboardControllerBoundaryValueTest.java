package com.dfms.dairy_farm_management_system.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DashboardControllerBoundaryValueTest {

    private DashboardController controller;

    @BeforeEach
    void setUp() {
        controller = new DashboardController();
    }

    // BVT: below minimum boundary
    @Test
    void shouldReturnZero_whenEarningsBelowZero() {
        assertEquals(0.0, controller.clampDailyEarnings(-0.01));
    }

    // BVT: minimum boundary
    @Test
    void shouldReturnZero_whenEarningsIsZero() {
        assertEquals(0.0, controller.clampDailyEarnings(0.0));
    }

    // BVT: just above minimum boundary
    @Test
    void shouldReturnSameValue_whenEarningsJustAboveZero() {
        assertEquals(0.01, controller.clampDailyEarnings(0.01));
    }

    // BVT: just below maximum boundary
    @Test
    void shouldReturnSameValue_whenEarningsJustBelowMax() {
        assertEquals(999_999.99, controller.clampDailyEarnings(999_999.99));
    }

    // BVT: maximum boundary
    @Test
    void shouldReturnMax_whenEarningsIsMax() {
        assertEquals(1_000_000.0, controller.clampDailyEarnings(1_000_000.0));
    }

    // BVT: above maximum boundary
    @Test
    void shouldReturnMax_whenEarningsAboveMax() {
        assertEquals(1_000_000.0, controller.clampDailyEarnings(1_000_000.01));
    }
}
