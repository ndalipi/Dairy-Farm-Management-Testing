package com.dfms.dairy_farm_management_system.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Equivalence Class Testing (SAFE - does not modify real DB data)
 * Class under test: Client
 * Method under test: update()
 *
 * Safety approach:
 *  - Use IDs that should not exist (e.g., Integer.MAX_VALUE) so UPDATE affects 0 rows
 *  - Result should be false, and no real data is changed.
 *
 * Equivalence classes (based on typical update input partitions):
 *  EC1: Valid-looking fields + non-existing ID -> update should fail (0 rows affected)
 *  EC2: Invalid ID (0 or negative)            -> update should fail
 *  EC3: Invalid email format                  -> update should fail (still 0 rows due to non-existing ID)
 *  EC4: Missing required field (empty name)   -> update should fail (still 0 rows due to non-existing ID)
 *
 * Note:
 *  Client.update() does not validate fields itself; it relies on DB update result.
 *  These tests are designed to be non-destructive.
 */
class ClientUpdateEquivalenceTest {

    private Client buildClientWithId(int id) {
        Client c = new Client();
        c.setId(id);
        c.setName("Test Client");
        c.setType("Regular");
        c.setPhone("+355691234567");
        c.setEmail("test@example.com");
        return c;
    }

    @Test
    void update_validFieldsButNonExistingId_equivalenceClass_shouldReturnFalse() {
        Client client = buildClientWithId(Integer.MAX_VALUE); // should not exist

        assertDoesNotThrow(() -> {
            boolean result = client.update();
            assertFalse(result, "Non-existing ID must not update any row (safe, no data change).");
        });
    }

    @Test
    void update_invalidIdZero_equivalenceClass_shouldReturnFalse() {
        Client client = buildClientWithId(0);

        assertDoesNotThrow(() -> {
            boolean result = client.update();
            assertFalse(result, "ID=0 should not update any row.");
        });
    }

    @Test
    void update_invalidEmail_equivalenceClass_shouldReturnFalse() {
        Client client = buildClientWithId(Integer.MAX_VALUE); // still safe
        client.setEmail("invalid-email");

        assertDoesNotThrow(() -> {
            boolean result = client.update();
            assertFalse(result, "Invalid email with non-existing ID must not change data.");
        });
    }

    @Test
    void update_missingName_equivalenceClass_shouldReturnFalse() {
        Client client = buildClientWithId(Integer.MAX_VALUE); // still safe
        client.setName("");

        assertDoesNotThrow(() -> {
            boolean result = client.update();
            assertFalse(result, "Empty name with non-existing ID must not change data.");
        });
    }
}