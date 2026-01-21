package com.dfms.dairy_farm_management_system.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

class ReportsControllerDailyMilkCollectionUnitTest {

    private ReportsController reportsController;
    private ReportsController.DailyMilkCollection d;

    @BeforeEach
    void setUp() {
        reportsController = new ReportsController();
        d = reportsController.new DailyMilkCollection();
    }

    // ========== INITIALIZATION TESTS ==========

    @Test
    void newInstance_hasZeroTotal() {
        assertEquals(0.0f, d.getTotal_day_collection(), 0.0001f);
        assertEquals(0.0f, d.getMorning_collection(), 0.0001f);
        assertEquals(0.0f, d.getEvening_collection(), 0.0001f);
    }

    // ========== BUSINESS LOGIC TESTS ==========

    @Test
    void setMorningCollection_updatesTotal() {
        d.setMorning_collection(10.0f);

        assertEquals(10.0f, d.getMorning_collection(), 0.0001f);
        assertEquals(10.0f, d.getTotal_day_collection(), 0.0001f);
    }

    @Test
    void setEveningCollection_updatesTotal() {
        d.setEvening_collection(5.0f);

        assertEquals(5.0f, d.getEvening_collection(), 0.0001f);
        assertEquals(5.0f, d.getTotal_day_collection(), 0.0001f);
    }

    @Test
    void setMorningAndEvening_calculatesTotalCorrectly() {
        d.setMorning_collection(10.0f);
        d.setEvening_collection(5.0f);

        assertEquals(15.0f, d.getTotal_day_collection(), 0.0001f);
    }

    @Test
    void settingMorningTwice_accumulatesTotal() {
        d.setMorning_collection(10.0f);
        d.setMorning_collection(7.0f);

        assertEquals(7.0f, d.getMorning_collection(), 0.0001f);
        assertEquals(17.0f, d.getTotal_day_collection(), 0.0001f);
    }

    // ========== EDGE CASES ==========

    @Test
    void setZeroValues_calculatesCorrectly() {
        d.setMorning_collection(0.0f);
        d.setEvening_collection(0.0f);

        assertEquals(0.0f, d.getTotal_day_collection(), 0.0001f);
    }

    @Test
    void setNegativeValues_handlesCorrectly() {
        d.setMorning_collection(-5.0f);

        assertEquals(-5.0f, d.getMorning_collection(), 0.0001f);
        assertEquals(-5.0f, d.getTotal_day_collection(), 0.0001f);
    }

    @Test
    void setLargeRealisticValues_handlesCorrectly() {
        d.setMorning_collection(1_000_000.0f);
        d.setEvening_collection(1_000_000.0f);

        assertEquals(2_000_000.0f, d.getTotal_day_collection(), 0.0001f);
    }

    // ========== OVERRIDE BEHAVIOR ==========

    @Test
    void setTotalDirectly_overridesCalculatedValue() {
        d.setMorning_collection(10.0f);
        d.setEvening_collection(5.0f);

        d.setTotal_day_collection(100.0f);

        assertEquals(100.0f, d.getTotal_day_collection(), 0.0001f);
    }

    // ========== DATE HANDLING ==========

    @Test
    void setCollectionDate_storesCorrectly() {
        Date date = Date.valueOf("2026-01-21");
        d.setCollection_date(date);

        assertEquals(date, d.getCollection_date());
    }

    @Test
    void setNullDate_handlesGracefully() {
        assertDoesNotThrow(() -> d.setCollection_date(null));
        assertNull(d.getCollection_date());
    }
}