package com.dfms.dairy_farm_management_system.models;

import com.dfms.dairy_farm_management_system.connection.DBConfig;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeCRUDIntegrationTest {

    private Employee employee;
    private String cin;
    private String email;
    private String phone;

    @BeforeEach
    void setUp() {
        String tag = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        cin = ("CIN_" + tag).toUpperCase();
        email = "employee_" + tag + "@test.com";
        phone = "+35569" + (10000000 + (int)(Math.random() * 90000000));

        employee = new Employee();
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setGender("Male");
        employee.setCin(cin);
        employee.setEmail(email);
        employee.setPhone(phone);
        employee.setAdress("Test Address");
        employee.setSalary(1200.0f);
        employee.setHireDate(Date.valueOf("2025-01-01"));
        employee.setContractType("CDI");

        deleteByCin(cin);
    }

    @AfterEach
    void tearDown() {
        deleteByCin(cin);
    }

    @Test
    void save_validEmployee_shouldReturnTrue() {
        boolean result = employee.save();

        assertTrue(result, "save() should return true when insert succeeds");
        assertTrue(existsByCin(cin), "Employee must exist in database after save");
        assertEquals(email, getEmailByCin(cin), "Saved email should match");
    }

    @Test
    void save_genderMale_shouldStoreAsM() {
        employee.setGender("Male");

        assertTrue(employee.save(), "Precondition failed: save() must succeed");
        assertEquals("M", getGenderByCin(cin), "Gender 'Male' should be stored as 'M'");
    }

    @Test
    void save_genderFemale_shouldStoreAsF() {
        employee.setGender("Female");

        assertTrue(employee.save(), "Precondition failed: save() must succeed");
        assertEquals("F", getGenderByCin(cin), "Gender 'Female' should be stored as 'F'");
    }

    @Test
    void save_lowercaseCin_shouldStoreAsUppercase() {
        employee.setCin(cin.toLowerCase());

        assertTrue(employee.save(), "Precondition failed: save() must succeed");
        assertTrue(existsByCin(cin.toUpperCase()), "CIN should be stored as uppercase");
    }

    @Test
    void save_duplicateCin_shouldReturnFalse() {
        assertTrue(employee.save(), "Precondition failed: first save() must succeed");

        String dupTag = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        Employee duplicate = new Employee();
        duplicate.setFirstName("Duplicate");
        duplicate.setLastName("Employee");
        duplicate.setGender("Female");
        duplicate.setCin(cin);
        duplicate.setEmail("dup_" + dupTag + "@test.com");
        duplicate.setPhone("+35569" + (10000000 + (int)(Math.random() * 90000000)));
        duplicate.setAdress("Different Address");
        duplicate.setSalary(2000.0f);
        duplicate.setHireDate(Date.valueOf("2025-02-01"));
        duplicate.setContractType("CDD");

        boolean result = duplicate.save();

        assertFalse(result, "Saving duplicate CIN should return false");
    }

    @Test
    void save_allFields_shouldStoreCorrectly() {
        assertTrue(employee.save(), "Precondition failed: save() must succeed");

        assertEquals("John", getFirstNameByCin(cin));
        assertEquals("Doe", getLastNameByCin(cin));
        assertEquals("M", getGenderByCin(cin));
        assertEquals(email, getEmailByCin(cin));
        assertEquals(phone, getPhoneByCin(cin));
        assertEquals("Test Address", getAddressByCin(cin));
        assertEquals(1200.0f, getSalaryByCin(cin), 0.01f);
        assertEquals("CDI", getContractTypeByCin(cin));
    }

    @Test
    void update_existingEmployee_shouldReturnTrue() {
        assertTrue(employee.save(), "Precondition failed: save() must succeed");

        employee.setFirstName("Updated");
        employee.setLastName("Name");
        employee.setSalary(2000.0f);

        boolean result = employee.update();

        assertTrue(result, "update() should return true for existing employee");
    }

    @Test
    void update_allFields_shouldModifyCorrectly() {
        assertTrue(employee.save(), "Precondition failed: save() must succeed");

        employee.setFirstName("Updated");
        employee.setLastName("Employee");
        employee.setGender("Female");
        employee.setPhone("+355691111111");
        employee.setAdress("New Address");
        employee.setSalary(2500.0f);
        employee.setContractType("CTT");

        assertTrue(employee.update(), "Precondition failed: update() must succeed");

        assertEquals("Updated", getFirstNameByCin(cin));
        assertEquals("Employee", getLastNameByCin(cin));
        assertEquals("F", getGenderByCin(cin));
        assertEquals("+355691111111", getPhoneByCin(cin));
        assertEquals("New Address", getAddressByCin(cin));
        assertEquals(2500.0f, getSalaryByCin(cin), 0.01f);
        assertEquals("CTT", getContractTypeByCin(cin));
    }

    @Test
    void update_nonExistingEmployee_shouldReturnFalse() {
        boolean result = employee.update();

        assertFalse(result, "update() should return false for non-existing employee");
    }

    @Test
    void delete_existingEmployee_shouldReturnTrue() {
        assertTrue(employee.save(), "Precondition failed: save() must succeed");
        assertTrue(existsByCin(cin), "Precondition: employee must exist");

        boolean result = employee.delete();

        assertTrue(result, "delete() should return true for existing employee");
        assertFalse(existsByCin(cin), "Employee should not exist after delete");
    }

    @Test
    void delete_nonExistingEmployee_shouldReturnFalse() {
        boolean result = employee.delete();

        assertFalse(result, "delete() should return false for non-existing employee");
    }

    @Test
    void delete_cinCaseInsensitive_shouldWork() {
        assertTrue(employee.save(), "Precondition failed: save() must succeed");

        employee.setCin(cin.toLowerCase());
        boolean result = employee.delete();

        assertTrue(result, "delete() should work with lowercase CIN");
        assertFalse(existsByCin(cin), "Employee should be deleted regardless of CIN case");
    }

    private static boolean existsByCin(String cin) {
        String sql = "SELECT COUNT(*) FROM employees WHERE cin = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cin.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DBConfig.disconnect();
        }
    }

    private static void deleteByCin(String cin) {
        String sql = "DELETE FROM employees WHERE cin = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cin.toUpperCase());
            ps.executeUpdate();
        } catch (SQLException ignored) {
        } finally {
            DBConfig.disconnect();
        }
    }

    private static String getEmailByCin(String cin) {
        return getStringField(cin, "email");
    }

    private static String getFirstNameByCin(String cin) {
        return getStringField(cin, "first_name");
    }

    private static String getLastNameByCin(String cin) {
        return getStringField(cin, "last_name");
    }

    private static String getGenderByCin(String cin) {
        return getStringField(cin, "gender");
    }

    private static String getPhoneByCin(String cin) {
        return getStringField(cin, "phone");
    }

    private static String getAddressByCin(String cin) {
        return getStringField(cin, "address");
    }

    private static String getContractTypeByCin(String cin) {
        return getStringField(cin, "contract_type");
    }

    private static float getSalaryByCin(String cin) {
        String sql = "SELECT salary FROM employees WHERE cin = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cin.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Float.NaN;
                return rs.getFloat(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DBConfig.disconnect();
        }
    }

    private static String getStringField(String cin, String field) {
        String sql = "SELECT " + field + " FROM employees WHERE cin = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cin.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getString(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DBConfig.disconnect();
        }
    }
}