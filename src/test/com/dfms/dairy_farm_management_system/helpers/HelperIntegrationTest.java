package com.dfms.dairy_farm_management_system.helpers;

import com.dfms.dairy_farm_management_system.controllers.LoginController;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.Map;
import java.util.UUID;

import static com.dfms.dairy_farm_management_system.connection.DBConfig.getConnection;
import static com.dfms.dairy_farm_management_system.helpers.Helper.encryptPassword;
import static org.junit.jupiter.api.Assertions.*;

class HelperIntegrationTest {

    // DB test data (unique per test to avoid UNIQUE constraint failures)
    private String testEmail;
    private String testCin;
    private String testPhone;
    private int insertedUserId;

    @BeforeEach
    void setUp() {
        String tag = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        testEmail = "integration_" + tag + "@test.com";
        testCin = "CIN_" + tag;
        testPhone = "06" + tag;
        insertedUserId = -1;
    }

    @AfterEach
    void tearDown() {
        // Only cleanup if we actually inserted a user
        if (insertedUserId == -1) return;

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setInt(1, insertedUserId);
            ps.executeUpdate();
        } catch (SQLException ignored) {
            // If cleanup fails, the TEST DB can be cleaned manually later.
        }
    }

    // DB Integration: LoginController.validatePassword + Helper hashing

    @Test
    void validatePassword_shouldReturnTrueForCorrectPassword() throws Exception {
        String plainPassword = "Pass123!";

        insertedUserId = insertTestUser(testEmail, encryptPassword(plainPassword));

        LoginController controller = new LoginController();
        assertTrue(controller.validatePassword(testEmail, plainPassword),
                "validatePassword should return true for correct credentials");
    }

    @Test
    void validatePassword_shouldReturnFalseForWrongPassword() throws Exception {
        String plainPassword = "Pass123!";

        insertedUserId = insertTestUser(testEmail, encryptPassword(plainPassword));

        LoginController controller = new LoginController();
        assertFalse(controller.validatePassword(testEmail, "Wrong123!"),
                "validatePassword should return false for wrong password");
    }

    // -------------------------
    // 7 ADDITIONAL TESTS
    // -------------------------

    @Test
    void validatePassword_nonExistingEmail_returnsFalse() {
        LoginController controller = new LoginController();

        boolean result = assertDoesNotThrow(
                () -> controller.validatePassword("does_not_exist_" + System.nanoTime() + "@test.com", "Pass123!"),
                "validatePassword should not throw for non-existing user"
        );
        assertFalse(result, "validatePassword should return false if email doesn't exist");
    }

    @Test
    void validatePassword_sqlInjectionLikeEmail_returnsFalse() throws Exception {
        String plainPassword = "Pass123!";
        insertedUserId = insertTestUser(testEmail, encryptPassword(plainPassword));

        LoginController controller = new LoginController();

        String injection = "' OR '1'='1";
        boolean result = assertDoesNotThrow(
                () -> controller.validatePassword(injection, plainPassword),
                "validatePassword should not throw for suspicious input"
        );

        assertFalse(result, "validatePassword must not be bypassed by SQL injection-like input");
    }

    @Test
    void validatePassword_nullEmail_returnsFalseAndDoesNotThrow() {
        LoginController controller = new LoginController();

        boolean result = assertDoesNotThrow(
                () -> controller.validatePassword(null, "Pass123!"),
                "validatePassword should handle null email safely"
        );

        assertFalse(result, "validatePassword should return false for null email");
    }

    @Test
    void validatePassword_nullPassword_returnsFalseAndDoesNotThrow() throws Exception {
        String plainPassword = "Pass123!";
        insertedUserId = insertTestUser(testEmail, encryptPassword(plainPassword));

        LoginController controller = new LoginController();

        boolean result = assertDoesNotThrow(
                () -> controller.validatePassword(testEmail, null),
                "validatePassword should handle null password safely"
        );

        assertFalse(result, "validatePassword should return false for null password");
    }

    @Test
    void validatePassword_emptyPassword_returnsFalse() throws Exception {
        String plainPassword = "Pass123!";
        insertedUserId = insertTestUser(testEmail, encryptPassword(plainPassword));

        LoginController controller = new LoginController();
        boolean result = assertDoesNotThrow(
                () -> controller.validatePassword(testEmail, ""),
                "validatePassword should not throw for empty password"
        );

        assertFalse(result, "validatePassword should return false for empty password");
    }

    @Test
    void insertTestUser_shouldInsertRowAndReturnValidId() throws Exception {
        String plainPassword = "Pass123!";
        insertedUserId = insertTestUser(testEmail, encryptPassword(plainPassword));

        assertTrue(insertedUserId > 0, "Inserted user id should be a positive number");

        // Verify row exists and ID matches DB record
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id FROM users WHERE email = ? LIMIT 1")) {
            ps.setString(1, testEmail);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Inserted user should exist in DB");
                assertEquals(insertedUserId, rs.getInt("id"), "DB id should match returned generated id");
            }
        }
    }

    @Test
    void insertTestUser_roleOrRoleIdColumn_shouldBeSetToOne() throws Exception {
        String plainPassword = "Pass123!";
        insertedUserId = insertTestUser(testEmail, encryptPassword(plainPassword));

        try (Connection con = getConnection()) {
            String roleColumn = columnExists(con, "users", "role") ? "role" : "role_id";

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT " + roleColumn + " FROM users WHERE id = ? LIMIT 1"
            )) {
                ps.setInt(1, insertedUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next(), "Expected inserted user row to be found");
                    assertEquals(1, rs.getInt(roleColumn), roleColumn + " should be set to 1 for inserted test user");
                }
            }
        }
    }
    @Test
    @Disabled("Enable only if DBConfig points to a SAFE TEST database with a 'roles' table")
    void getRoles_shouldReturnInsertedRoleFromDatabase() throws Exception {
        int roleId;
        // Use a short role name in case roles.name has a small VARCHAR limit
        String roleName = ("T" + System.nanoTime()).substring(0, 4);

        // Insert role
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO roles(name) VALUES (?)",
                     Statement.RETURN_GENERATED_KEYS
             )) {
            ps.setString(1, roleName);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                assertTrue(rs.next(), "Expected generated key for inserted role");
                roleId = rs.getInt(1);
            }
        }

        try {
            Map<String, Integer> roles = Helper.getRoles();
            assertNotNull(roles, "Helper.getRoles() should not return null");
            assertTrue(roles.containsKey(roleName), "Roles map should contain inserted role name");
            assertEquals(roleId, roles.get(roleName), "Role id should match the inserted role");
        } finally {
            // Cleanup role row
            try (Connection con = getConnection();
                 PreparedStatement ps = con.prepareStatement("DELETE FROM roles WHERE id = ?")) {
                ps.setInt(1, roleId);
                ps.executeUpdate();
            }
        }
    }

    // Helpers
    /*
     * Inserts a user into DB in a schema-flexible way (role vs role_id).
     * Returns the generated user id.
     */
    private int insertTestUser(String email, String hashedPassword) throws SQLException {
        try (Connection con = getConnection()) {
            String roleColumn = columnExists(con, "users", "role") ? "role" : "role_id";

            String sql = "INSERT INTO users (" +
                    roleColumn + ", password, first_name, last_name, gender, cin, phone, salary, email, address" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, 1); // admin-ish role or role_id
                ps.setString(2, hashedPassword);
                ps.setString(3, "Integration");
                ps.setString(4, "Test");
                ps.setString(5, "M");          // must match enum/constraint in DB
                ps.setString(6, testCin);      // unique
                ps.setString(7, testPhone);    // unique
                ps.setFloat(8, 1000f);
                ps.setString(9, email);
                ps.setString(10, "Test Address");

                int affected = ps.executeUpdate();
                assertEquals(1, affected, "Expected exactly 1 inserted user row");

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) return keys.getInt(1);
                }
            }

            // Fallback if getGeneratedKeys fails
            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM users WHERE email = ? LIMIT 1")) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("id");
                }
            }
        }

        fail("Could not insert test user or fetch its id.");
        return -1;
    }

    private boolean columnExists(Connection con, String table, String column) throws SQLException {
        DatabaseMetaData meta = con.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, table, column)) {
            return rs.next();
        }
    }
}
