package com.dfms.dairy_farm_management_system.controllers;

import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

class DashboardControllerEarningsEquivalenceTest {

    DashboardController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new DashboardController();

        // Inject a fake connection that returns controlled results
        Connection fakeConnection = fakeConnectionReturning(100, true, false);
        inject(controller, "connection", fakeConnection);
    }

    @Test
    void validDay_Sun_returnsEarningsFromResultSet() {
        int earnings = controller.getEarningsOfSpecificDay("Sun");
        assertEquals(100, earnings);
    }

    @Test
    void invalidDay_returnsZero() {
        assertEquals(0, controller.getEarningsOfSpecificDay("Sund"));
    }

    @Test
    void emptyDay_returnsZero() {
        assertEquals(0, controller.getEarningsOfSpecificDay(""));
    }

    // -----------------------
    // EC4: Null day
    // -----------------------
    @Test
    void nullDay_returnsZero() {
        assertEquals(0, controller.getEarningsOfSpecificDay(null));
    }

    @Test
    void validDay_resultSetNextFalse_returnsZero() throws Exception {
        inject(controller, "connection", fakeConnectionReturning(0, false, false));
        assertEquals(0, controller.getEarningsOfSpecificDay("Tue"));
    }

    @Test
    void validDay_executeQueryThrowsSQLException_returnsZero() throws Exception {
        inject(controller, "connection", fakeConnectionReturning(0, true, true));
        assertEquals(0, controller.getEarningsOfSpecificDay("Wed"));
    }

    private static Connection fakeConnectionReturning(int value, boolean nextReturnsTrue, boolean throwOnExecute) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("prepareStatement")) {
                        return fakePreparedStatementReturning(value, nextReturnsTrue, throwOnExecute);
                    }
                    if (method.getName().equals("close")) return null;
                    throw new UnsupportedOperationException("Not implemented: " + method.getName());
                }
        );
    }

    private static PreparedStatement fakePreparedStatementReturning(int value, boolean nextReturnsTrue, boolean throwOnExecute) {
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "setString":
                            return null;
                        case "executeQuery":
                            if (throwOnExecute) throw new SQLException("db error");
                            return fakeResultSetReturning(value, nextReturnsTrue);
                        case "close":
                            return null;
                        default:
                            throw new UnsupportedOperationException("Not implemented: " + method.getName());
                    }
                }
        );
    }

    private static ResultSet fakeResultSetReturning(int value, boolean nextReturnsTrue) {
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class[]{ResultSet.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "next":
                            return nextReturnsTrue;
                        case "getInt":
                            return value;
                        case "close":
                            return null;
                        default:
                            throw new UnsupportedOperationException("Not implemented: " + method.getName());
                    }
                }
        );
    }

    // Reflection helper
    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
