package com.dfms.dairy_farm_management_system.helpers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//Equivalence Class Testing
class HelperMD5Test {

    //Part 1
    @Test
    void testValidMatch() {
        String password = "password123";
        String hash = Helper.encryptPassword(password);
        assertTrue(Helper.MD5(hash, password));
    }

    //Part 1
    @Test
    void testInvalidMatch() {
        String password = "password123";
        String wrongHash = Helper.encryptPassword("otherpassword");
        assertFalse(Helper.MD5(wrongHash, password));
    }

    //Part 1
    @Test
    void testEmptyPassword() {
        String password = "";
        String hash = Helper.encryptPassword(password);
        assertTrue(Helper.MD5(hash, password));
    }

    //Part 1
    @Test
    void testUnicodePassword() {
        String password = "pässwörd✓";
        String hash = Helper.encryptPassword(password);
        assertTrue(Helper.MD5(hash, password));
    }

    //Part 1
    @Test
    void testNullPassword() {
        assertThrows(NullPointerException.class, () -> Helper.MD5("anyhash", null));
    }

    //Part 1
    @Test
    void testNullEncryptedPassword() {
        String password = "password123";
        assertFalse(Helper.MD5(null, password));
    }

    //Part 1
    @Test
    void testEncryptedPasswordNotMd5Format() {
        assertFalse(Helper.MD5("not-a-hash", "password123"));
    }
}
