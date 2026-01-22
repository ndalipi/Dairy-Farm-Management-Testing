package com.dfms.dairy_farm_management_system.models;

import com.dfms.dairy_farm_management_system.connection.DBConfig;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MilkSaleCrudIntegrationTest {

    private MilkSale milkSale;
    private int clientId;
    private String clientEmail;
    private String clientPhone;
    private int insertedMilkSaleId;

    @BeforeEach
    void setUp() throws Exception {
        insertedMilkSaleId = -1;

        String tag = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        clientEmail = "milk_client_" + tag + "@test.com";
        clientPhone = "+35569" + (10000000 + (int) (Math.random() * 90000000));

        clientId = insertClientAndReturnId(
                "MilkClient_" + tag,
                "person",
                clientPhone,
                clientEmail
        );

        milkSale = new MilkSale();
        milkSale.setClientId(clientId);
        milkSale.setQuantity(12.5f);
        milkSale.setPrice(50.0f);
        milkSale.setSale_date(Date.valueOf("2025-01-10"));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (insertedMilkSaleId != -1) {
            deleteMilkSaleById(insertedMilkSaleId);
        }
        if (clientId != -1) {
            deleteClientById(clientId);
        }
    }

    @Test
    void save_validMilkSale_shouldInsertRow() throws Exception {
        boolean result = milkSale.save();
        assertTrue(result, "MilkSale.save() should return true when insert succeeds");

        insertedMilkSaleId = findLatestMilkSaleIdForClient(clientId);
        assertTrue(insertedMilkSaleId > 0, "Inserted milk sale should exist in database");

        assertEquals(12.5f, getMilkSaleQuantityById(insertedMilkSaleId), 0.0001f);
        assertEquals(50.0f, getMilkSalePriceById(insertedMilkSaleId), 0.0001f);
        assertEquals(Date.valueOf("2025-01-10"), getMilkSaleDateById(insertedMilkSaleId));
        assertEquals(clientId, getMilkSaleClientIdById(insertedMilkSaleId));
    }

    @Test
    void save_invalidClientId_shouldReturnFalse() {
        milkSale.setClientId(999999);

        boolean result = milkSale.save();

        assertFalse(result, "save() should return false for invalid client_id (FK violation)");
    }

    @Test
    void update_existingMilkSale_shouldModifyRow() throws Exception {
        assertTrue(milkSale.save(), "Precondition: save() must succeed");
        insertedMilkSaleId = findLatestMilkSaleIdForClient(clientId);
        assertTrue(insertedMilkSaleId > 0, "Inserted milk sale id must be found");

        milkSale.setId(insertedMilkSaleId);
        milkSale.setQuantity(20.0f);
        milkSale.setPrice(80.0f);
        milkSale.setSale_date(Date.valueOf("2025-02-01"));

        boolean updated = milkSale.update();
        assertTrue(updated, "update() should return true for existing row");

        assertEquals(20.0f, getMilkSaleQuantityById(insertedMilkSaleId), 0.0001f);
        assertEquals(80.0f, getMilkSalePriceById(insertedMilkSaleId), 0.0001f);
        assertEquals(Date.valueOf("2025-02-01"), getMilkSaleDateById(insertedMilkSaleId));
    }

    @Test
    void update_nonExistingMilkSale_shouldReturnFalse() {
        milkSale.setId(999999);

        boolean result = milkSale.update();

        assertFalse(result, "update() should return false for non-existing milk sale");
    }

    @Test
    void delete_existingMilkSale_shouldRemoveRow() throws Exception {
        assertTrue(milkSale.save(), "Precondition: save() must succeed");
        insertedMilkSaleId = findLatestMilkSaleIdForClient(clientId);
        assertTrue(insertedMilkSaleId > 0, "Inserted milk sale id must be found");

        milkSale.setId(insertedMilkSaleId);
        boolean deleted = milkSale.delete();
        assertTrue(deleted, "delete() should return true for existing row");

        assertFalse(existsMilkSaleById(insertedMilkSaleId), "Milk sale should not exist after delete");
        insertedMilkSaleId = -1;
    }

    @Test
    void delete_nonExistingMilkSale_shouldReturnFalse() {
        milkSale.setId(999999);

        boolean result = milkSale.delete();

        assertFalse(result, "delete() should return false for non-existing milk sale");
    }

    @Test
    void getClientName_shouldReturnNameFromClientsTable() {
        milkSale.setClientId(clientId);

        String name = milkSale.getClientName();

        assertNotNull(name);
        assertTrue(name.startsWith("MilkClient_"), "Client name should match test client");
    }

    private static int insertClientAndReturnId(String name, String type, String phone, String email) throws Exception {
        String sql = "INSERT INTO clients (name, type, phone, email, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setString(3, phone);
            ps.setString(4, email);
            ps.setTimestamp(5, now);
            ps.setTimestamp(6, now);

            int rows = ps.executeUpdate();
            if (rows == 0) throw new SQLException("Insert client failed");

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
            throw new SQLException("Insert client failed, no generated key");
        } finally {
            DBConfig.disconnect();
        }
    }

    private static void deleteClientById(int id) throws Exception {
        String sql = "DELETE FROM clients WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } finally {
            DBConfig.disconnect();
        }
    }

    private static void deleteMilkSaleById(int id) throws Exception {
        String sql = "DELETE FROM milk_sales WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } finally {
            DBConfig.disconnect();
        }
    }

    private static int findLatestMilkSaleIdForClient(int clientId) throws Exception {
        String sql = "SELECT id FROM milk_sales WHERE client_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
                return -1;
            }
        } finally {
            DBConfig.disconnect();
        }
    }

    private static boolean existsMilkSaleById(int id) throws Exception {
        String sql = "SELECT COUNT(*) FROM milk_sales WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } finally {
            DBConfig.disconnect();
        }
    }

    private static float getMilkSaleQuantityById(int id) throws Exception {
        return getMilkSaleFloatFieldById(id, "quantity");
    }

    private static float getMilkSalePriceById(int id) throws Exception {
        return getMilkSaleFloatFieldById(id, "price");
    }

    private static Date getMilkSaleDateById(int id) throws Exception {
        String sql = "SELECT sale_date FROM milk_sales WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getDate("sale_date");
            }
        } finally {
            DBConfig.disconnect();
        }
    }

    private static int getMilkSaleClientIdById(int id) throws Exception {
        String sql = "SELECT client_id FROM milk_sales WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return -1;
                return rs.getInt("client_id");
            }
        } finally {
            DBConfig.disconnect();
        }
    }

    private static float getMilkSaleFloatFieldById(int id, String field) throws Exception {
        String sql = "SELECT " + field + " FROM milk_sales WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Float.NaN;
                return rs.getFloat(field);
            }
        } finally {
            DBConfig.disconnect();
        }
    }
}