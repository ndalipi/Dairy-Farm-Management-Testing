package com.dfms.dairy_farm_management_system.controllers;

import com.dfms.dairy_farm_management_system.Main;
import com.dfms.dairy_farm_management_system.connection.DBConfig;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.AnimalDetailsController;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.NewAnimalController;
import com.dfms.dairy_farm_management_system.models.Animal;
import com.dfms.dairy_farm_management_system.models.Employee;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.dfms.dairy_farm_management_system.connection.DBConfig.getConnection;
import static com.dfms.dairy_farm_management_system.helpers.Helper.*;

public class ManageAnimalController implements Initializable {
    private static final String APP_ICON_PATH = "file:src/main/resources/images/logo.png";
    private static final String ICON_STYLE = "-fx-background-color: transparent;-fx-cursor: hand;-fx-size:15px;";
    private static final String ERROR_TITLE = "Error";

    @FXML
    private TableView<Animal> animals;

    @FXML
    private TableColumn<Animal, String> colid;

    @FXML
    private TableColumn<Animal, String> coltype;

    @FXML
    private TableColumn<Animal, String> colrace;

    @FXML
    private TableColumn<Animal, Date> colbirth;

    @FXML
    private TableColumn<Animal, String> colroutine;

    @FXML
    private TableColumn<Animal, String> colactions;

    @FXML
    private ComboBox<String> export_combo;

    @FXML
    private TextField textField_search;

    private Connection connection = getConnection();
    private PreparedStatement preparedStatement;
    private Statement statement;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            displayAnimals();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        ObservableList<String> list = FXCollections.observableArrayList("PDF", "Excel");
        export_combo.setItems(list);
        liveSearch();
        export_combo.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            if (t1.equals("PDF")) {
                exportToPDF();
            } else {
                exportToExcel();
            }
        });
    }

    public ObservableList<Animal> getAnimals() {
        ObservableList<Animal> listAnimal = FXCollections.observableArrayList();
        String select_query = "SELECT * from `animals`";

        try (Connection connection = DBConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(select_query);
             ResultSet resultSet = ps.executeQuery()) {

            while (resultSet.next()) {
                Animal animal = new Animal();
                animal.setId(resultSet.getString("id"));
                animal.setBirth_date(resultSet.getDate("birth_date"));
                animal.setPurchase_date(resultSet.getDate("purchase_date"));
                animal.setRoutineId(resultSet.getInt("routine"));
                animal.setRaceId(resultSet.getInt("race"));
                animal.setType(resultSet.getString("type"));
                animal.setCreated_at(resultSet.getTimestamp("created_at"));
                animal.setUpdated_at(resultSet.getTimestamp("updated_at"));
                listAnimal.add(animal);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return listAnimal;
    }

    public void refreshTableAnimal() {
        ObservableList<Animal> listAnimal = FXCollections.observableArrayList();
        listAnimal.clear();
        listAnimal = getAnimals();
        animals.setItems(listAnimal);
    }

    public void displayAnimals() throws SQLException, ClassNotFoundException {
        ObservableList<Animal> list = getAnimals();
        colid.setCellValueFactory(new PropertyValueFactory<Animal, String>("id"));
        coltype.setCellValueFactory(new PropertyValueFactory<Animal, String>("type"));
        colbirth.setCellValueFactory(new PropertyValueFactory<Animal, Date>("birth_date"));
        colrace.setCellValueFactory(new PropertyValueFactory<Animal, String>("raceName"));
        colroutine.setCellValueFactory(new PropertyValueFactory<Animal, String>("routineName"));

        Callback<TableColumn<Animal, String>, TableCell<Animal, String>> cellFoctory = (TableColumn<Animal, String> param) -> {
            final TableCell<Animal, String> cell = new TableCell<Animal, String>() {

                Image edit_img = new Image(getClass().getResourceAsStream("/images/edit.png"));
                Image delete_img = new Image(getClass().getResourceAsStream("/images/delete.png"));
                Image view_details_img = new Image(getClass().getResourceAsStream("/images/eye.png"));

                @Override
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }

                    ImageView iv_view_details = createIcon(view_details_img);
                    ImageView iv_edit = createIcon(edit_img);
                    ImageView iv_delete = createIcon(delete_img);

                    HBox managebtn = new HBox(iv_view_details, iv_edit, iv_delete);
                    managebtn.setStyle("-fx-alignment:center");
                    HBox.setMargin(iv_view_details, new Insets(1, 1, 0, 3));
                    HBox.setMargin(iv_delete, new Insets(1, 1, 0, 3));
                    HBox.setMargin(iv_edit, new Insets(1, 1, 0, 3));

                    setGraphic(managebtn);
                    setText(null);

                    attachAnimalHandlers(iv_view_details, iv_edit, iv_delete);
                }
            };
            return cell;
        };

        colactions.setCellFactory(cellFoctory);
        animals.setItems(list);
    }

    // ===== MOVED OUT of TableCell (same code, just correct location) =====

    public void liveSearch() {
        ObservableList<Animal> listAnimal = getAnimals();
        FilteredList<Animal> filteredData = new FilteredList<>(listAnimal, p -> true);
        textField_search.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(animal -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                if (animal.getType().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (animal.getRaceName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else return animal.getId().toLowerCase().contains(lowerCaseFilter);
            });
        });
        SortedList<Animal> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(animals.comparatorProperty());
        animals.setItems(sortedData);
    }

    void exportToPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();
                try {
                    document.add(new Paragraph("Animal List"));
                    document.add(new Paragraph(" "));
                } catch (Exception e) {
                    displayAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
                }
                PdfPTable table = new PdfPTable(6);
                table.addCell("Cow ID");
                table.addCell("Race");
                table.addCell("Birth Date");
                table.addCell("Type");
                table.addCell("Routine");
                table.addCell("Purchase Date");

                for (Animal animal : animals.getItems()) {
                    String id = animal.getId() != null ? animal.getId() : "-";
                    String race = animal.getRaceName() != null ? animal.getRaceName() : "-";
                    String birth_date = animal.getBirth_date() != null ? animal.getBirth_date().toString() : "-";
                    String type = animal.getType() != null ? animal.getType() : "-";
                    String routine = animal.getRoutineName() != null ? animal.getRoutineName() : "-";
                    String purchase_date = animal.getPurchase_date() != null ? animal.getPurchase_date().toString() : "-";

                    table.addCell(id);
                    table.addCell(race);
                    table.addCell(birth_date);
                    table.addCell(type);
                    table.addCell(routine);
                    table.addCell(purchase_date);
                }

                document.add(table);
                document.close();
                displayAlert("Success", "Animals exported successfully", Alert.AlertType.INFORMATION);

            } catch (Exception e) {
                displayAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    void exportToExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"),
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = fileChooser.showSaveDialog(null);
        if (file == null) return;

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fileOutputStream = new FileOutputStream(file)) {

            Sheet sheet = workbook.createSheet("Animals");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Cow ID");
            header.createCell(1).setCellValue("Race");
            header.createCell(2).setCellValue("Birth Date");
            header.createCell(3).setCellValue("Type");
            header.createCell(4).setCellValue("Routine");
            header.createCell(5).setCellValue("Purchase Date");

            ObservableList<Animal> list = animals.getItems();

            for (Animal animal : list) {
                String id = animal.getId() != null ? animal.getId() : "-";
                String race = animal.getRaceName() != null ? animal.getRaceName() : "-";
                String birth_date = animal.getBirth_date() != null ? animal.getBirth_date().toString() : "-";
                String type = animal.getType() != null ? animal.getType() : "-";
                String routine = animal.getRoutineName() != null ? animal.getRoutineName() : "-";
                String purchase_date = animal.getPurchase_date() != null ? animal.getPurchase_date().toString() : "-";

                Row row = sheet.createRow(list.indexOf(animal) + 1);
                row.createCell(0).setCellValue(id);
                row.createCell(1).setCellValue(race);
                row.createCell(2).setCellValue(birth_date);
                row.createCell(3).setCellValue(type);
                row.createCell(4).setCellValue(routine);
                row.createCell(5).setCellValue(purchase_date);
            }

            workbook.write(fileOutputStream);
            displayAlert("Success", "Animals exported successfully", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private ImageView createIcon(Image img) {
        ImageView iv = new ImageView();
        iv.setStyle(ICON_STYLE);
        iv.setImage(img);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setCache(true);
        return iv;
    }

    private void attachAnimalHandlers(ImageView iv_view_details, ImageView iv_edit, ImageView iv_delete) {

        iv_delete.setOnMouseClicked(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Confirmation");
            alert.setHeaderText("Are you sure you want to delete this Cow?");
            Animal animal = animals.getSelectionModel().getSelectedItem();

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    animal.delete();
                    displayAnimals();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Alert alertInfo = new Alert(Alert.AlertType.INFORMATION);
                alertInfo.setTitle("Delete Cow");
                alertInfo.setHeaderText("Cow deleted successfully");
                alertInfo.showAndWait();
            }
        });

        iv_view_details.setOnMouseClicked(event -> {
            Animal animal = animals.getSelectionModel().getSelectedItem();
            FXMLLoader fxmlLoader = new FXMLLoader(
                    Main.class.getResource("/com/dfms/dairy_farm_management_system/popups/animal_details.fxml")
            );

            try {
                Scene scene = new Scene(fxmlLoader.load());
                AnimalDetailsController controller = fxmlLoader.getController();
                controller.fetchAnimal(
                        animal.getId(),
                        animal.getRaceName(),
                        animal.getBirth_date(),
                        animal.getRoutineName(),
                        animal.getPurchase_date(),
                        animal.getType()
                );

                Stage stage = new Stage();
                stage.getIcons().add(new Image(APP_ICON_PATH));
                stage.setTitle("Animal Details");
                stage.setResizable(false);
                stage.setScene(scene);
                centerScreen(stage);
                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
                displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
            }
        });

        iv_edit.setOnMouseClicked(event -> {
            Animal animal = animals.getSelectionModel().getSelectedItem();
            FXMLLoader fxmlLoader = new FXMLLoader(
                    Main.class.getResource("/com/dfms/dairy_farm_management_system/popups/add_new_animal.fxml")
            );

            try {
                Scene scene = new Scene(fxmlLoader.load());
                NewAnimalController newAnimalController = fxmlLoader.getController();
                newAnimalController.setUpdate(true);
                newAnimalController.fetchAnimal(
                        animal.getId(),
                        animal.getRaceName(),
                        animal.getBirth_date().toLocalDate(),
                        animal.getRoutineName(),
                        animal.getPurchase_date().toLocalDate(),
                        animal.getType()
                );

                Stage stage = new Stage();
                stage.getIcons().add(new Image(APP_ICON_PATH));
                stage.setTitle("Update Animal");
                stage.setResizable(false);
                stage.setScene(scene);
                centerScreen(stage);
                stage.show();

            } catch (IOException e) {
                displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        });
    }

    public void refreshTable(MouseEvent mouseEvent) {
        refreshTableAnimal();
    }

    @FXML
    void openAddNewRace(MouseEvent event) throws IOException {
        openNewWindow("Add New Race", "add_new_race");
    }

    public void openAddNewAnimal(MouseEvent mouseEvent) throws IOException {
        openNewWindow("Add New Animal", "add_new_animal");
    }
}