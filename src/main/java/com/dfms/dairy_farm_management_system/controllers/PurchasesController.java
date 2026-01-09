package com.dfms.dairy_farm_management_system.controllers;

import com.dfms.dairy_farm_management_system.Main;
import com.dfms.dairy_farm_management_system.connection.DBConfig;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.NewPurchaseController;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.PurchaseDetailsController;
import com.dfms.dairy_farm_management_system.models.Purchase;
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
import javafx.util.Callback;
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

public class PurchasesController implements Initializable {

    private static final String ERROR_TITLE = "Error";
    private static final String ICON_STYLE = "-fx-background-color: transparent;-fx-cursor: hand;-fx-size:15px;";
    private static final String APP_ICON_PATH = "file:src/main/resources/images/logo.png";

    private static final String PROP_PRODUCT_NAME = "product_name";
    private static final String PROP_PRICE = "price";
    private static final String PROP_QUANTITY = "quantity";
    private static final String PROP_SUPPLIER_NAME = "supplier_name";
    private static final String PROP_PURCHASE_DATE = "purchase_date";

    private static final String COL_PURCHASE_ID = "purchase_id";
    private static final String COL_PRODUCT_NAME = "product_name";
    private static final String COL_PRICE = "price";
    private static final String COL_QUANTITY = "quantity";
    private static final String COL_SUPPLIER_NAME = "supplier_name";
    private static final String COL_PURCHASE_DATE = "purchase_date";

    private static final int PDF_COLUMNS_COUNT = 5;

    private static final Image EDIT_IMG = new Image(PurchasesController.class.getResourceAsStream("/images/edit.png"));
    private static final Image DELETE_IMG = new Image(PurchasesController.class.getResourceAsStream("/images/delete.png"));
    private static final Image VIEW_IMG = new Image(PurchasesController.class.getResourceAsStream("/images/eye.png"));

    @FXML private TableView<Purchase> PurchaseTable;
    @FXML private TableColumn<Purchase, String> actions_c;
    @FXML private TableColumn<Purchase, Date> date_c;
    @FXML private TableColumn<Purchase, Float> price_c;
    @FXML private TableColumn<Purchase, Float> quantity_c;
    @FXML private TableColumn<Purchase, String> supplier_c;
    @FXML private TableColumn<Purchase, String> product_c;
    @FXML private ComboBox<String> export_combo;
    @FXML private TextField search_input;

    private final Connection connection = getConnection();

    @FXML
    void openAddPurchase(MouseEvent event) throws IOException {
        openNewWindow("Add New Purchase", "add_new_purchase");
    }

    @FXML
    void refreshTable(MouseEvent event) {
        refreshTablePurchase();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        BasicConfigurator.configure();

        export_combo.setItems(FXCollections.observableArrayList("PDF", "Excel"));

        export_combo.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            if (t1 == null) return;
            if ("PDF".equals(t1)) exportToPDF();
            else exportToExcel();
        });

        setupColumns();
        setupActionsColumn();

        liveSearch(search_input, PurchaseTable);
        refreshTablePurchase();
    }

    private void setupColumns() {
        product_c.setCellValueFactory(new PropertyValueFactory<>(PROP_PRODUCT_NAME));
        price_c.setCellValueFactory(new PropertyValueFactory<>(PROP_PRICE));
        quantity_c.setCellValueFactory(new PropertyValueFactory<>(PROP_QUANTITY));
        supplier_c.setCellValueFactory(new PropertyValueFactory<>(PROP_SUPPLIER_NAME));
        date_c.setCellValueFactory(new PropertyValueFactory<>(PROP_PURCHASE_DATE));
    }

    public ObservableList<Purchase> getPurchase() throws SQLException {
        ObservableList<Purchase> list = FXCollections.observableArrayList();
        String query = "SELECT id, supplier_id, stock_id, quantity, price, purchase_date, created_at, updated_at FROM purchases";

        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Purchase purchase = new Purchase();

                purchase.setId(rs.getInt("id"));
                purchase.setSupplier_id(rs.getInt("supplier_id"));
                purchase.setStock_id(rs.getInt("stock_id"));
                purchase.setQuantity(rs.getFloat(PROP_QUANTITY));
                purchase.setPrice(rs.getFloat(PROP_PRICE));
                purchase.setPurchase_date(rs.getDate(PROP_PURCHASE_DATE));
                purchase.setCreated_at(rs.getTimestamp("created_at"));
                purchase.setUpdated_at(rs.getTimestamp("updated_at"));

                list.add(purchase);
            }
        }

        return list;
    }

    public void refreshTablePurchase() {
        try {
            ObservableList<Purchase> list = getPurchase();
            PurchaseTable.setItems(list);
        } catch (SQLException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupActionsColumn() {
        actions_c.setCellFactory(createActionsFactory());
    }

    private Callback<TableColumn<Purchase, String>, TableCell<Purchase, String>> createActionsFactory() {
        return (TableColumn<Purchase, String> param) -> new TableCell<>() {

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                ImageView ivView = iconView(VIEW_IMG);
                ImageView ivEdit = iconView(EDIT_IMG);
                ImageView ivDelete = iconView(DELETE_IMG);

                HBox manage = new HBox(ivView, ivEdit, ivDelete);
                manage.setStyle("-fx-alignment:center");
                setMargins(ivView, ivEdit, ivDelete);

                ivDelete.setOnMouseClicked(e -> {
                    Purchase purchase = getTableView().getItems().get(getIndex());
                    if (purchase == null) return;

                    if (!confirmDelete("Delete Confirmation", "Are you sure you want to delete this purchase sale?")) return;

                    try {
                        if (purchase.delete()) {
                            displayAlert("success", "Purchase deleted successfully", Alert.AlertType.INFORMATION);
                            refreshTablePurchase();
                        } else {
                            displayAlert(ERROR_TITLE, "Error while deleting!!!", Alert.AlertType.ERROR);
                        }
                    } catch (Exception ex) {
                        displayAlert(ERROR_TITLE, ex.getMessage(), Alert.AlertType.ERROR);
                    }
                });

                ivEdit.setOnMouseClicked(e -> {
                    Purchase purchase = getTableView().getItems().get(getIndex());
                    if (purchase == null) return;

                    openPopup(
                            "/com/dfms/dairy_farm_management_system/popups/add_new_purchase.fxml",
                            "Update Purchase",
                            controller -> {
                                NewPurchaseController c = (NewPurchaseController) controller;
                                c.setUpdate(true);
                                c.fetchPurchase(purchase);
                            }
                    );
                });

                ivView.setOnMouseClicked(e -> {
                    Purchase purchase = getTableView().getItems().get(getIndex());
                    if (purchase == null) return;

                    openPopup(
                            "/com/dfms/dairy_farm_management_system/popups/purchase_details.fxml",
                            "Purchase Details",
                            controller -> {
                                PurchaseDetailsController c = (PurchaseDetailsController) controller;
                                c.fetchPurchase(
                                        purchase.getId(),
                                        purchase.getProduct_name(),
                                        purchase.getQuantity(),
                                        purchase.getPrice(),
                                        purchase.getSupplier_name(),
                                        (Date) purchase.getPurchase_date()
                                );
                            }
                    );
                });

                setGraphic(manage);
                setText(null);
            }
        };
    }

    private ImageView iconView(Image img) {
        ImageView iv = new ImageView(img);
        iv.setStyle(ICON_STYLE);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setCache(true);
        return iv;
    }

    private void setMargins(ImageView... views) {
        for (ImageView v : views) {
            HBox.setMargin(v, new Insets(1, 1, 0, 3));
        }
    }

    private boolean confirmDelete(String title, String header) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void openPopup(String fxmlPath, String title, ControllerInit initializer) {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
        try {
            Scene scene = new Scene(loader.load());
            Object controller = loader.getController();
            initializer.init(controller);

            Stage stage = new Stage();
            stage.getIcons().add(new Image(APP_ICON_PATH));
            stage.setTitle(title);
            stage.setResizable(false);
            stage.setScene(scene);
            centerScreen(stage);
            stage.show();
        } catch (IOException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FunctionalInterface
    private interface ControllerInit {
        void init(Object controller);
    }

    // ========================= SEARCH =========================
    public void liveSearch(TextField search_input, TableView<Purchase> table) {
        search_input.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                refreshTablePurchase();
                return;
            }

            String needle = newValue.toLowerCase();
            ObservableList<Purchase> filteredList = FXCollections.observableArrayList();
            try {
                for (Purchase p : getPurchase()) {
                    String supplier = p.getSupplier_name() == null ? "" : p.getSupplier_name().toLowerCase();
                    String product = p.getProduct_name() == null ? "" : p.getProduct_name().toLowerCase();
                    if (supplier.contains(needle) || product.contains(needle)) {
                        filteredList.add(p);
                    }
                }
                table.setItems(filteredList);
            } catch (SQLException e) {
                displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    // ========================= EXPORT EXCEL =========================
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

            Sheet sheet = workbook.createSheet("Purchases");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Purchase ID");
            header.createCell(1).setCellValue("Product");
            header.createCell(2).setCellValue("Price");
            header.createCell(3).setCellValue("Quantity");
            header.createCell(4).setCellValue("Supplier");
            header.createCell(5).setCellValue("Date");

            String query =
                    "SELECT pur.id AS " + COL_PURCHASE_ID + ", " +
                            "st.name AS " + COL_PRODUCT_NAME + ", " +
                            "pur.price AS " + COL_PRICE + ", " +
                            "pur.quantity AS " + COL_QUANTITY + ", " +
                            "s.name AS " + COL_SUPPLIER_NAME + ", " +
                            "pur.purchase_date AS " + COL_PURCHASE_DATE + " " +
                            "FROM purchases pur " +
                            "JOIN suppliers s ON pur.supplier_id = s.id " +
                            "JOIN stocks st ON pur.stock_id = st.id";

            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery(query)) {

                int rowNum = 1;
                while (rs.next()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(rs.getString(COL_PURCHASE_ID));
                    row.createCell(1).setCellValue(rs.getString(COL_PRODUCT_NAME));
                    row.createCell(2).setCellValue(rs.getString(COL_PRICE));
                    row.createCell(3).setCellValue(rs.getString(COL_QUANTITY));
                    row.createCell(4).setCellValue(rs.getString(COL_SUPPLIER_NAME));
                    row.createCell(5).setCellValue(rs.getString(COL_PURCHASE_DATE));
                }
            }

            workbook.write(fileOutputStream);
            displayAlert("Success", "Purchases exported successfully", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ========================= EXPORT PDF =========================
    void exportToPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File file = fileChooser.showSaveDialog(null);
        if (file == null) return;

        try {
            Document document = new Document();
            document.setPageSize(PageSize.A4.rotate());

            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            Paragraph title = new Paragraph("Purchases List",
                    FontFactory.getFont(FontFactory.COURIER_BOLD, 20, BaseColor.BLACK));
            Paragraph text = new Paragraph("This is the list of the purchases",
                    FontFactory.getFont(FontFactory.COURIER, 14, BaseColor.BLACK));

            title.setAlignment(Element.ALIGN_CENTER);
            text.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30);
            text.setSpacingAfter(30);

            document.add(title);
            document.add(text);

            PdfPTable table = new PdfPTable(PDF_COLUMNS_COUNT);
            table.setWidthPercentage(100);
            table.setSpacingBefore(11f);
            table.setSpacingAfter(11f);

            addPdfHeader(table);

            ObservableList<Purchase> purchases = PurchaseTable.getItems();
            NewPurchaseController controller = new NewPurchaseController();

            for (Purchase p : purchases) {
                Purchase pur = controller.getPurchase(p.getId());
                if (pur == null) continue;

                table.addCell(new PdfPCell(new Paragraph(pur.getProduct_name()))).setPadding(5);
                table.addCell(new PdfPCell(new Paragraph(String.valueOf(pur.getPrice())))).setPadding(5);
                table.addCell(new PdfPCell(new Paragraph(String.valueOf(pur.getQuantity())))).setPadding(5);
                table.addCell(new PdfPCell(new Paragraph(pur.getSupplier_name()))).setPadding(5);
                table.addCell(new PdfPCell(new Paragraph(String.valueOf(pur.getPurchase_date())))).setPadding(5);
            }

            document.add(table);
            document.close();

            displayAlert("Success", "Purchases exported successfully", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void addPdfHeader(PdfPTable table) {
        table.addCell(new PdfPCell(new Paragraph("Product", FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)))).setPadding(5);
        table.addCell(new PdfPCell(new Paragraph("Price", FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)))).setPadding(5);
        table.addCell(new PdfPCell(new Paragraph("Quantity", FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)))).setPadding(5);
        table.addCell(new PdfPCell(new Paragraph("Supplier", FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)))).setPadding(5);
        table.addCell(new PdfPCell(new Paragraph("Date", FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)))).setPadding(5);
    }
}
