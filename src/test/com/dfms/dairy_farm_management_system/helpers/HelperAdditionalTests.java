package com.dfms.dairy_farm_management_system.helpers;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

//Part 3 tests
class HelperAdditionalTests {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    protected static void runFxAndWait(Runnable action) {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });

        try {
            assertTrue(latch.await(3, TimeUnit.SECONDS), "Timed out waiting for JavaFX thread");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for JavaFX thread");
        }
    }

    /*
     * TEST TYPE: Equivalence Class Testing
     * Valid email containing Unicode characters.
     */
    @Test
    void validateEmailInput_unicodeEmail_shouldBeValid() {
        TextField tf = new TextField();
        runFxAndWait(() -> Helper.validateEmailInput(tf));

        runFxAndWait(() -> tf.setText("mëri@domain.al"));
        runFxAndWait(() -> {});

        assertEquals("-fx-border-color: transparent", tf.getStyle());
    }

    /*
     * TEST TYPE: Boundary Value Testing
     * Local-part length > 64 should be invalid.
     */
    @Test
    void validateEmailInput_localPartTooLong_shouldBeInvalid() {
        TextField tf = new TextField();
        runFxAndWait(() -> Helper.validateEmailInput(tf));

        String local65 = "a".repeat(65);
        runFxAndWait(() -> tf.setText(local65 + "@test.com"));
        runFxAndWait(() -> {});

        assertEquals("-fx-border-color: red", tf.getStyle());
    }

    /*
     * TEST TYPE: Equivalence Class Testing
     * Empty string should be treated as invalid email.
     */
    @Test
    void validateEmailInput_emptyEmail_shouldBeInvalid() {
        TextField tf = new TextField();
        runFxAndWait(() -> Helper.validateEmailInput(tf));

        runFxAndWait(() -> tf.setText(""));
        runFxAndWait(() -> {});

        assertEquals("-fx-border-color: red", tf.getStyle());
    }

    /*
     * TEST TYPE: Equivalence Class Testing
     * Multiple dots are currently allowed by Helper implementation.
     * This test documents existing behavior.
     */
    @Test
    void validateDecimalInput_multipleDots_shouldRemainUnchanged() {
        TextField tf = new TextField();
        runFxAndWait(() -> Helper.validateDecimalInput(tf));

        runFxAndWait(() -> tf.setText("12..3"));
        runFxAndWait(() -> {});

        assertEquals("12..3", tf.getText());
    }

    /*
     * TEST TYPE: Equivalence Class Testing
     * Expected behavior: remove everything except digits
     */
    @Test
    void validateNumericInput_shouldStripNonDigits() {
        TextField tf = new TextField();
        runFxAndWait(() -> Helper.validateNumericInput(tf));

        runFxAndWait(() -> tf.setText("12a3!  9"));
        runFxAndWait(() -> {});

        assertEquals("1239", tf.getText());
    }

    /*
     * TEST TYPE: Equivalence Class Testing
     * Test ensures only valid phone characters survive
     */
    @Test
    void validatePhoneInput_shouldKeepPlusAndDigitsOnly() {
        TextField tf = new TextField();
        runFxAndWait(() -> Helper.validatePhoneInput(tf));

        runFxAndWait(() -> tf.setText("+355 ab 69-12 (x)"));
        runFxAndWait(() -> {});

        assertEquals("+3556912", tf.getText());
    }
    /*
     * TEST TYPE: Boundary Value Testing
     * Leading dot should be accepted.
     */
    @Test
    void validateDecimalInput_leadingDot_shouldBeAccepted() {
        TextField tf = new TextField();
        runFxAndWait(() -> Helper.validateDecimalInput(tf));

        runFxAndWait(() -> tf.setText(".5"));
        runFxAndWait(() -> {});

        assertEquals(".5", tf.getText());
    }

    /*
     * TEST TYPE: Boundary Value Testing
     * Trailing dot should be accepted.
     */
    @Test
    void validateDecimalInput_trailingDot_shouldBeAccepted() {
        TextField tf = new TextField();
        runFxAndWait(() -> Helper.validateDecimalInput(tf));

        runFxAndWait(() -> tf.setText("5."));
        runFxAndWait(() -> {});

        assertEquals("5.", tf.getText());
    }

    /*
     * TEST TYPE: Equivalence Class Testing
     * Null input should return null.
     */
    @Test
    void formatString_nullInput_shouldReturnNull() {
        assertNull(Helper.formatString(null, 10));
    }

    /*
     * TEST TYPE: Boundary Value Testing
     * Length = 0 should return original string.
     */
    @Test
    void formatString_zeroLength_shouldReturnSameString() {
        assertEquals("abc", Helper.formatString("abc", 0));
    }

    /*
     * TEST TYPE: Boundary Value Testing
     * Negative length should return original string.
     */
    @Test
    void formatString_negativeLength_shouldReturnSameString() {
        assertEquals("abc", Helper.formatString("abc", -5));
    }


    /*
     * TEST TYPE: Class Coverage + Boundary Value
     * Handler should be called once and listener removed.
     */
    @Test
    void listenToSizeInitialization_shouldCallHandlerOnce() {
        SimpleDoubleProperty size = new SimpleDoubleProperty(0.0);
        AtomicInteger calls = new AtomicInteger(0);

        Helper.listenToSizeInitialization(size, newSize -> calls.incrementAndGet());

        size.set(100.0);
        size.set(200.0);

        assertEquals(1, calls.get());
    }

}
