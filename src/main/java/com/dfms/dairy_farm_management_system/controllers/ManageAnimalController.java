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
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
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
    private static final String ERROR_TITLE = "Error";

    private static final String PROP_ID = "id";
    private static final String PROP_TYPE = "type";
    private static final String PROP_BIRTH = "birth_date";
    private static final String PROP_RACE = "raceName";
    private static final String PROP_ROUTINE = "routineName";

    private static final String OPT_PDF = "PDF";
    private static final String OPT_EXCEL = "Excel";

    private static final String[] COL_TITLES = {"Cow ID", "Race", "Birth Date", "Type", "Routine", "Purchase Date"};

    @FXML private TableView<Animal> animals;
    @FXML private TableColumn<Animal, String> colid;
    @FXML private TableColumn<Animal, String> coltype;
    @FXML private TableColumn<Animal, String> colrace;
    @FXML private TableColumn<Animal, Date> colbirth;
    @FXML private TableColumn<Animal, String> colroutine;
    @FXML private TableColumn<Animal, String> colactions;
    @FXML private ComboBox<String> export_combo;
    @FXML private TextField textField_search;

    private final ObservableList<Animal> masterAnimals = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        export_combo.setItems(FXCollections.observableArrayList(OPT_PDF, OPT_EXCEL));
        export_combo.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b == null) return;
            if (OPT_PDF.equals(b)) exportToPDF();
            else exportToExcel();
        });

        bindColumns();
        colactions.setCellFactory(actionCellFactory());

        setupFiltering();
        refreshTableAnimal();
    }

    private void bindColumns() {
        colid.setCellValueFactory(new PropertyValueFactory<>(PROP_ID));
        coltype.setCellValueFactory(new PropertyValueFactory<>(PROP_TYPE));
        colbirth.setCellValueFactory(new PropertyValueFactory<>(PROP_BIRTH));
        colrace.setCellValueFactory(new PropertyValueFactory<>(PROP_RACE));
        colroutine.setCellValueFactory(new PropertyValueFactory<>(PROP_ROUTINE));
    }

    private void setupFiltering() {
        FilteredList<Animal> filtered = new FilteredList<>(masterAnimals, x -> true);

        textField_search.textProperty().addListener((obs, oldV, newV) -> {
            String key = normalize(newV);
            filtered.setPredicate(a -> {
                if (key.isEmpty()) return true;
                return normalize(a.getType()).contains(key)
                        || normalize(a.getRaceName()).contains(key)
                        || normalize(a.getId()).contains(key);
            });
        });

        SortedList<Animal> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(animals.comparatorProperty());
        animals.setItems(sorted);
    }

    private String normalize(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    public ObservableList<Animal> getAnimals() {
        ObservableList<Animal> out = FXCollections.observableArrayList();
        String q = "SELECT * FROM `animals`";

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

    public void refreshTableAnimal() {
        masterAnimals.setAll(getAnimals());
    }

    @FXML
    public void refreshTable(javafx.scene.input.MouseEvent mouseEvent) {
        refreshTableAnimal();
    }

    private Callback<TableColumn<Animal, String>, TableCell<Animal, String>> actionCellFactory() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                MenuItem view = new MenuItem("View details");
                MenuItem edit = new MenuItem("Edit");
                MenuItem del = new MenuItem("Delete");

                MenuButton btn = new MenuButton("Actions", null, view, edit, del);

                view.setOnAction(e -> openDetails(getRowAnimal()));
                edit.setOnAction(e -> openEdit(getRowAnimal()));
                del.setOnAction(e -> deleteAnimal(getRowAnimal()));

                setGraphic(btn);
                setText(null);
            }

            private Animal getRowAnimal() {
                int i = getIndex();
                if (i < 0 || i >= getTableView().getItems().size()) return null;
                return getTableView().getItems().get(i);
            }
        };
    }

    private void deleteAnimal(Animal a) {
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

    private void openDetails(Animal a) {
        if (a == null) return;

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dfms/dairy_farm_management_system/popups/animal_details.fxml"));
        try {
            Scene scene = new Scene(loader.load());
            AnimalDetailsController c = loader.getController();
            c.fetchAnimal(a.getId(), a.getRaceName(), a.getBirth_date(), a.getRoutineName(), a.getPurchase_date(), a.getType());

            Stage stage = new Stage();
            stage.getIcons().add(new javafx.scene.image.Image(APP_ICON_PATH));
            stage.setTitle("Animal Details");
            stage.setResizable(false);
            stage.setScene(scene);
            centerScreen(stage);
            stage.show();
        } catch (IOException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void openEdit(Animal a) {
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
            stage.getIcons().add(new javafx.scene.image.Image(APP_ICON_PATH));
            stage.setTitle("Update Animal");
            stage.setResizable(false);
            stage.setScene(scene);
            centerScreen(stage);
            stage.show();
        } catch (IOException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
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

            PdfPTable table = new PdfPTable(COL_TITLES.length);
            writeHeadersPdf(table);

            for (Animal a : animals.getItems()) {
                String[] row = toRow(a);
                for (String v : row) table.addCell(v);
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
            writeHeadersExcel(sheet);

            int rowIndex = 1;
            for (Animal a : animals.getItems()) {
                Row r = sheet.createRow(rowIndex++);
                String[] data = toRow(a);
                for (int i = 0; i < data.length; i++) r.createCell(i).setCellValue(data[i]);
            }

            wb.write(out);
            displayAlert("Success", "Animals exported successfully", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void writeHeadersPdf(PdfPTable table) {
        for (String h : COL_TITLES) table.addCell(h);
    }

    private void writeHeadersExcel(Sheet sheet) {
        Row head = sheet.createRow(0);
        for (int i = 0; i < COL_TITLES.length; i++) head.createCell(i).setCellValue(COL_TITLES[i]);
    }

    private String[] toRow(Animal a) {
        return new String[]{
                dash(a == null ? null : a.getId()),
                dash(a == null ? null : a.getRaceName()),
                dash(a == null || a.getBirth_date() == null ? null : a.getBirth_date().toString()),
                dash(a == null ? null : a.getType()),
                dash(a == null ? null : a.getRoutineName()),
                dash(a == null || a.getPurchase_date() == null ? null : a.getPurchase_date().toString())
        };
    }

    private String dash(String v) {
        return v == null ? "-" : v;
    }
}
