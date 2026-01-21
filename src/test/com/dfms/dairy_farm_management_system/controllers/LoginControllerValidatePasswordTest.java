package com.dfms.dairy_farm_management_system.controllers;

import com.dfms.dairy_farm_management_system.connection.DBConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class LoginControllerValidatePasswordTest {

    private LoginController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new LoginController();

        Connection connection = DBConfig.getConnection();
        Statement statement = connection.createStatement();

        statement.execute("DELETE FROM users");

        // Insert test user with MD5 hashed password
        statement.execute("""
        INSERT INTO users (id, role, password, gender, cin, phone, salary, email, first_name, last_name, address, created_at, updated_at)
        VALUES (1, 1, '5f4dcc3b5aa765d61d8327deb882cf99', 'M', 'CIN001', '0000000000', 1000, 'test@example.com', 'Test', 'User', 'Address', NOW(), NOW())
    """); // MD5 hash for "password"

        DBConfig.disconnect();
    }

    // ========== VALID EQUIVALENCE CLASS TEST ==========

    @Test
    void testValidEmailAndCorrectPassword() throws Exception {
        // EC1: Email exists in DB + EC6: Correct password
        boolean result = controller.validatePassword("test@example.com", "password");
        assertTrue(result, "Valid email with correct password should return true");
    }

    // ========== INVALID EQUIVALENCE CLASS TESTS - EMAIL ==========

    @Test
    void testNonExistingEmail() throws Exception {
        // EC2: Email doesn't exist in database
        boolean result = controller.validatePassword("nonexistent@example.com", "password");
        assertFalse(result, "Non-existing email should return false");
    }

    @Test
    void testInvalidEmailFormat() throws Exception {
        // EC3: Invalid email format (not in DB)
        boolean result = controller.validatePassword("notanemail", "password");
        assertFalse(result, "Invalid email format (not in DB) should return false");
    }

    @Test
    void testEmptyEmail() throws Exception {
        // EC4: Empty email string
        boolean result = controller.validatePassword("", "password");
        assertFalse(result, "Empty email should return false");
    }

    @Test
    void testNullEmail() throws Exception {
        // EC5: Null email
        boolean result = controller.validatePassword(null, "password");
        assertFalse(result, "Null email should return false");
    }

    // ========== INVALID EQUIVALENCE CLASS TESTS - PASSWORD ==========

    @Test
    void testValidEmailAndIncorrectPassword() throws Exception {
        // EC1: Valid existing email + EC7: Incorrect password
        boolean result = controller.validatePassword("test@example.com", "wrongpassword");
        assertFalse(result, "Valid email with incorrect password should return false");
    }

    @Test
    void testValidEmailAndEmptyPassword() throws Exception {
        // EC1: Valid existing email + EC8: Empty password
        boolean result = controller.validatePassword("test@example.com", "");
        assertFalse(result, "Valid email with empty password should return false");
    }

    @Test
    void testValidEmailAndNullPassword() throws Exception {
        // EC1: Valid existing email + EC9: Null password
        // The MD5 helper doesn't handle null - it throws NullPointerException
        assertThrows(NullPointerException.class,
                () -> controller.validatePassword("test@example.com", null),
                "Null password should throw NullPointerException (bug in MD5 helper)");
    }

    // ========== ADDITIONAL EDGE CASE TESTS ==========

    @Test
    void testPasswordWithWhitespace() throws Exception {
        // Password with leading/trailing spaces won't match MD5 hash
        boolean result = controller.validatePassword("test@example.com", " password ");
        assertFalse(result, "Password with whitespace should not match");
    }

    @Test
    void testSQLInjectionAttemptInEmail() throws Exception {
        // Test that PreparedStatement prevents SQL injection
        boolean result = controller.validatePassword("' OR '1'='1", "password");
        assertFalse(result, "SQL injection attempt should return false");
    }

    @Test
    void testEmptyEmailAndPassword() throws Exception {
        // EC4 + EC8: Both empty
        boolean result = controller.validatePassword("", "");
        assertFalse(result, "Both empty email and password should return false");
    }

    @Test
    void testNullEmailAndPassword() throws Exception {
        // EC5 + EC9: Both null
        // When email is null, the SQL query doesn't match any rows,
        // so the method returns false before the MD5 helper is called with null password
        boolean result = controller.validatePassword(null, null);
        assertFalse(result, "Both null email and password should return false (no DB match)");
    }
}