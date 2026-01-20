package com.dfms.dairy_farm_management_system.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SalesControllerUnitTest {

    private SalesController controller;

    @BeforeEach
    void setUp() {
        controller = new SalesController();
    }

    // -----------------------------
    // Tests for safeLower(String)
    // -----------------------------

    @Test
    void safeLower_nullInput_returnsEmptyString() {
        String result = controller.safeLower(null);
        assertEquals("", result);
    }

    @Test
    void safeLower_emptyString_returnsEmptyString() {
        String result = controller.safeLower("");
        assertEquals("", result);
    }

    @Test
    void safeLower_uppercaseString_convertsToLowercase() {
        String result = controller.safeLower("HELLO");
        assertEquals("hello", result);
    }

    @Test
    void safeLower_mixedCase_convertsToLowercase() {
        String result = controller.safeLower("HeLLo");
        assertEquals("hello", result);
    }

    // -----------------------------
    // Tests for safeContains(String, String)
    // -----------------------------

    @Test
    void safeContains_caseInsensitiveMatch_returnsTrue() {
        boolean result = controller.safeContains("Hello World", "hello");
        assertTrue(result);
    }

    @Test
    void safeContains_noMatch_returnsFalse() {
        boolean result = controller.safeContains("Hello World", "xyz");
        assertFalse(result);
    }

    @Test
    void safeContains_nullText_returnsFalse() {
        boolean result = controller.safeContains(null, "test");
        assertFalse(result);
    }

    @Test
    void safeContains_emptyQuery_returnsTrue() {
        boolean result = controller.safeContains("Hello", "");
        assertTrue(result);
    }

    @Test
    void safeContains_nullQuery_returnsTrue() {
        boolean result = controller.safeContains("Hello", null);
        assertTrue(result);
    }
}
