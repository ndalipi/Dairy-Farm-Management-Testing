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

    private static final String FX_PRODUCT = "product_name";
    private static final String FX_PRICE = "price";
    private static final String FX_QTY = "quantity";
    private static final String FX_SUPPLIER = "supplier_name";
    private static final String FX_DATE = "purchase_date";

    private static final String PDF_TITLE = "Purchases List";
    private static final String PDF_SUBTITLE = "This is the list of the purchases";

    private static final String[] EXPORT_PICK = {"PDF", "Excel"};
    private static final String[] PDF_HEAD = {"Product", "Price", "Quantity", "Supplier", "Date"};

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
        reloadPurchases();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        BasicConfigurator.configure();

        export_combo.setItems(FXCollections.observableArrayList(EXPORT_PICK));
        export_combo.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b == null) return;
            if ("PDF".equals(b)) exportToPDF();
            else exportToExcel();
        });

        product_c.setCellValueFactory(new PropertyValueFactory<>(FX_PRODUCT));
        price_c.setCellValueFactory(new PropertyValueFactory<>(FX_PRICE));
        quantity_c.setCellValueFactory(new PropertyValueFactory<>(FX_QTY));
        supplier_c.setCellValueFactory(new PropertyValueFactory<>(FX_SUPPLIER));
        date_c.setCellValueFactory(new PropertyValueFactory<>(FX_DATE));

        actions_c.setCellFactory(actionCell());
        hookSearch();

        reloadPurchases();
    }

    private void hookSearch() {
        search_input.textProperty().addListener((o, a, b) -> {
            if (b == null || b.isBlank()) {
                reloadPurchases();
                return;
            }
            String key = b.toLowerCase();
            ObservableList<Purchase> out = FXCollections.observableArrayList();
            try {
                for (Purchase p : fetchPurchases()) {
                    if (str(p.getSupplier_name()).contains(key) || str(p.getProduct_name()).contains(key)) out.add(p);
                }
                PurchaseTable.setItems(out);
            } catch (SQLException e) {
                displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private String str(String v) {
        return v == null ? "" : v.toLowerCase();
    }

    private void reloadPurchases() {
        try {
            PurchaseTable.setItems(fetchPurchases());
        } catch (SQLException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public ObservableList<Purchase> fetchPurchases() throws SQLException {
        ObservableList<Purchase> list = FXCollections.observableArrayList();
        String q = "SELECT id, supplier_id, stock_id, quantity, price, purchase_date, created_at, updated_at FROM purchases";

        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(q);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Purchase p = new Purchase();
                p.setId(rs.getInt("id"));
                p.setSupplier_id(rs.getInt("supplier_id"));
                p.setStock_id(rs.getInt("stock_id"));
                p.setQuantity(rs.getFloat(FX_QTY));
                p.setPrice(rs.getFloat(FX_PRICE));
                p.setPurchase_date(rs.getDate(FX_DATE));
                p.setCreated_at(rs.getTimestamp("created_at"));
                p.setUpdated_at(rs.getTimestamp("updated_at"));
                list.add(p);
            }
        }
        return list;
    }

    private Callback<TableColumn<Purchase, String>, TableCell<Purchase, String>> actionCell() {
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

                view.setOnAction(e -> showDetails(current()));
                edit.setOnAction(e -> editPurchase(current()));
                del.setOnAction(e -> deletePurchase(current()));

                menu.getItems().addAll(view, edit, del);

                actions.setOnMouseClicked(e -> menu.show(actions, e.getScreenX(), e.getScreenY()));

                HBox box = new HBox(actions);
                box.setStyle("-fx-alignment:center");
                HBox.setMargin(actions, new Insets(1, 1, 0, 3));

                setGraphic(box);
                setText(null);
            }

            private Purchase current() {
                int i = getIndex();
                if (i < 0 || i >= getTableView().getItems().size()) return null;
                return getTableView().getItems().get(i);
            }
        };
    }

    private void deletePurchase(Purchase p) {
        if (p == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Confirmation");
        alert.setHeaderText("Are you sure you want to delete this purchase sale?");
        Optional<ButtonType> res = alert.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        try {
            if (p.delete()) {
                displayAlert("success", "Purchase deleted successfully", Alert.AlertType.INFORMATION);
                reloadPurchases();
            } else {
                displayAlert(ERROR_TITLE, "Error while deleting!!!", Alert.AlertType.ERROR);
            }
        } catch (Exception ex) {
            displayAlert(ERROR_TITLE, ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void editPurchase(Purchase p) {
        if (p == null) return;

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dfms/dairy_farm_management_system/popups/add_new_purchase.fxml"));
        try {
            Scene scene = new Scene(loader.load());
            NewPurchaseController c = loader.getController();
            c.setUpdate(true);
            c.fetchPurchase(p);

            Stage stage = new Stage();
            stage.getIcons().add(new Image(APP_ICON_PATH));
            stage.setTitle("Update Purchase");
            stage.setResizable(false);
            stage.setScene(scene);
            centerScreen(stage);
            stage.show();
        } catch (IOException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showDetails(Purchase p) {
        if (p == null) return;

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dfms/dairy_farm_management_system/popups/purchase_details.fxml"));
        try {
            Scene scene = new Scene(loader.load());
            PurchaseDetailsController c = loader.getController();
            c.fetchPurchase(
                    p.getId(),
                    p.getProduct_name(),
                    p.getQuantity(),
                    p.getPrice(),
                    p.getSupplier_name(),
                    (Date) p.getPurchase_date()
            );

            Stage stage = new Stage();
            stage.getIcons().add(new Image(APP_ICON_PATH));
            stage.setTitle("Purchase Details");
            stage.setResizable(false);
            stage.setScene(scene);
            centerScreen(stage);
            stage.show();
        } catch (IOException e) {
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

        ObservableList<Purchase> rows = PurchaseTable.getItems();

        try (Workbook wb = new XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(file)) {

            Sheet sheet = wb.createSheet("Purchases");
            Row head = sheet.createRow(0);
            head.createCell(0).setCellValue("Purchase ID");
            head.createCell(1).setCellValue(PDF_HEAD[0]);
            head.createCell(2).setCellValue(PDF_HEAD[1]);
            head.createCell(3).setCellValue(PDF_HEAD[2]);
            head.createCell(4).setCellValue(PDF_HEAD[3]);
            head.createCell(5).setCellValue(PDF_HEAD[4]);

            int r = 1;
            for (Purchase p : rows) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(String.valueOf(p.getId()));
                row.createCell(1).setCellValue(val(p.getProduct_name()));
                row.createCell(2).setCellValue(String.valueOf(p.getPrice()));
                row.createCell(3).setCellValue(String.valueOf(p.getQuantity()));
                row.createCell(4).setCellValue(val(p.getSupplier_name()));
                row.createCell(5).setCellValue(String.valueOf(p.getPurchase_date()));
            }

            wb.write(out);
            displayAlert("Success", "Purchases exported successfully", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private String val(String s) {
        return s == null ? "" : s;
    }

    void exportToPDF() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save As");
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File file = chooser.showSaveDialog(null);
        if (file == null) return;

        ObservableList<Purchase> rows = PurchaseTable.getItems();

        try {
            Document doc = new Document();
            doc.setPageSize(PageSize.A4.rotate());
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            Paragraph t = new Paragraph(PDF_TITLE, FontFactory.getFont(FontFactory.COURIER_BOLD, 20, BaseColor.BLACK));
            Paragraph s = new Paragraph(PDF_SUBTITLE, FontFactory.getFont(FontFactory.COURIER, 14, BaseColor.BLACK));
            t.setAlignment(Element.ALIGN_CENTER);
            s.setAlignment(Element.ALIGN_CENTER);
            t.setSpacingAfter(30);
            s.setSpacingAfter(30);
            doc.add(t);
            doc.add(s);

            PdfPTable table = new PdfPTable(PDF_HEAD.length);
            table.setWidthPercentage(100);
            table.setSpacingBefore(11f);
            table.setSpacingAfter(11f);

            for (String h : PDF_HEAD) {
                PdfPCell cell = new PdfPCell(new Paragraph(h, FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)));
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (Purchase p : rows) {
                PdfPCell c1 = new PdfPCell(new Paragraph(val(p.getProduct_name()))); c1.setPadding(5); table.addCell(c1);
                PdfPCell c2 = new PdfPCell(new Paragraph(String.valueOf(p.getPrice()))); c2.setPadding(5); table.addCell(c2);
                PdfPCell c3 = new PdfPCell(new Paragraph(String.valueOf(p.getQuantity()))); c3.setPadding(5); table.addCell(c3);
                PdfPCell c4 = new PdfPCell(new Paragraph(val(p.getSupplier_name()))); c4.setPadding(5); table.addCell(c4);
                PdfPCell c5 = new PdfPCell(new Paragraph(String.valueOf(p.getPurchase_date()))); c5.setPadding(5); table.addCell(c5);
            }

            doc.add(table);
            doc.close();

            displayAlert("Success", "Purchases exported successfully", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}
