package com.dfms.dairy_farm_management_system.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.text.Text;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.ResourceBundle;

import static com.dfms.dairy_farm_management_system.connection.DBConfig.executeQuery;
import static com.dfms.dairy_farm_management_system.connection.DBConfig.getConnection;
import static com.dfms.dairy_farm_management_system.helpers.Helper.displayAlert;

public class DashboardController implements Initializable {

    private static final String ERROR_TITLE = "Error";

    private static final String COUNT_SQL_PREFIX = "SELECT COUNT(*) FROM ";
    private static final String SQL_SUM_PRICE = "SELECT SUM(price) FROM ";

    private static final String TABLE_ANIMALS = "animals";
    private static final String TABLE_ANIMALS_SALES = "animals_sales";
    private static final String TABLE_MILK_SALES = "milk_sales";

    private static final String TYPE_COW = "cow";
    private static final String TYPE_CALF = "calf";
    private static final String TYPE_BULL = "bull";

    private static final String DAY_SUN = "Sun";
    private static final String DAY_MON = "Mon";
    private static final String DAY_TUE = "Tue";
    private static final String DAY_WED = "Wed";
    private static final String DAY_THU = "Thu";
    private static final String DAY_FRI = "Fri";
    private static final String DAY_SAT = "Sat";

    private static final Map<String, String> DAY_NAME = Map.of(
            DAY_SUN, "Sunday",
            DAY_MON, "Monday",
            DAY_TUE, "Tuesday",
            DAY_WED, "Wednesday",
            DAY_THU, "Thursday",
            DAY_FRI, "Friday",
            DAY_SAT, "Saturday"
    );

    private final Connection connection = getConnection();

    @FXML private Text today_earnings;
    @FXML private Text today_sales;
    @FXML private Text total_bulls;
    @FXML private Text total_calfs;
    @FXML private Text total_clients;
    @FXML private Text total_cows;
    @FXML private Text total_products;
    @FXML private Text total_suppliers;
    @FXML private Text total_employees;

    @FXML private BarChart<String, Number> barChart;
    @FXML private PieChart pieChart;
    @FXML private LineChart<String, Number> lineChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            initDashboard();
        } catch (SQLException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }

        fillPieChart();
        fillBarChart();
        fillLineChart();
    }

    public void initDashboard() throws SQLException {
        total_employees.setText(String.valueOf(countAll("employees")));
        total_suppliers.setText(String.valueOf(countAll("suppliers")));
        total_products.setText(String.valueOf(countAll("stocks")));
        total_clients.setText(String.valueOf(countAll("clients")));

        total_cows.setText(String.valueOf(countAnimalsOfType(TYPE_COW)));
        total_calfs.setText(String.valueOf(countAnimalsOfType(TYPE_CALF)));
        total_bulls.setText(String.valueOf(countAnimalsOfType(TYPE_BULL)));

        int salesToday = countTodaySales(TABLE_ANIMALS_SALES);
        today_sales.setText(String.valueOf(salesToday));

        int earningsToday = sumTodayPrice(TABLE_ANIMALS_SALES);
        today_earnings.setText("$" + earningsToday);
    }

    public void fillPieChart() {
        try {
            int cows = countAnimalsOfType(TYPE_COW);
            int calfs = countAnimalsOfType(TYPE_CALF);
            int bulls = countAnimalsOfType(TYPE_BULL);

            total_cows.setText(String.valueOf(cows));
            total_calfs.setText(String.valueOf(calfs));
            total_bulls.setText(String.valueOf(bulls));

            pieChart.getData().clear();
            pieChart.getData().addAll(
                    new PieChart.Data("Cows", cows),
                    new PieChart.Data("Calfs", calfs),
                    new PieChart.Data("Bulls", bulls)
            );
        } catch (SQLException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void fillBarChart() {
        xAxis.setLabel("Days");
        yAxis.setLabel("Sales");

        XYChart.Series<String, Number> animalSales = new XYChart.Series<>();
        XYChart.Series<String, Number> milkSales = new XYChart.Series<>();

        animalSales.setName("Animal Sales");
        milkSales.setName("Milk Sales");

        addWeekSeries(animalSales, TABLE_ANIMALS_SALES);
        addWeekSeries(milkSales, TABLE_MILK_SALES);

        barChart.getData().clear();
        barChart.getData().addAll(animalSales, milkSales);
    }

    public void fillLineChart() {
        xAxis.setLabel("Days");
        yAxis.setLabel("Count of animals sales");

        XYChart.Series<String, Number> data = new XYChart.Series<>();
        addWeekEarningsSeries(data);

        lineChart.getData().clear();
        lineChart.getData().add(data);
    }

    public int getSalesOfSpecificDay(String day, String table) {
        String dayName = DAY_NAME.get(day);
        if (dayName == null) return 0;

        String sql = "SELECT COUNT(*) FROM " + table +
                " WHERE DAYNAME(sale_date) = ? AND WEEK(sale_date) = WEEK(CURDATE())";

        return queryInt(sql, dayName);
    }

    public int getEarningsOfSpecificDay(String day) {
        String dayName = DAY_NAME.get(day);
        if (dayName == null) return 0;

        String sql = "SELECT " +
                "COALESCE((SELECT SUM(price) FROM " + TABLE_ANIMALS_SALES + " WHERE DAYNAME(sale_date)=? AND WEEK(sale_date)=WEEK(CURDATE())), 0) +" +
                "COALESCE((SELECT SUM(price) FROM " + TABLE_MILK_SALES + " WHERE DAYNAME(sale_date)=? AND WEEK(sale_date)=WEEK(CURDATE())), 0)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, dayName);
            ps.setString(2, dayName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
            return 0;
        }
    }

    private void addWeekSeries(XYChart.Series<String, Number> series, String table) {
        addDayPoint(series, DAY_SUN, table);
        addDayPoint(series, DAY_MON, table);
        addDayPoint(series, DAY_TUE, table);
        addDayPoint(series, DAY_WED, table);
        addDayPoint(series, DAY_THU, table);
        addDayPoint(series, DAY_FRI, table);
        addDayPoint(series, DAY_SAT, table);
    }

    private void addDayPoint(XYChart.Series<String, Number> series, String day, String table) {
        series.getData().add(new XYChart.Data<>(day, getSalesOfSpecificDay(day, table)));
    }

    private void addWeekEarningsSeries(XYChart.Series<String, Number> series) {
        addEarningPoint(series, DAY_SUN);
        addEarningPoint(series, DAY_MON);
        addEarningPoint(series, DAY_TUE);
        addEarningPoint(series, DAY_WED);
        addEarningPoint(series, DAY_THU);
        addEarningPoint(series, DAY_FRI);
        addEarningPoint(series, DAY_SAT);
    }

    private void addEarningPoint(XYChart.Series<String, Number> series, String day) {
        series.getData().add(new XYChart.Data<>(day, getEarningsOfSpecificDay(day)));
    }

    private int countAll(String table) throws SQLException {
        try (ResultSet rs = executeQuery(COUNT_SQL_PREFIX + table)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int countAnimalsOfType(String type) throws SQLException {
        String sql = COUNT_SQL_PREFIX + TABLE_ANIMALS + " WHERE type = ?";
        return queryInt(sql, type);
    }

    private int countTodaySales(String table) throws SQLException {
        String sql = COUNT_SQL_PREFIX + table + " WHERE sale_date = CURDATE()";
        return queryInt(sql);
    }

    private int sumTodayPrice(String table) throws SQLException {
        String sql = SQL_SUM_PRICE + table + " WHERE sale_date = CURDATE()";
        try (ResultSet rs = executeQuery(sql)) {
            if (!rs.next()) return 0;
            int v = rs.getInt(1);
            return rs.wasNull() ? 0 : v;
        }
    }

    private int queryInt(String sql, String... params) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
            return 0;
        }
    }

    //method created to do BVT
    public double clampDailyEarnings(double earnings) {
        if (earnings < 0) return 0;
        if (earnings > 1_000_000) return 1_000_000;
        return earnings;
    }
    // method created to demonstrate Statement/Branch/Condition/MC/DC coverage
    public boolean isValidEarningsInput(Double earnings, boolean allowNull) {
        if ((earnings == null && !allowNull) || (earnings != null && earnings < 0)) {
            return false;
        }
        return true;
    }


}
