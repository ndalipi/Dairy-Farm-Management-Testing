package com.dfms.dairy_farm_management_system.controllers;

import com.dfms.dairy_farm_management_system.Main;
import com.dfms.dairy_farm_management_system.connection.DBConfig;
import com.dfms.dairy_farm_management_system.connection.Session;
import com.dfms.dairy_farm_management_system.models.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dfms.dairy_farm_management_system.connection.DBConfig.getConnection;
import static com.dfms.dairy_farm_management_system.helpers.Helper.*;

public class LoginController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    private static final String COL_EMAIL = "email";
    private static final String COL_PASSWORD = "password";

    private static final String PREFILL_PROP = "dfms.debug.prefillLogin";

    @FXML private Circle close_btn;
    @FXML TextField email_input;
    @FXML private Label forget_password;
    @FXML Button login_btn;
    @FXML PasswordField password_input;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        email_input.setText(getEmail());

        if (Boolean.getBoolean(PREFILL_PROP)) {
            password_input.setText(DEFAULT_PASSWORD);
        } else {
            password_input.clear();
        }
    }

    public String getEmail() {
        String query = "SELECT email FROM `users` ORDER BY `users`.`id` ASC LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getString(COL_EMAIL);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to fetch default email", e);
        }

        return "";
    }

    @FXML
    void login(MouseEvent event) {
        Node source = (Node) event.getSource();
        handleLogin(source);
    }

    @FXML
    void loginWithEnter(KeyEvent event) {
        if (!"ENTER".equals(event.getCode().toString())) return;
        Node source = (Node) event.getSource();
        handleLogin(source);
    }

    private void handleLogin(Node sourceNode) {
        String emailRaw = (email_input.getText() == null) ? "" : email_input.getText();
        String passRaw = (password_input.getText() == null) ? "" : password_input.getText();

        String email = emailRaw.trim();
        String password = passRaw.trim();

        if (email.isBlank() || password.isBlank()) {
            displayAlert("Error", "Please fill the required fields!", Alert.AlertType.ERROR);
            return;
        }

        try {
            if (!validatePassword(email, password)) {
                displayAlert("Invalid email or password",
                        "Please check your email and password and try again",
                        Alert.AlertType.ERROR);
                return;
            }

            User user = loadUserByEmail(email);
            if (user == null) {
                displayAlert("Invalid email or password",
                        "Please check your email and password and try again",
                        Alert.AlertType.ERROR);
                return;
            }

            Session.setCurrentUser(user);
            switchToMainLayout(sourceNode);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Login failed due to DB error", e);
            displayAlert("Error", "Error occurred while trying to login.", Alert.AlertType.ERROR);
        }
    }

    private User loadUserByEmail(String email) throws SQLException {
        String userQuery = "SELECT * FROM `users` WHERE email = ? LIMIT 1";
        String employeeQuery = "SELECT * FROM `employees` WHERE email = ? LIMIT 1";

        User user = null;

        try (Connection connection = getConnection();
             PreparedStatement userStmt = connection.prepareStatement(userQuery);
             PreparedStatement empStmt = connection.prepareStatement(employeeQuery)) {

            userStmt.setString(1, email);

            try (ResultSet rs = userStmt.executeQuery()) {
                if (rs.next()) {
                    user = new User();
                    user.setId(rs.getInt("id"));
                    user.setFirstName(rs.getString("first_name"));
                    user.setLastName(rs.getString("last_name"));
                    user.setEmail(rs.getString(COL_EMAIL));
                    user.setEncryptedPassword(rs.getString(COL_PASSWORD));
                    user.setRole(rs.getInt("role"));
                    user.setSalary(rs.getFloat("salary"));
                    user.setGender(rs.getString("gender"));
                    user.setPhone(rs.getString("phone"));
                    user.setAdress(rs.getString("address"));
                    user.setCin(rs.getString("cin"));
                    user.setCreatedAt(rs.getTimestamp("created_at"));
                    user.setUpdatedAt(rs.getTimestamp("updated_at"));
                } else {
                    return null;
                }
            }

            empStmt.setString(1, email);
            try (ResultSet rsEmp = empStmt.executeQuery()) {
                if (rsEmp.next()) {
                    user.setHireDate(rsEmp.getDate("hire_date"));
                    user.setContractType(rsEmp.getString("contract_type"));
                }
            }
        }

        return user;
    }

    public boolean validatePassword(String email, String password) {
        String query = "SELECT `password` FROM `users` WHERE email = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return MD5(resultSet.getString(COL_PASSWORD), password);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Password validation failed", e);
        }

        return false;
    }

    @FXML
    private void exitApplication(MouseEvent event) {
        DBConfig.disconnect();
        System.exit(0);
    }

    private void switchToMainLayout(Node sourceNode) {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main_layout.fxml"));
        try {
            Scene scene = new Scene(fxmlLoader.load());

            Stage stage = new Stage();
            stage.setTitle("Dairy Farm Management System");
            stage.getIcons().add(new Image("file:src/main/resources/images/logo.png"));
            stage.setScene(scene);

            sourceNode.getScene().getWindow().hide();

            stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to open main layout", e);
            displayAlert("Error", "Failed to open main layout.", Alert.AlertType.ERROR);
        }
    }

    //method created to do BVT
    public boolean isValidLoginAttempts(int attempts) {
        return attempts >= 0 && attempts <= 5;
    }

}
