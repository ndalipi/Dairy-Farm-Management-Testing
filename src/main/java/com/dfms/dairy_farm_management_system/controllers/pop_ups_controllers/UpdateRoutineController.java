package com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers;

import com.dfms.dairy_farm_management_system.models.Routine;
import com.dfms.dairy_farm_management_system.models.RoutineDetails;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.dfms.dairy_farm_management_system.connection.DBConfig.getConnection;
import static com.dfms.dairy_farm_management_system.helpers.Helper.closePopUp;
import static com.dfms.dairy_farm_management_system.helpers.Helper.displayAlert;

public class UpdateRoutineController {
    private static final String STYLE_LABEL_BOLD_14 = "-fx-font-size: 14px; -fx-font-weight: bold;";
    private static final String STYLE_INPUT = "input";
    private static final ObservableList<String> PERIODS = FXCollections.observableArrayList("Morning", "Evening");

    @FXML
    TextField routineName;
    @FXML
    TextArea routineNotes;
    @FXML
    VBox foodList;
    @FXML
    Button routineBtn;

    public void initData(Routine routine) {
        this.routineName.setText(routine.getName());
        this.routineNotes.setText(routine.getNote());
        setFoods(routine.getDetails());

        routineBtn.setText("UPDATE");
        routineBtn.setOnMouseClicked((MouseEvent mouseEvent) -> {
            routine.setName(routineName.getText());
            routine.setNote(routineNotes.getText());

            if (!routine.update()) {
                displayAlert("ERROR", "Some error happened while updating!", Alert.AlertType.ERROR);
                return;
            }

            deleteDetails(routine.getDetails());

            ArrayList<RoutineDetails> routineDetails = new ArrayList<>();

            for (Node box : foodList.getChildren()) {
                CheckBox checkBox = (CheckBox) ((VBox) ((HBox) box).getChildren().get(0)).getChildren().get(1);
                if (!checkBox.isSelected()) {
                    continue;
                }

                String foodName = checkBox.getText();
                String foodQuantity = ((TextField) (((VBox) ((HBox) box).getChildren().get(1)).getChildren().get(1))).getText();
                String foodPeriod = ((ComboBox<String>) (((VBox) ((HBox) box).getChildren().get(2)).getChildren().get(1))).getValue();

                RoutineDetails routineDetails1 = new RoutineDetails();
                routineDetails1.setRoutine_id(routine.getId());
                routineDetails1.setQuantity(Float.parseFloat(foodQuantity));
                routineDetails1.setFeeding_time(foodPeriod);

                if (!routineDetails1.save()) {
                    revertChanges(routine, routineDetails);
                    displayAlert("ERROR", "Some error happened while updating!", Alert.AlertType.ERROR);
                    return;
                }

                routineDetails1.setId(RoutineDetails.getLastId());
                routineDetails.add(routineDetails1);
            }

            closePopUp(mouseEvent);
            displayAlert("SUCCESS", "Routine updated successfully", Alert.AlertType.INFORMATION);
        });
    }

    public void setFoods(List<RoutineDetails> details) {
        this.foodList.getChildren().clear();

        List<String> foods = getFoods();

        if (details == null || details.isEmpty()) {
            for (String foodName : foods) {
                addItem(foodName);
            }
            return;
        }

        System.out.println("Details != null");
        System.out.println("Details != empty");

        Map<String, RoutineDetails> detailsHashMap = new HashMap<>();
        Set<String> routinesFeedsNames = new HashSet<>();

        for (RoutineDetails routineDetails : details) {
            String name = routineDetails.getStock_name();
            detailsHashMap.put(name, routineDetails);
            routinesFeedsNames.add(name);
        }

        System.out.println(detailsHashMap);
        System.out.println(routinesFeedsNames);
        System.out.println(foods);

        for (String foodName : foods) {
            if (routinesFeedsNames.contains(foodName)) {
                System.out.println("routine feeds contains " + foodName);
                addSelectedItem(foodName, detailsHashMap.get(foodName));
            } else {
                addItem(foodName);
            }
        }
    }

    public void addItem(String food) {
        this.foodList.getChildren().add(createFoodRow(food, false, null, null));
    }

    public void addSelectedItem(String food, RoutineDetails detail) {
        HBox row = createFoodRow(
                food,
                true,
                detail == null ? null : String.valueOf(detail.getQuantity()),
                detail == null ? null : detail.getFeeding_time()
        );
        this.foodList.getChildren().add(row);
    }

    private HBox createFoodRow(String food, boolean selected, String quantityText, String feedingTimeValue) {
        HBox hBox = new HBox();
        hBox.setSpacing(60);

        VBox foodType = new VBox();
        Label label = new Label("Food type");
        label.setStyle(STYLE_LABEL_BOLD_14);
        CheckBox checkBox = new CheckBox(food);
        checkBox.setSelected(selected);
        if (selected) {
            System.out.println(food + " checkbox is selected " + checkBox.isSelected());
        }
        checkBox.getStyleClass().add("main_content");
        VBox.setMargin(checkBox, new Insets(10, 0, 0, 0));
        foodType.getChildren().add(label);
        foodType.getChildren().add(checkBox);
        hBox.getChildren().add(foodType);

        VBox foodQuantity = new VBox();
        Label label1 = new Label("Food Quantity");
        label1.setStyle(STYLE_LABEL_BOLD_14);
        TextField quantity = new TextField();
        if (quantityText != null) {
            quantity.setText(quantityText);
        }
        quantity.setPromptText("Quantity");
        quantity.getStyleClass().add(STYLE_INPUT);
        quantity.getStyleClass().add("quantity_input");
        VBox.setMargin(quantity, new Insets(10, 0, 0, 0));
        foodQuantity.getChildren().add(label1);
        foodQuantity.getChildren().add(quantity);
        hBox.getChildren().add(foodQuantity);

        VBox feedingTime = new VBox();
        Label label2 = new Label("Feeding Time");
        label2.setStyle(STYLE_LABEL_BOLD_14);
        ComboBox<String> period = new ComboBox<String>(PERIODS);
        if (feedingTimeValue != null) {
            period.setValue(feedingTimeValue);
        }
        period.setPromptText("Period");
        period.getStyleClass().add(STYLE_INPUT);
        period.getStyleClass().add("clock_input");
        VBox.setMargin(period, new Insets(10, 0, 0, 8));
        feedingTime.getChildren().add(label2);
        feedingTime.getChildren().add(period);
        hBox.getChildren().add(feedingTime);

        return hBox;
    }

    public ArrayList<String> getFoods() {
        ArrayList<String> list = new ArrayList<>();
        String query = "SELECT * FROM stocks WHERE type = 'feed'";
        Connection connection = getConnection();
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                list.add(resultSet.getString("name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void revertChanges(Routine routine, ArrayList<RoutineDetails> details) {
        for (RoutineDetails detail : details) {
            detail.delete();
            details.remove(detail);
        }
        routine.delete();
    }

    public void deleteDetails(ArrayList<RoutineDetails> details) {
        for (RoutineDetails detail : details) {
            detail.delete();
            details.remove(detail);
        }
    }
}