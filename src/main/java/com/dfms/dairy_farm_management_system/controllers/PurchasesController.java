package com.dfms.dairy_farm_management_system.controllers;

import com.dfms.dairy_farm_management_system.Main;
import com.dfms.dairy_farm_management_system.connection.DBConfig;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.NewPurchaseController;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.PurchaseDetailsController;
import com.dfms.dairy_farm_management_system.models.Purchase;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
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
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
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
import org.apache.log4j.BasicConfigurator;
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
import java.sql.Statement;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.dfms.dairy_farm_management_system.connection.DBConfig.getConnection;
import static com.dfms.dairy_farm_management_system.helpers.Helper.centerScreen;
import static com.dfms.dairy_farm_management_system.helpers.Helper.displayAlert;
import static com.dfms.dairy_farm_management_system.helpers.Helper.openNewWindow;

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

    private static final String TITLE_TEXT = "Purchases List";
    private static final String SUBTITLE_TEXT = "This is the list of the purchases";

    private static final String[] EXPORT_OPTIONS = {"PDF", "Excel"};
    private static final String[] TABLE_HEADERS = {"Product", "Price", "Quantity", "Supplier", "Date"};

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

        export_combo.setItems(FXCollections.observableArrayList(EXPORT_OPTIONS));
        export_combo.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            if ("PDF".equals(newV)) exportToPDF();
            else exportToExcel();
        });

        bindColumns();
        actions_c.setCellFactory(createActionsFactory());
        wireLiveSearch();

        refreshTablePurchase();
    }

    private void bindColumns() {
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
            PurchaseTable.setItems(getPurchase());
        } catch (SQLException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private Callback<TableColumn<Purchase, String>, TableCell<Purchase, String>> createActionsFactory() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                ImageView ivView = buildIcon(VIEW_IMG);
                ImageView ivEdit = buildIcon(EDIT_IMG);
                ImageView ivDelete = buildIcon(DELETE_IMG);

                HBox manage = new HBox(ivView, ivEdit, ivDelete);
                manage.setStyle("-fx-alignment:center");
                applyMargins(ivView, ivEdit, ivDelete);

                ivDelete.setOnMouseClicked(e -> onDelete());
                ivEdit.setOnMouseClicked(e -> onEdit());
                ivView.setOnMouseClicked(e -> onView());

                setGraphic(manage);
                setText(null);
            }

            private Purchase current() {
                int idx = getIndex();
                if (idx < 0 || idx >= getTableView().getItems().size()) return null;
                return getTableView().getItems().get(idx);
            }

            private void onDelete() {
                Purchase purchase = current();
                if (purchase == null) return;

                if (!confirm("Delete Confirmation", "Are you sure you want to delete this purchase sale?")) return;

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
            }

            private void onEdit() {
                Purchase purchase = current();
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
            }

            private void onView() {
                Purchase purchase = current();
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
            }
        };
    }

    private void wireLiveSearch() {
        search_input.textProperty().addListener((obs, oldV, newV) -> {
            if (newV == null || newV.isBlank()) {
                refreshTablePurchase();
                return;
            }
            String needle = newV.toLowerCase();

            ObservableList<Purchase> filtered = FXCollections.observableArrayList();
            try {
                for (Purchase p : getPurchase()) {
                    String s1 = safeLower(p.getSupplier_name());
                    String s2 = safeLower(p.getProduct_name());
                    if (s1.contains(needle) || s2.contains(needle)) filtered.add(p);
                }
                PurchaseTable.setItems(filtered);
            } catch (SQLException e) {
                displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    private ImageView buildIcon(Image img) {
        ImageView iv = new ImageView(img);
        iv.setStyle(ICON_STYLE);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setCache(true);
        return iv;
    }

    private void applyMargins(ImageView... views) {
        for (ImageView v : views) {
            HBox.setMargin(v, new Insets(1, 1, 0, 3));
        }
    }

    private boolean confirm(String title, String header) {
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
            initializer.init(loader.getController());

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
            writeSheetHeader(sheet, TABLE_HEADERS);

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

    private void writeSheetHeader(Sheet sheet, String[] headers) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Purchase ID");
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i + 1).setCellValue(headers[i]);
        }
    }

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

            writePdfTitle(document, TITLE_TEXT, SUBTITLE_TEXT);

            PdfPTable table = new PdfPTable(TABLE_HEADERS.length);
            table.setWidthPercentage(100);
            table.setSpacingBefore(11f);
            table.setSpacingAfter(11f);

            writePdfHeader(table, TABLE_HEADERS);

            ObservableList<Purchase> purchases = PurchaseTable.getItems();
            NewPurchaseController controller = new NewPurchaseController();

            for (Purchase p : purchases) {
                Purchase pur = controller.getPurchase(p.getId());
                if (pur == null) continue;

                addPdfRow(table,
                        pur.getProduct_name(),
                        String.valueOf(pur.getPrice()),
                        String.valueOf(pur.getQuantity()),
                        pur.getSupplier_name(),
                        String.valueOf(pur.getPurchase_date())
                );
            }

            document.add(table);
            document.close();

            displayAlert("Success", "Purchases exported successfully", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void writePdfTitle(Document document, String titleText, String subtitleText) throws Exception {
        Paragraph title = new Paragraph(titleText, FontFactory.getFont(FontFactory.COURIER_BOLD, 20, BaseColor.BLACK));
        Paragraph text = new Paragraph(subtitleText, FontFactory.getFont(FontFactory.COURIER, 14, BaseColor.BLACK));

        title.setAlignment(Element.ALIGN_CENTER);
        text.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(30);
        text.setSpacingAfter(30);

        document.add(title);
        document.add(text);
    }

    private void writePdfHeader(PdfPTable table, String[] headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Paragraph(h, FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)));
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void addPdfRow(PdfPTable table, String... values) {
        for (String v : values) {
            PdfPCell cell = new PdfPCell(new Paragraph(v == null ? "" : v));
            cell.setPadding(5);
            table.addCell(cell);
        }
    }
}
