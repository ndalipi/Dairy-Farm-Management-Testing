package com.dfms.dairy_farm_management_system.controllers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DashboardControllerAdditionalTests {
     //EQUIVALENCE CLASS TESTING
    @Test
    void getSalesOfSpecificDay_emptyDay_returnsZero() {
        DashboardController dc = new DashboardController();
        int result = dc.getSalesOfSpecificDay("", "animals_sales");
        assertEquals(0, result);
    }

    @Test
    void getSalesOfSpecificDay_unknownDay_returnsZero() {
        DashboardController dc = new DashboardController();
        int result = dc.getSalesOfSpecificDay("XYZ", "animals_sales");
        assertEquals(0, result);
    }

    @Test
    void getSalesOfSpecificDay_fullDayName_returnsZero() {
        DashboardController dc = new DashboardController();
        int result = dc.getSalesOfSpecificDay("Monday", "animals_sales");
        assertEquals(0, result);
    }

    @Test
    void getSalesOfSpecificDay_invalidDay_withNullTable_stillReturnsZero() {
        DashboardController dc = new DashboardController();
        int result = dc.getSalesOfSpecificDay("InvalidDay", null);
        assertEquals(0, result);
    }

    @Test
    void getEarningsOfSpecificDay_emptyDay_returnsZero() {
        DashboardController dc = new DashboardController();
        int result = dc.getEarningsOfSpecificDay("");
        assertEquals(0, result);
    }

    @Test
    void getEarningsOfSpecificDay_unknownDay_returnsZero() {
        DashboardController dc = new DashboardController();
        int result = dc.getEarningsOfSpecificDay("XYZ");
        assertEquals(0, result);
    }

    //CODE COVERAGE
    @Test
    void getSalesOfSpecificDay_nullDay_throwsNullPointerException() {
        DashboardController dc = new DashboardController();
        assertThrows(NullPointerException.class,
                () -> dc.getSalesOfSpecificDay(null, "animals_sales"));
    }

    @Test
    void getEarningsOfSpecificDay_nullDay_throwsNullPointerException() {
        DashboardController dc = new DashboardController();
        assertThrows(NullPointerException.class,
                () -> dc.getEarningsOfSpecificDay(null));
    }
}
