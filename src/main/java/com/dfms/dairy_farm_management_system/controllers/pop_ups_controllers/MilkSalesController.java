package com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers;

import com.dfms.dairy_farm_management_system.connection.DBConfig;
import com.dfms.dairy_farm_management_system.models.AnimalSale;
import com.dfms.dairy_farm_management_system.models.MilkSale;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.ResourceBundle;

import static com.dfms.dairy_farm_management_system.connection.DBConfig.getConnection;
import static com.dfms.dairy_farm_management_system.helpers.Helper.*;
import static com.dfms.dairy_farm_management_system.helpers.Helper.displayAlert;

public class MilkSalesController implements Initializable {
    private static final String ERROR_TITLE = "Error";
    private static final String SUCCESS_TITLE = "success";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            this.setClientsList();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load clients list", e);

        }
        validateDecimalInput(priceOfSale);
        validateDecimalInput(quantityInput);
    }

    @FXML
    ComboBox<String> clientsCombo;
    @FXML
    DatePicker operationDate;
    @FXML
    TextField quantityInput;
    @FXML
    TextField priceOfSale;

    ObservableList<String> clientsList;

    HashMap<String, Integer> clients = new HashMap<>();

    PreparedStatement statement = null;
    ResultSet resultSet = null;

    @FXML
    private Button add_update;

    @FXML
    private Label header;

    @FXML
    private Label key;
    private int MilkSale_ID;

    public void setClientsList() throws SQLException {
        ObservableList<String> client = FXCollections.observableArrayList();

        String select_query = "SELECT name, id from clients ";

        statement = DBConfig.getConnection().prepareStatement(select_query);
        resultSet = statement.executeQuery();
        while (resultSet.next()) {
            clients.put(resultSet.getString("name"), resultSet.getInt("id"));
            client.add(resultSet.getString("name"));
        }

        clientsCombo.setItems(client);
    }

    private boolean update;

    public void setUpdate(boolean b) {
        this.update = b;
    }

    @FXML
    public void addMilkSale(MouseEvent mouseEvent) {
        MilkSale milkSale = new MilkSale();

        if (clientsCombo.getValue() == null || quantityInput.getText().isEmpty() || priceOfSale.getText().isEmpty() || operationDate.getValue() == null) {
            displayAlert(ERROR_TITLE, "Please Fill all fields ", Alert.AlertType.ERROR);
            return;
        }

        if (Float.parseFloat(priceOfSale.getText()) == 0) {
            displayAlert(ERROR_TITLE, "Price can't be null ", Alert.AlertType.ERROR);
            return;
        }

        if (!this.update && Float.parseFloat(quantityInput.getText()) == 0) {
            displayAlert(ERROR_TITLE, "Price or quantity can not  be null ", Alert.AlertType.ERROR);
            return;
        }

        if (this.update) {
            if (!milkSale.update()) {
                return;
            }

            milkSale.setId(this.MilkSale_ID);
            milkSale.setQuantity(Float.parseFloat(quantityInput.getText()));
            milkSale.setPrice(Float.parseFloat(priceOfSale.getText()));
            milkSale.setClientId(clients.get(clientsCombo.getValue()));
            milkSale.setSale_date(Date.valueOf(operationDate.getValue()));
            clear();
            closePopUp(mouseEvent);
            displayAlert(SUCCESS_TITLE, "Milk Sale Updated successfully", Alert.AlertType.INFORMATION);
            return;
        }

        milkSale.setQuantity(Float.parseFloat(quantityInput.getText()));
        milkSale.setPrice(Float.parseFloat(priceOfSale.getText()));
        milkSale.setClientId(clients.get(clientsCombo.getValue()));
        milkSale.setSale_date(Date.valueOf(operationDate.getValue()));
        if (milkSale.save()) {
            closePopUp(mouseEvent);
            displayAlert(SUCCESS_TITLE, "Sale added successfully", Alert.AlertType.INFORMATION);
        } else {
            displayAlert(ERROR_TITLE, "Error while saving!!!", Alert.AlertType.ERROR);
        }
    }


    public void fetchMilkSale(MilkSale milkSale) {
        this.MilkSale_ID = milkSale.getId();
        header.setText("Update Milk Sale");
        this.quantityInput.setText(String.valueOf(milkSale.getQuantity()));
        this.clientsCombo.setValue(milkSale.getClientName());
        this.priceOfSale.setText(String.valueOf(milkSale.getPrice()));
        this.operationDate.setValue(LocalDate.parse(milkSale.getSale_date().toString()));
        key.setText("Update");
        add_update.setText("Update");
        add_update.setOnMouseClicked(mouseEvent -> {
            milkSale.setQuantity(Float.parseFloat(quantityInput.getText()));
            milkSale.setClientId(clients.get(clientsCombo.getValue()));
            milkSale.setPrice(Float.parseFloat(priceOfSale.getText()));
            milkSale.setSale_date(Date.valueOf(operationDate.getValue()));
            if (milkSale.update()) {
                closePopUp(mouseEvent);
                displayAlert(SUCCESS_TITLE, "Milk Sale Updated successfully", Alert.AlertType.INFORMATION);
            } else {
                displayAlert(ERROR_TITLE, "Error while updating!!!", Alert.AlertType.ERROR);
            }
        });

    }

    private void clear() {
        quantityInput.clear();
        clientsCombo.getSelectionModel().clearSelection();
        priceOfSale.clear();
        operationDate.setValue(null);
        add_update.setDisable(false);
    }

    public MilkSale getSale(int milkSale_ID) {
        MilkSale milkSale = new MilkSale();

        String query = "SELECT id, quantity, price, client_id, sale_date FROM milk_sales WHERE id = ? LIMIT 1";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setInt(1, milkSale_ID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    milkSale.setId(rs.getInt("id"));
                    milkSale.setQuantity(rs.getFloat("quantity"));
                    milkSale.setPrice(rs.getFloat("price"));
                    milkSale.setClientId(rs.getInt("client_id"));
                    milkSale.setSale_date(rs.getDate("sale_date"));
                }
            }

        } catch (SQLException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }

        return milkSale;
    }

}