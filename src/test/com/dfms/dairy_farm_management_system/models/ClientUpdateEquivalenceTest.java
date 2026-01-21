package com.dfms.dairy_farm_management_system.models;

import com.dfms.dairy_farm_management_system.connection.DBConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class ClientUpdateEquivalenceTest {

    private int existingClientId;

    @BeforeEach
    void setUp() throws SQLException {
        // Clean up and create a test client
        Connection conn = DBConfig.getConnection();
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM clients WHERE email = ?");
        stmt.setString(1, "test@example.com");
        stmt.executeUpdate();

        // Insert a test client to update
        stmt = conn.prepareStatement(
                "INSERT INTO clients (name, type, phone, email, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
                PreparedStatement.RETURN_GENERATED_KEYS
        );
        stmt.setString(1, "Original Name");
        stmt.setString(2, "person");
        stmt.setString(3, "+355691234567");
        stmt.setString(4, "test@example.com");
        stmt.executeUpdate();

        var rs = stmt.getGeneratedKeys();
        if (rs.next()) {
            existingClientId = rs.getInt(1);
        }

        DBConfig.disconnect();
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Clean up test data
        Connection conn = DBConfig.getConnection();
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM clients WHERE id = ?");
        stmt.setInt(1, existingClientId);
        stmt.executeUpdate();
        DBConfig.disconnect();
    }

    @Test
    void update_existingIdWithValidFields_shouldReturnTrue() {
        // EC1: ID exists + valid fields
        Client client = new Client();
        client.setId(existingClientId);
        client.setName("Updated Name");
        client.setType("company");
        client.setPhone("+355697777777");
        client.setEmail("updated@example.com");

        boolean result = client.update();

        assertTrue(result, "Updating existing client with valid fields should return true");
    }

    @Test
    void update_nonExistingId_shouldReturnFalse() {
        // EC2: ID doesn't exist
        Client client = new Client();
        client.setId(Integer.MAX_VALUE);
        client.setName("Test Client");
        client.setType("person");
        client.setPhone("+355691234567");
        client.setEmail("nonexistent@example.com");

        boolean result = client.update();

        assertFalse(result, "Updating non-existing ID should return false");
    }

    @Test
    void update_zeroId_shouldReturnFalse() {
        // EC3: ID is zero
        Client client = new Client();
        client.setId(0);
        client.setName("Test Client");
        client.setType("person");
        client.setPhone("+355691234567");
        client.setEmail("zero@example.com");

        boolean result = client.update();

        assertFalse(result, "Updating with ID=0 should return false");
    }

    @Test
    void update_negativeId_shouldReturnFalse() {
        // EC3: ID is negative
        Client client = new Client();
        client.setId(-1);
        client.setName("Test Client");
        client.setType("person");
        client.setPhone("+355691234567");
        client.setEmail("negative@example.com");

        boolean result = client.update();

        assertFalse(result, "Updating with negative ID should return false");
    }

    @Test
    void update_existingIdWithNullName_shouldReturnFalse() {
        // EC4: Null name violates NOT NULL constraint
        Client client = new Client();
        client.setId(existingClientId);
        client.setName(null);
        client.setType("person");
        client.setPhone("+355691234567");
        client.setEmail("test@example.com");

        boolean result = client.update();

        assertFalse(result, "Updating with null name should return false due to NOT NULL constraint");
    }
}