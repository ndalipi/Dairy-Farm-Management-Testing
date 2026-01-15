package com.dfms.dairy_farm_management_system.helpers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
public class HelperBoundaryValueTesting {

    @Test
    void shouldReturnZero_whenValueIsBelowZero() {
        assertEquals(0, Helper.normalizePercentage(-1));
    }

    @Test
    void shouldReturnZero_whenValueIsZero() {
        assertEquals(0, Helper.normalizePercentage(0));
    }

    @Test
    void shouldReturnOne_whenValueIsOne() {
        assertEquals(1, Helper.normalizePercentage(1));
    }

    @Test
    void shouldReturnNinetyNine_whenValueIsNinetyNine() {
        assertEquals(99, Helper.normalizePercentage(99));
    }

    @Test
    void shouldReturnOneHundred_whenValueIsOneHundred() {
        assertEquals(100, Helper.normalizePercentage(100));
    }

    @Test
    void shouldReturnOneHundred_whenValueIsAboveOneHundred() {
        assertEquals(100, Helper.normalizePercentage(101));
    }
}
