package com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers;

import com.dfms.dairy_farm_management_system.connection.DBConfig;
import com.dfms.dairy_farm_management_system.models.AnimalSale;
import com.dfms.dairy_farm_management_system.models.MilkCollection;
import com.dfms.dairy_farm_management_system.models.Purchase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.ResourceBundle;

import static com.dfms.dairy_farm_management_system.connection.DBConfig.disconnect;
import static com.dfms.dairy_farm_management_system.connection.DBConfig.getConnection;
import static com.dfms.dairy_farm_management_system.helpers.Helper.*;

public class NewPurchaseController implements Initializable {
    private static final String TITLE_SUCCESS = "success";
    private static final String TITLE_ERROR = "Error";

    @FXML
    private Button add_update;

    @FXML
    private Label header;

    @FXML
    private Label key;

    @FXML
    private DatePicker operationDate;

    @FXML
    private TextField priceOfSale;

    @FXML
    private TextField quantityInput;

    @FXML
    private ComboBox<String> suppliersCombo;
    private int Purchase_ID;
    HashMap<String, Integer> suppliers = new HashMap<>();
    HashMap<String, Integer> stocks = new HashMap<>();
    @FXML
    private ComboBox<String> stockCombo;
    @FXML
    private Button openAddNewProduct;
    PreparedStatement statement = null;
    ResultSet resultSet = null;

    @FXML
    void addPurchase(MouseEvent event) {
        Purchase purchase = new Purchase();

        // 1) Validate (early returns)
        if (suppliersCombo.getValue() == null ||
                stockCombo.getValue() == null ||
                operationDate.getValue() == null ||
                priceOfSale.getText() == null || priceOfSale.getText().trim().isEmpty() ||
                quantityInput.getText() == null || quantityInput.getText().trim().isEmpty()) {
            displayAlert(TITLE_ERROR, "Please Fill all fields", Alert.AlertType.ERROR);
            return;
        }

        float price;
        float qty;
        try {
            price = Float.parseFloat(priceOfSale.getText().trim());
            qty = Float.parseFloat(quantityInput.getText().trim());
        } catch (NumberFormatException e) {
            displayAlert(TITLE_ERROR, "Price and Quantity must be valid numbers", Alert.AlertType.ERROR);
            return;
        }

        if (price <= 0) {
            displayAlert(TITLE_ERROR, "Price can't be null", Alert.AlertType.ERROR);
            return;
        }

        if (qty <= 0) {
            displayAlert(TITLE_ERROR, "Quantity can't be null", Alert.AlertType.ERROR);
            return;
        }

        // 2) Fill purchase from form
        purchase.setStock_id(stocks.get(stockCombo.getValue()));
        purchase.setPrice(price);
        purchase.setQuantity(qty);
        purchase.setSupplier_id(suppliers.get(suppliersCombo.getValue()));
        purchase.setPurchase_date(Date.valueOf(operationDate.getValue()));

        // 3) Save / update
        if (this.update) {
            purchase.setId(this.Purchase_ID);

            if (purchase.update()) {
                clear();
                closePopUp(event);
                displayAlert(TITLE_SUCCESS, "Purchase Updated successfully", Alert.AlertType.INFORMATION);
            } else {
                displayAlert(TITLE_ERROR, "Error while updating!!!", Alert.AlertType.ERROR);
            }
            return;
        }

        if (purchase.save()) {
            closePopUp(event);
            displayAlert(TITLE_SUCCESS, "Purchase added successfully", Alert.AlertType.INFORMATION);
        } else {
            displayAlert(TITLE_ERROR, "Error while saving!!!", Alert.AlertType.ERROR);
        }
    }


    private void clear() {
        stockCombo.getSelectionModel().clearSelection();
        suppliersCombo.getSelectionModel().clearSelection();
        priceOfSale.clear();
        quantityInput.clear();
        operationDate.setValue(null);
        add_update.setDisable(false);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        try {
            this.setSupplierList();
            this.setProuctsList();
        } catch (SQLException e) {
            displayAlert(TITLE_ERROR,
                    "Error while loading data: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }

        validateDecimalInput(priceOfSale);
        validateDecimalInput(quantityInput);
    }

    public void setSupplierList() throws SQLException {
        ObservableList<String> supplierNames = FXCollections.observableArrayList();

        String query = "SELECT id, name from suppliers ";

        statement = DBConfig.getConnection().prepareStatement(query);
        resultSet = statement.executeQuery();
        while (resultSet.next()) {
            suppliers.put(resultSet.getString("name"), resultSet.getInt("id"));
            supplierNames.add(resultSet.getString("name"));
        }
        disconnect();
        suppliersCombo.setItems(supplierNames);
    }

    public void setProuctsList() throws SQLException {
        ObservableList<String> products = FXCollections.observableArrayList();

        String select_query = "SELECT id,name from stocks ";

        statement = DBConfig.getConnection().prepareStatement(select_query);
        resultSet = statement.executeQuery();
        while (resultSet.next()) {

            stocks.put(resultSet.getString("name"), resultSet.getInt("id"));
            products.add(resultSet.getString("name"));
        }
        disconnect();
        stockCombo.setItems(products);
    }

    private boolean update;

    public void setUpdate(boolean b) {
        this.update = b;
    }

    public void fetchPurchase(Purchase purchase) {
        Purchase_ID = purchase.getId();
        header.setText("Update Purchase");
        stockCombo.setValue(purchase.getProduct_name());
        suppliersCombo.setValue(purchase.getSupplier_name());
        priceOfSale.setText(String.valueOf(purchase.getPrice()));
        quantityInput.setText(String.valueOf(purchase.getQuantity()));
        operationDate.setValue(LocalDate.parse(purchase.getPurchase_date().toString()));
        key.setText("Update");
        add_update.setText("Update");
        add_update.setOnMouseClicked(event -> {
            purchase.setStock_id(stocks.get(stockCombo.getValue()));
            purchase.setSupplier_id(suppliers.get(suppliersCombo.getValue()));
            purchase.setPrice(Float.parseFloat(priceOfSale.getText()));
            purchase.setQuantity(Float.parseFloat(quantityInput.getText()));
            purchase.setPurchase_date(Date.valueOf(operationDate.getValue()));
            if (purchase.update()) {
                displayAlert(TITLE_SUCCESS, "Purchase Updated successfully", Alert.AlertType.INFORMATION);
                closePopUp(event);
            } else {
                displayAlert(TITLE_ERROR, "Error while updating!!!", Alert.AlertType.ERROR);
            }
        });
    }

    @FXML
    void openAddProduct(MouseEvent event) throws IOException {
        openNewWindow("Add Product", "add_new_product");
    }

    @FXML
    void refreshTable(MouseEvent event) throws SQLException {
        setProuctsList();
    }

    public Purchase getPurchase(int purchaseId) {
        Purchase purchase = new Purchase();

        String sql = "SELECT id, quantity, stock_id, supplier_id, price, purchase_date " +
                "FROM purchases WHERE id = ? LIMIT 1";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, purchaseId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    purchase.setId(rs.getInt("id"));
                    purchase.setQuantity(rs.getFloat("quantity"));
                    purchase.setStock_id(rs.getInt("stock_id"));
                    purchase.setSupplier_id(rs.getInt("supplier_id"));
                    purchase.setPrice(rs.getInt("price"));
                    purchase.setPurchase_date(rs.getDate("purchase_date"));
                }
            }

        } catch (SQLException e) {
            displayAlert(TITLE_ERROR, e.getMessage(), Alert.AlertType.ERROR);
        }

        return purchase;
    }
}