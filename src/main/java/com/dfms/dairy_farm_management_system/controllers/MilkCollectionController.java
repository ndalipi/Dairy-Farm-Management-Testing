package com.dfms.dairy_farm_management_system.controllers;

import com.dfms.dairy_farm_management_system.Main;
import com.dfms.dairy_farm_management_system.connection.DBConfig;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.*;
import com.dfms.dairy_farm_management_system.models.MilkCollection;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.log4j.BasicConfigurator;
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

public class MilkCollectionController implements Initializable {
    private static final String ERROR_TITLE = "Error";
    private static final String ICON_STYLE = "-fx-background-color: transparent;-fx-cursor: hand;-fx-size:15px;";
    private static final String COL_COW_ID = "cow_id";
    private static final String COL_QUANTITY = "quantity";
    private static final String COL_PERIOD = "period";
    private static final String COL_CREATED_AT = "created_at";

    private static final String SELECT_QUERY =
            "SELECT mc.id, mc.cow_id, mc.quantity, mc.period, mc.created_at " +
                    "FROM milk_collections mc, animals a " +
                    "WHERE mc.cow_id = a.id AND a.type='cow'";

    private static final String EXPORT_QUERY =
            "SELECT id, cow_id, quantity, period, created_at FROM milk_collections";

    MilkCollection mc;
    @FXML
    private Button refresh_table_btn;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        BasicConfigurator.configure();

        ObservableList<String> list = FXCollections.observableArrayList("PDF", "Excel");
        combo.setItems(list);
        combo.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            if (t1.equals("PDF")) {
                exportToPDF();
            } else {
                exportToExcel();
            }
        });
        try {
            afficher();
            liveSearch(search_input, MilkCollectionTable);
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private ComboBox<String> combo;
    @FXML
    private TableView<MilkCollection> MilkCollectionTable;

    @FXML
    private TableColumn<MilkCollection, String> actions_col;
    @FXML
    private TableColumn<MilkCollection, Date> date_col;

    @FXML
    private TableColumn<MilkCollection, String> id_col;

    @FXML
    private TableColumn<MilkCollection, Float> milk_col;

    @FXML
    private Button openAddNewMilkCollectionBtn;

    @FXML
    private TableColumn<MilkCollection, String> period_col;

    @FXML
    private Button search_button;

    @FXML
    private TextField search_input;

    ObservableList<MilkCollection> list = FXCollections.observableArrayList();

    public ObservableList<MilkCollection> getMilkCollection() throws SQLException, ClassNotFoundException {
        ObservableList<MilkCollection> list = FXCollections.observableArrayList();

        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_QUERY);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                MilkCollection milkCollection = new MilkCollection();
                milkCollection.setId(rs.getInt("id"));
                milkCollection.setCow_id(rs.getString(COL_COW_ID));
                milkCollection.setQuantity(rs.getFloat(COL_QUANTITY));
                milkCollection.setPeriod(rs.getString(COL_PERIOD));
                milkCollection.setCreated_at(rs.getTimestamp(COL_CREATED_AT));
                list.add(milkCollection);
            }
        }

        return list;
    }

    public void refreshTableMilkCollection() throws SQLException {

        MilkCollectionTable.getItems().clear();

        try {
            afficher();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    public void afficher() throws SQLException, ClassNotFoundException {
        ObservableList<MilkCollection> list = getMilkCollection();

        id_col.setCellValueFactory(new PropertyValueFactory<>(COL_COW_ID));
        milk_col.setCellValueFactory(new PropertyValueFactory<>(COL_QUANTITY));
        period_col.setCellValueFactory(new PropertyValueFactory<>(COL_PERIOD));
        date_col.setCellValueFactory(new PropertyValueFactory<>(COL_CREATED_AT));

        actions_col.setCellFactory(col -> new ActionCell());
        MilkCollectionTable.setItems(list);
    }

    private class ActionCell extends TableCell<MilkCollection, String> {

        private final Image editImg = new Image(getClass().getResourceAsStream("/images/edit.png"));
        private final Image deleteImg = new Image(getClass().getResourceAsStream("/images/delete.png"));
        private final Image viewImg = new Image(getClass().getResourceAsStream("/images/eye.png"));

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setGraphic(null);
                setText(null);
                return;
            }

            ImageView ivView = new ImageView(viewImg);
            ivView.setStyle(ICON_STYLE);
            ivView.setPreserveRatio(true);
            ivView.setSmooth(true);
            ivView.setCache(true);

            ImageView ivEdit = new ImageView(editImg);
            ivEdit.setStyle(ICON_STYLE);
            ivEdit.setPreserveRatio(true);
            ivEdit.setSmooth(true);
            ivEdit.setCache(true);

            ImageView ivDelete = new ImageView(deleteImg);
            ivDelete.setStyle(ICON_STYLE);
            ivDelete.setPreserveRatio(true);
            ivDelete.setSmooth(true);
            ivDelete.setCache(true);

            HBox managebtn = new HBox(ivView, ivEdit, ivDelete);
            managebtn.setStyle("-fx-alignment:center");
            HBox.setMargin(ivView, new Insets(1, 1, 0, 3));
            HBox.setMargin(ivEdit, new Insets(1, 1, 0, 3));
            HBox.setMargin(ivDelete, new Insets(1, 1, 0, 3));

            setGraphic(managebtn);
            setText(null);

            ivDelete.setOnMouseClicked(event -> {
                MilkCollection mc = MilkCollectionTable.getSelectionModel().getSelectedItem();
                if (mc == null) return;

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Delete Confirmation");
                alert.setHeaderText("Are you sure you want to delete this cow sale?");
                Optional<ButtonType> result = alert.showAndWait();

                if (result.isPresent() && result.get() == ButtonType.OK) {
                    try {
                        if (mc.delete()) {
                            displayAlert("success", "Milk Sale deleted successfully", Alert.AlertType.INFORMATION);
                            refreshTableMilkCollection();
                        } else {
                            displayAlert(ERROR_TITLE, "Error while deleting!!!", Alert.AlertType.ERROR);
                        }
                    } catch (Exception e) {
                        displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
                    }
                }
            });

            ivEdit.setOnMouseClicked(event -> {
                MilkCollection milkcollection = MilkCollectionTable.getSelectionModel().getSelectedItem();
                if (milkcollection == null) return;

                FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource(
                        "/com/dfms/dairy_farm_management_system/popups/add_new_milk_collection.fxml"
                ));

                Scene scene;
                try {
                    scene = new Scene(fxmlLoader.load());
                } catch (IOException e) {
                    displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
                    return;
                }

                NewMilkCollectionController newMilkCollectionController = fxmlLoader.getController();
                newMilkCollectionController.setUpdate(true);
                newMilkCollectionController.fetchMilkCollection(milkcollection);

                Stage stage = new Stage();
                stage.getIcons().add(new Image("file:src/main/resources/images/logo.png"));
                stage.setTitle("Update MilkCollection");
                stage.setResizable(false);
                stage.setScene(scene);
                centerScreen(stage);
                stage.show();
            });

            ivView.setOnMouseClicked(event -> {
                MilkCollection mc = MilkCollectionTable.getSelectionModel().getSelectedItem();
                if (mc == null) return;

                FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource(
                        "/com/dfms/dairy_farm_management_system/popups/milkcollection_details.fxml"
                ));

                Scene scene;
                try {
                    scene = new Scene(fxmlLoader.load());
                    MilkCollectionlDetailsController controller = fxmlLoader.getController();
                    controller.fetchMilkCollection(mc.getId(), mc.getCow_id(), mc.getPeriod(), mc.getQuantity(), mc.getCreated_at());
                } catch (IOException e) {
                    displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
                    return;
                }

                Stage stage = new Stage();
                stage.getIcons().add(new Image("file:src/main/resources/images/logo.png"));
                stage.setTitle("Milk Collection  Details");
                stage.setResizable(false);
                stage.setScene(scene);
                centerScreen(stage);
                stage.show();
            });
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
        if (file == null) {
            return;
        }

        String[] headers = {
                "Milk Collection ID",
                "Cow ID",
                "Milk Quantity",
                "Collection Period",
                "Collection Date"
        };

        String[] cols = {
                "id",
                COL_COW_ID,
                COL_QUANTITY,
                COL_PERIOD,
                COL_CREATED_AT
        };

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fileOutputStream = new FileOutputStream(file);
             Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(EXPORT_QUERY);
             ResultSet rs = ps.executeQuery()) {

            Sheet sheet = workbook.createSheet("Milk Collection");

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            int rowNum = 1; // start after header
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < cols.length; i++) {
                    row.createCell(i).setCellValue(rs.getString(cols[i]));
                }
            }

            workbook.write(fileOutputStream);
            displayAlert("Success", "Milk Collection exported successfully", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private static int COLUMNS_COUNT = 4;

    void exportToPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(EXPORT_QUERY);
                 ResultSet rs = ps.executeQuery()) {

                Document document = new Document();
                // change document orientation to landscape
                document.setPageSize(PageSize.A4.rotate());

                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                Paragraph title = new Paragraph(
                        "Milk Collections List",
                        FontFactory.getFont(FontFactory.COURIER_BOLD, 20, BaseColor.BLACK)
                );
                Paragraph text = new Paragraph(
                        "This is the list of Milk Collections",
                        FontFactory.getFont(FontFactory.COURIER, 14, BaseColor.BLACK)
                );

                // center paragraph
                title.setAlignment(Element.ALIGN_CENTER);
                text.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(30);
                text.setSpacingAfter(30);

                document.add(title);
                document.add(text);

                PdfPTable table = new PdfPTable(COLUMNS_COUNT);

                // change pdf orientation to landscape
                table.setWidthPercentage(100);
                table.setSpacingBefore(11f);
                table.setSpacingAfter(11f);

                float[] colWidth = new float[COLUMNS_COUNT];
                for (int i = 0; i < COLUMNS_COUNT; i++) {
                    colWidth[i] = 2f;
                }
                table.setWidths(colWidth);

                // add table header
                String[] headers = {"Cow ID", "Quantity", "Period", "Collection date"};
                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Paragraph(h,
                            FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)));
                    cell.setPadding(5);
                    table.addCell(cell);
                }

                // add padding to cells
                table.getDefaultCell().setPadding(3);
                table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
                table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);

                String[] cols = {COL_COW_ID, COL_QUANTITY, COL_PERIOD, COL_CREATED_AT};
                while (rs.next()) {
                    for (String c : cols) {
                        PdfPCell cell = new PdfPCell(new Paragraph(String.valueOf(rs.getString(c))));
                        cell.setPadding(5);
                        table.addCell(cell);
                    }
                }

                document.add(table);
                document.close();

                displayAlert("Success", "Milk Collections exported successfully", Alert.AlertType.INFORMATION);

            } catch (Exception e) {
                displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    public void liveSearch(TextField search_input, TableView table) {
        search_input.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                try {
                    refreshTableMilkCollection();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            } else {
                ObservableList<MilkCollection> filteredList = FXCollections.observableArrayList();
                ObservableList<MilkCollection> milkCollections = null;
                try {
                    milkCollections = getMilkCollection();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                for (MilkCollection milkCollection : milkCollections) {
                    if (milkCollection.getPeriod().toLowerCase().contains(newValue.toLowerCase()) || milkCollection.getCow_id().toLowerCase().contains(newValue.toLowerCase())) {
                        filteredList.add(milkCollection);
                    }
                }
                MilkCollectionTable.setItems(filteredList);
            }
        });
    }

    @FXML
    void openAddNewMilkCollection(MouseEvent event) throws IOException {
        openNewWindow("Add Milk Collection", "add_new_milk_collection");
    }

    private Connection con = DBConfig.getConnection();
    private Statement stt;

    @FXML
    void refreshTable(MouseEvent event) throws SQLException {
        refreshTableMilkCollection();
    }
}