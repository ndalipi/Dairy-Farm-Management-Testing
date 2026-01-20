package com.dfms.dairy_farm_management_system.controllers;

import com.dfms.dairy_farm_management_system.connection.DBConfig;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class DashboardControllerIntegrationTest {

    private static boolean dbAvailable;

    @BeforeAll
    static void checkDatabaseIsReachable() {
        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {

            dbAvailable = rs.next();
        } catch (Exception e) {
            dbAvailable = false;
        }
    }

    @BeforeEach
    void skipIfDbNotAvailable() {
        assumeTrue(dbAvailable, "Skipping integration tests: database is not reachable on this machine.");
    }

    @Test
    void getSalesOfSpecificDay_invalidDay_returnsZero() {
        DashboardController controller = new DashboardController();

        int result1 = controller.getSalesOfSpecificDay("Monday", "animals_sales");
        int result2 = controller.getSalesOfSpecificDay("X", "animals_sales");
        int result3 = controller.getSalesOfSpecificDay("", "animals_sales");

        assertEquals(0, result1);
        assertEquals(0, result2);
        assertEquals(0, result3);
    }

    @Test
    void getSalesOfSpecificDay_validDay_returnsNonNegative() {
        DashboardController controller = new DashboardController();

        int result = controller.getSalesOfSpecificDay("Fri", "animals_sales");
        assertTrue(result >= 0, "Sales count should never be negative.");
    }

    @Test
    void getEarningsOfSpecificDay_invalidDay_returnsZero() {
        DashboardController controller = new DashboardController();

        int result1 = controller.getEarningsOfSpecificDay("Friday");
        int result2 = controller.getEarningsOfSpecificDay("?");
        int result3 = controller.getEarningsOfSpecificDay("");
        assertEquals(0, result1);
        assertEquals(0, result2);
        assertEquals(0, result3);
    }

    @Test
    void getEarningsOfSpecificDay_validDay_returnsNonNegative() {
        DashboardController controller = new DashboardController();

        int result = controller.getEarningsOfSpecificDay("Fri");
        assertTrue(result >= 0, "Earnings should never be negative.");
    }
}
