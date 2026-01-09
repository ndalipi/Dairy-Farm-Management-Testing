package com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers;

import com.dfms.dairy_farm_management_system.connection.DBConfig;
import com.dfms.dairy_farm_management_system.models.MilkCollection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

import static com.dfms.dairy_farm_management_system.connection.DBConfig.getConnection;
import static com.dfms.dairy_farm_management_system.helpers.Helper.*;

public class NewMilkCollectionController implements Initializable {
    private static final String ERROR_TITLE = "Error";
    private static final String SUCCESS_TITLE = "success";

    private static final String SQL_SELECT_COW_IDS = "SELECT id FROM animals WHERE type = 'cow'";
    private static final String SQL_SELECT_MILK_COLLECTION_BY_ID =
            "SELECT id, quantity, period, cow_id FROM milk_collections WHERE id = ? LIMIT 1";

    @FXML
    private ComboBox<String> cowid;

    @FXML
    private TextField milkquantity_input;

    @FXML
    private ComboBox<String> period_input;

    @FXML
    private Label header;

    @FXML
    private Label key;

    @FXML
    private Button Add_Update;

    private int MilkCollection_ID;
    private boolean update;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            setCowComboItems();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        setPeriodComboItems();
        validateDecimalInput(milkquantity_input);
    }

    public void setPeriodComboItems() {
        period_input.setItems(FXCollections.observableArrayList("morning", "evening"));
    }

    public void setCowComboItems() throws SQLException {
        ObservableList<String> cows = FXCollections.observableArrayList();

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SELECT_COW_IDS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cows.add(rs.getString("id"));
            }
        }

        cowid.setItems(cows);
    }

    public void setUpdate(boolean b) {
        this.update = b;
    }

    @FXML
    public void addMilkCollection(MouseEvent mouseEvent) throws SQLException {
        if (period_input.getValue() == null || cowid.getValue() == null || milkquantity_input.getText().isEmpty()) {
            displayAlert(ERROR_TITLE, "Please Fill all field ", Alert.AlertType.ERROR);
            return;
        }

        float quantity;
        try {
            quantity = Float.parseFloat(milkquantity_input.getText());
        } catch (NumberFormatException e) {
            displayAlert(ERROR_TITLE, "Quantity must be a number ", Alert.AlertType.ERROR);
            return;
        }

        if (quantity == 0) {
            displayAlert(ERROR_TITLE, "Quantity can't be null ", Alert.AlertType.ERROR);
            return;
        }

        MilkCollection milkCollection = new MilkCollection();
        milkCollection.setCow_id(cowid.getValue());
        milkCollection.setPeriod(period_input.getValue());
        milkCollection.setQuantity(quantity);

        boolean success;
        if (this.update) {
            milkCollection.setId(this.MilkCollection_ID);
            success = milkCollection.update();
            if (success) {
                clear();
                closePopUp(mouseEvent);
                displayAlert(SUCCESS_TITLE, "Milk Collection Updated successfully", Alert.AlertType.INFORMATION);
            } else {
                displayAlert(ERROR_TITLE, "Error while saving!!!", Alert.AlertType.ERROR);
            }
        } else {
            success = milkCollection.save();
            if (success) {
                clear();
                closePopUp(mouseEvent);
                displayAlert(SUCCESS_TITLE, "Milk Collection Added successfully", Alert.AlertType.INFORMATION);
            } else {
                displayAlert(ERROR_TITLE, "Error while saving!!!", Alert.AlertType.ERROR);
            }
        }
    }

    public void fetchMilkCollection(MilkCollection milkCollection) {
        this.MilkCollection_ID = milkCollection.getId();
        header.setText("Update Milk Collection");
        this.cowid.setValue(milkCollection.getCow_id());
        this.period_input.setValue(milkCollection.getPeriod());
        this.milkquantity_input.setText(String.valueOf(milkCollection.getQuantity()));
        key.setText("Update");
        Add_Update.setText("Update");
        Add_Update.setOnMouseClicked(event -> {
            milkCollection.setCow_id(cowid.getValue());
            milkCollection.setPeriod(period_input.getValue());
            milkCollection.setQuantity(Float.parseFloat(milkquantity_input.getText()));
            if (milkCollection.update()) {
                displayAlert(SUCCESS_TITLE, "Milk Collection Updated successfully", Alert.AlertType.INFORMATION);
                closePopUp(event);
            } else {
                displayAlert(ERROR_TITLE, "Error while updating!!!", Alert.AlertType.ERROR);
            }
        });
    }

    private void clear() {
        cowid.getSelectionModel().clearSelection();
        period_input.getSelectionModel().clearSelection();
        milkquantity_input.clear();
        Add_Update.setDisable(false);
    }

    public MilkCollection getCollection(int milkCollection_ID) {
        MilkCollection milkCollection = new MilkCollection();

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SELECT_MILK_COLLECTION_BY_ID)) {

            ps.setInt(1, milkCollection_ID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    milkCollection.setId(rs.getInt("id"));
                    milkCollection.setQuantity(rs.getFloat("quantity"));
                    milkCollection.setPeriod(rs.getString("period"));
                    milkCollection.setCow_id(rs.getString("cow_id"));
                }
            }
        } catch (SQLException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }

        return milkCollection;
    }
}
