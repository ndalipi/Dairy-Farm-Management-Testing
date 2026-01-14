package com.dfms.dairy_farm_management_system.controllers;

import com.dfms.dairy_farm_management_system.Main;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.UpdateProductController;
import com.dfms.dairy_farm_management_system.models.Stock;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.Optional;
import java.util.ResourceBundle;

import static com.dfms.dairy_farm_management_system.connection.DBConfig.getConnection;
import static com.dfms.dairy_farm_management_system.helpers.Helper.*;

public class StockController implements Initializable {

    private static final String SUCCESS_TITLE = "Success";
    private static final String ERROR_TITLE = "Error";
    private static final String COLUMN_QUANTITY = "Quantity";

    private static final String ICON_STYLE = "-fx-background-color: transparent;-fx-cursor: hand;-fx-size:15px;";
    private static final String HBOX_CENTER_STYLE = "-fx-alignment:center";
    private static final String LOGO_PATH = "file:src/main/resources/images/logo.png";

    private static final String DELETE_PRODUCT_TITLE = "Delete Product";
    private static final String DELETE_PRODUCT_HEADER = "Are you sure you want to delete this product?";
    private static final String PRODUCT_DELETED_MSG = "Product deleted successfully";

    private static final String UPDATE_PRODUCT_TITLE = "Update Product";
    private static final String UPDATE_PRODUCT_FXML = "/com/dfms/dairy_farm_management_system/popups/update_product.fxml";

    private static final String ICON_EDIT_PATH = "/images/edit.png";
    private static final String ICON_DELETE_PATH = "/images/delete.png";

    private static final int COLUMNS_COUNT = 7;

    private static final String EXPORT_QUERY =
            "SELECT id, name, type, Quantity, unit, created_at FROM stocks";

    private static final String[] EXPORT_HEADERS = {
            "Product ID",
            "Product Name",
            "Product Type",
            COLUMN_QUANTITY,
            "Availability",
            "Unit",
            "Added Date"
    };

    @FXML
    private TableColumn<Stock, String> actions_col;

    @FXML
    private TableColumn<Stock, String> product_qunatity_col;

    @FXML
    private TableColumn<Stock, String> availability_col;

    @FXML
    private ComboBox<String> export_combo;

    @FXML
    private TableColumn<Stock, String> id_col;

    @FXML
    private Button openAddNewEmployeeBtn;

    @FXML
    private TableColumn<Stock, String> product_name_col;

    @FXML
    private TableColumn<Stock, String> product_type_col;

    @FXML
    private TextField search_stock_input;

    @FXML
    private TableView<Stock> stock_table;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //this line of code is so important for the export !!!!
        BasicConfigurator.configure();

        ObservableList<String> list = FXCollections.observableArrayList("PDF", "Excel");
        export_combo.setItems(list);
        displayStock();

        export_combo.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            if (t1.equals("PDF")) {
                exportToPDF();
            } else {
                exportToExcel();
            }
        });

        liveSearch(search_stock_input, stock_table);
    }

    public ObservableList<Stock> getProducts() {
        ObservableList<Stock> products = FXCollections.observableArrayList();

        String query = "SELECT id, name, type, Quantity, unit FROM stocks";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Stock product = new Stock();

                product.setId(rs.getInt("id"));
                product.setName(rs.getString("name"));
                product.setType(rs.getString("type"));

                float quantity = rs.getFloat(COLUMN_QUANTITY);
                product.setQuantity(quantity);
                product.setAvailability(quantity > 0);

                product.setUnit(rs.getString("unit"));
                products.add(product);
            }

        } catch (SQLException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }

        return products;
    }

    public void displayStock() {
        ObservableList<Stock> products = getProducts();

        id_col.setCellValueFactory(new PropertyValueFactory<>("id"));
        product_name_col.setCellValueFactory(new PropertyValueFactory<>("name"));
        product_type_col.setCellValueFactory(new PropertyValueFactory<>("type"));
        product_qunatity_col.setCellValueFactory(new PropertyValueFactory<>(COLUMN_QUANTITY));
        availability_col.setCellValueFactory(new PropertyValueFactory<>("availability"));

        actions_col.setCellFactory(col -> new ActionCell());
        stock_table.setItems(products);
    }

    private class ActionCell extends TableCell<Stock, String> {

        private final Image editImg = new Image(getClass().getResourceAsStream(ICON_EDIT_PATH));
        private final Image deleteImg = new Image(getClass().getResourceAsStream(ICON_DELETE_PATH));

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setGraphic(null);
                setText(null);
                return;
            }

            ImageView ivEdit = buildIcon(editImg);
            ImageView ivDelete = buildIcon(deleteImg);

            setGraphic(buildActionsBox(ivEdit, ivDelete));
            setText(null);

            ivDelete.setOnMouseClicked(event -> handleDeleteProduct(getSelectedProduct()));
            ivEdit.setOnMouseClicked(event -> handleEditProduct(getSelectedProduct()));
        }

        private ImageView buildIcon(Image img) {
            ImageView iv = new ImageView(img);
            iv.setStyle(ICON_STYLE);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            iv.setCache(true);
            return iv;
        }

        private HBox buildActionsBox(ImageView... icons) {
            HBox box = new HBox(icons);
            box.setStyle(HBOX_CENTER_STYLE);
            for (ImageView iv : icons) {
                HBox.setMargin(iv, new Insets(1, 1, 0, 3));
            }
            return box;
        }

        private Stock getSelectedProduct() {
            return stock_table.getSelectionModel().getSelectedItem();
        }

        private boolean userConfirmedDelete() {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(DELETE_PRODUCT_TITLE);
            alert.setHeaderText(DELETE_PRODUCT_HEADER);

            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.OK;
        }

        private void handleDeleteProduct(Stock product) {
            if (product == null) return;
            if (!userConfirmedDelete()) return;

            try {
                if (product.delete()) {
                    displayAlert(SUCCESS_TITLE, PRODUCT_DELETED_MSG, Alert.AlertType.INFORMATION);
                    displayStock();
                }
            } catch (Exception e) {
                displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
            }
        }

        private void handleEditProduct(Stock product) {
            if (product == null) return;

            try {
                FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource(UPDATE_PRODUCT_FXML));
                Scene scene = new Scene(fxmlLoader.load());

                UpdateProductController controller = fxmlLoader.getController();
                controller.fetchProduct(product);

                Stage stage = new Stage();
                stage.getIcons().add(new Image(LOGO_PATH));
                stage.setTitle(UPDATE_PRODUCT_TITLE);
                stage.setResizable(false);
                stage.setScene(scene);
                centerScreen(stage);
                stage.show();

            } catch (IOException e) {
                displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    void openAddProduct(MouseEvent event) throws IOException {
        openNewWindow("Add Product", "add_new_product");
    }

    @FXML
    public void refreshTable() {
        stock_table.getItems().clear();
        displayStock();
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

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fileOutputStream = new FileOutputStream(file);
             Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(EXPORT_QUERY);
             ResultSet rs = ps.executeQuery()) {

            Sheet sheet = workbook.createSheet("Stock");
            Row header = sheet.createRow(0);

            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                header.createCell(i).setCellValue(EXPORT_HEADERS[i]);
            }

            int rowNum = 1;
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);

                float quantity = rs.getFloat(COLUMN_QUANTITY);

                row.createCell(0).setCellValue(rs.getString("id"));
                row.createCell(1).setCellValue(rs.getString("name"));
                row.createCell(2).setCellValue(rs.getString("type"));
                row.createCell(3).setCellValue(String.valueOf(quantity));
                row.createCell(4).setCellValue(quantity > 0 ? "Available" : "Not Available");
                row.createCell(5).setCellValue(rs.getString("unit"));
                row.createCell(6).setCellValue(rs.getString("created_at"));
            }

            workbook.write(fileOutputStream);
            displayAlert(SUCCESS_TITLE, "Stock exported successfully", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

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
                document.setPageSize(PageSize.A4.rotate());

                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                Paragraph title = new Paragraph("Stock List", FontFactory.getFont(FontFactory.COURIER_BOLD, 20, BaseColor.BLACK));
                Paragraph text = new Paragraph("This is the list of the products", FontFactory.getFont(FontFactory.COURIER, 14, BaseColor.BLACK));

                title.setAlignment(Element.ALIGN_CENTER);
                text.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(30);
                text.setSpacingAfter(30);

                document.add(title);
                document.add(text);

                PdfPTable table = new PdfPTable(COLUMNS_COUNT);
                table.setWidthPercentage(100);
                table.setSpacingBefore(11f);
                table.setSpacingAfter(11f);

                float[] colWidth = new float[COLUMNS_COUNT];
                for (int i = 0; i < COLUMNS_COUNT; i++) {
                    colWidth[i] = 2f;
                }
                table.setWidths(colWidth);

                for (String header : EXPORT_HEADERS) {
                    PdfPCell cell = new PdfPCell(new Paragraph(header, FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)));
                    cell.setPadding(5);
                    table.addCell(cell);
                }

                while (rs.next()) {
                    float quantity = rs.getFloat(COLUMN_QUANTITY);

                    table.addCell(new PdfPCell(new Paragraph(rs.getString("id"))));
                    table.addCell(new PdfPCell(new Paragraph(rs.getString("name"))));
                    table.addCell(new PdfPCell(new Paragraph(rs.getString("type"))));
                    table.addCell(new PdfPCell(new Paragraph(String.valueOf(quantity))));
                    table.addCell(new PdfPCell(new Paragraph(quantity > 0 ? "Available" : "Not Available")));
                    table.addCell(new PdfPCell(new Paragraph(rs.getString("unit"))));
                    table.addCell(new PdfPCell(new Paragraph(rs.getString("created_at"))));
                }

                document.add(table);
                document.close();
                displayAlert(SUCCESS_TITLE, "Stock exported successfully", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    public void liveSearch(TextField search_input, TableView table) {
        search_input.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                refreshTable();
            } else {
                ObservableList<Stock> filteredList = FXCollections.observableArrayList();
                ObservableList<Stock> products = getProducts();
                for (Stock product : products) {
                    if (product.getName().toLowerCase().contains(newValue.toLowerCase())
                            || product.getType().toLowerCase().contains(newValue.toLowerCase())) {
                        filteredList.add(product);
                    }
                }
                table.setItems(filteredList);
            }
        });
    }
}