package com.dfms.dairy_farm_management_system.controllers;

import com.dfms.dairy_farm_management_system.Main;
import com.dfms.dairy_farm_management_system.connection.DBConfig;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.AnimalDetailsController;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.NewAnimalController;
import com.dfms.dairy_farm_management_system.models.Animal;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.dfms.dairy_farm_management_system.helpers.Helper.centerScreen;
import static com.dfms.dairy_farm_management_system.helpers.Helper.displayAlert;
import static com.dfms.dairy_farm_management_system.helpers.Helper.openNewWindow;

public class ManageAnimalController implements Initializable {

    private static final String APP_ICON_PATH = "file:src/main/resources/images/logo.png";
    private static final String ICON_STYLE = "-fx-background-color: transparent;-fx-cursor: hand;-fx-size:15px;";
    private static final String ERROR_TITLE = "Error";

    private static final String PROP_ID = "id";
    private static final String PROP_TYPE = "type";
    private static final String PROP_BIRTH = "birth_date";
    private static final String PROP_RACE = "raceName";
    private static final String PROP_ROUTINE = "routineName";

    private static final String[] EXPORT_OPTIONS = {"PDF", "Excel"};
    private static final String[] HEADERS = {"Cow ID", "Race", "Birth Date", "Type", "Routine", "Purchase Date"};

    private static final Image EDIT_IMG = new Image(ManageAnimalController.class.getResourceAsStream("/images/edit.png"));
    private static final Image DELETE_IMG = new Image(ManageAnimalController.class.getResourceAsStream("/images/delete.png"));
    private static final Image VIEW_IMG = new Image(ManageAnimalController.class.getResourceAsStream("/images/eye.png"));

    @FXML private TableView<Animal> animals;
    @FXML private TableColumn<Animal, String> colid;
    @FXML private TableColumn<Animal, String> coltype;
    @FXML private TableColumn<Animal, String> colrace;
    @FXML private TableColumn<Animal, Date> colbirth;
    @FXML private TableColumn<Animal, String> colroutine;
    @FXML private TableColumn<Animal, String> colactions;
    @FXML private ComboBox<String> export_combo;
    @FXML private TextField textField_search;

    private final Connection connection = DBConfig.getConnection();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        export_combo.setItems(FXCollections.observableArrayList(EXPORT_OPTIONS));
        export_combo.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b == null) return;
            if ("PDF".equals(b)) exportToPDF();
            else exportToExcel();
        });

        bindColumns();
        colactions.setCellFactory(actionsFactory());

        ObservableList<Animal> base = getAnimals();
        setupSearch(base);
        animals.setItems(base);
    }

    private void bindColumns() {
        colid.setCellValueFactory(new PropertyValueFactory<>(PROP_ID));
        coltype.setCellValueFactory(new PropertyValueFactory<>(PROP_TYPE));
        colbirth.setCellValueFactory(new PropertyValueFactory<>(PROP_BIRTH));
        colrace.setCellValueFactory(new PropertyValueFactory<>(PROP_RACE));
        colroutine.setCellValueFactory(new PropertyValueFactory<>(PROP_ROUTINE));
    }

    public ObservableList<Animal> getAnimals() {
        ObservableList<Animal> out = FXCollections.observableArrayList();
        String q = "SELECT * from `animals`";

        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(q);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Animal a = new Animal();
                a.setId(rs.getString("id"));
                a.setBirth_date(rs.getDate("birth_date"));
                a.setPurchase_date(rs.getDate("purchase_date"));
                a.setRoutineId(rs.getInt("routine"));
                a.setRaceId(rs.getInt("race"));
                a.setType(rs.getString("type"));
                a.setCreated_at(rs.getTimestamp("created_at"));
                a.setUpdated_at(rs.getTimestamp("updated_at"));
                out.add(a);
            }

        } catch (SQLException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
        return out;
    }

    private void setupSearch(ObservableList<Animal> base) {
        FilteredList<Animal> filtered = new FilteredList<>(base, x -> true);

        textField_search.textProperty().addListener((obs, oldV, newV) -> {
            String key = (newV == null) ? "" : newV.toLowerCase();

            filtered.setPredicate(a -> {
                if (key.isEmpty()) return true;
                return low(a.getType()).contains(key)
                        || low(a.getRaceName()).contains(key)
                        || low(a.getId()).contains(key);
            });
        });

        SortedList<Animal> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(animals.comparatorProperty());
        animals.setItems(sorted);
    }

    private String low(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    private Callback<TableColumn<Animal, String>, TableCell<Animal, String>> actionsFactory() {
        return col -> new TableCell<>() {

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Button actions = new Button("Actions");
                actions.setStyle(ICON_STYLE);

                ContextMenu menu = new ContextMenu();
                MenuItem view = new MenuItem("View details");
                MenuItem edit = new MenuItem("Edit");
                MenuItem del = new MenuItem("Delete");

                view.setOnAction(e -> onView(current()));
                edit.setOnAction(e -> onEdit(current()));
                del.setOnAction(e -> onDelete(current()));

                menu.getItems().addAll(view, edit, del);
                actions.setOnMouseClicked(e -> menu.show(actions, e.getScreenX(), e.getScreenY()));

                HBox box = new HBox(actions);
                box.setStyle("-fx-alignment:center");
                HBox.setMargin(actions, new Insets(1, 1, 0, 3));

                setGraphic(box);
                setText(null);
            }

            private Animal current() {
                int i = getIndex();
                if (i < 0 || i >= getTableView().getItems().size()) return null;
                return getTableView().getItems().get(i);
            }
        };
    }

    private void onDelete(Animal a) {
        if (a == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Confirmation");
        alert.setHeaderText("Are you sure you want to delete this Cow?");
        Optional<ButtonType> res = alert.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        try {
            a.delete();
            refreshTableAnimal();
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Delete Cow");
            ok.setHeaderText("Cow deleted successfully");
            ok.showAndWait();
        } catch (Exception e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void onView(Animal a) {
        if (a == null) return;

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dfms/dairy_farm_management_system/popups/animal_details.fxml"));
        try {
            Scene scene = new Scene(loader.load());
            AnimalDetailsController c = loader.getController();
            c.fetchAnimal(a.getId(), a.getRaceName(), a.getBirth_date(), a.getRoutineName(), a.getPurchase_date(), a.getType());

            Stage stage = new Stage();
            stage.getIcons().add(new Image(APP_ICON_PATH));
            stage.setTitle("Animal Details");
            stage.setResizable(false);
            stage.setScene(scene);
            centerScreen(stage);
            stage.show();
        } catch (IOException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void onEdit(Animal a) {
        if (a == null) return;

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dfms/dairy_farm_management_system/popups/add_new_animal.fxml"));
        try {
            Scene scene = new Scene(loader.load());
            NewAnimalController c = loader.getController();
            c.setUpdate(true);
            c.fetchAnimal(
                    a.getId(),
                    a.getRaceName(),
                    a.getBirth_date().toLocalDate(),
                    a.getRoutineName(),
                    a.getPurchase_date().toLocalDate(),
                    a.getType()
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
        }
    }

    public void refreshTableAnimal() {
        animals.setItems(getAnimals());
    }

    @FXML
    public void refreshTable(MouseEvent mouseEvent) {
        refreshTableAnimal();
    }

    @FXML
    void openAddNewRace(MouseEvent event) throws IOException {
        openNewWindow("Add New Race", "add_new_race");
    }

    @FXML
    public void openAddNewAnimal(MouseEvent mouseEvent) throws IOException {
        openNewWindow("Add New Animal", "add_new_animal");
    }

    void exportToPDF() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save As");
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = chooser.showSaveDialog(null);
        if (file == null) return;

        try {
            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            doc.add(new Paragraph("Animal List"));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(HEADERS.length);
            for (String h : HEADERS) table.addCell(h);

            for (Animal a : animals.getItems()) {
                table.addCell(v(a.getId()));
                table.addCell(v(a.getRaceName()));
                table.addCell(d(a.getBirth_date()));
                table.addCell(v(a.getType()));
                table.addCell(v(a.getRoutineName()));
                table.addCell(d(a.getPurchase_date()));
            }

            doc.add(table);
            doc.close();

            displayAlert("Success", "Animals exported successfully", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    void exportToExcel() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save As");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"),
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = chooser.showSaveDialog(null);
        if (file == null) return;

        try (Workbook wb = new XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(file)) {

            Sheet sheet = wb.createSheet("Animals");
            Row head = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) head.createCell(i).setCellValue(HEADERS[i]);

            int r = 1;
            for (Animal a : animals.getItems()) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(v(a.getId()));
                row.createCell(1).setCellValue(v(a.getRaceName()));
                row.createCell(2).setCellValue(d(a.getBirth_date()));
                row.createCell(3).setCellValue(v(a.getType()));
                row.createCell(4).setCellValue(v(a.getRoutineName()));
                row.createCell(5).setCellValue(d(a.getPurchase_date()));
            }

            wb.write(out);
            displayAlert("Success", "Animals exported successfully", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private String v(String s) {
        return s == null ? "-" : s;
    }

    private String d(Date d) {
        return d == null ? "-" : d.toString();
    }
}
