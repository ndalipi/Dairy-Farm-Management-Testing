package com.dfms.dairy_farm_management_system.controllers;

import com.dfms.dairy_farm_management_system.Main;
import com.dfms.dairy_farm_management_system.connection.DBConfig;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.ClientDetailsController;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.NewClientController;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.NewSupplierController;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.SupplierDetailsController;
import com.dfms.dairy_farm_management_system.models.Client;
import com.dfms.dairy_farm_management_system.models.Supplier;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
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
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dfms.dairy_farm_management_system.helpers.Helper.*;

@SuppressWarnings("java:S116") // FXML fields use specific names; renaming breaks FXML injection
public class ClientsSuppliersController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(ClientsSuppliersController.class.getName());

    private static final String APP_ICON_PATH = "file:src/main/resources/images/logo.png";
    private static final String ERROR_TITLE = "Error";
    private static final String ICON_STYLE = "-fx-background-color: transparent;-fx-cursor: hand;-fx-size:15px;";

    private static final ObservableList<String> EXPORT_OPTIONS =
            FXCollections.observableArrayList("PDF", "Excel");

    private static final String CLIENTS_QUERY = "SELECT * FROM `clients`";
    private static final String SUPPLIERS_QUERY = "SELECT * FROM `suppliers`";

    // ========================= CLIENT UI =========================
    @FXML private TableView<Client> TableClient;
    @FXML private TableColumn<Client, Integer> colidClient;
    @FXML private TableColumn<Client, String> colnameClient;
    @FXML private TableColumn<Client, String> coltypeClient;
    @FXML private TableColumn<Client, String> colemailClient;
    @FXML private TableColumn<Client, String> colphoneClient;
    @FXML private TableColumn<Client, String> actionClient;

    @FXML private ComboBox<String> export_combo;
    @FXML private TextField search_input_client;

    // ========================= SUPPLIER UI =========================
    @FXML private TableView<Supplier> TableSupplier;
    @FXML private TableColumn<Supplier, Integer> colidSupplier;
    @FXML private TableColumn<Supplier, String> colnameSupplier;
    @FXML private TableColumn<Supplier, String> coltypeSupplier;
    @FXML private TableColumn<Supplier, String> colemailSupplier;
    @FXML private TableColumn<Supplier, String> colphoneSupplier;
    @FXML private TableColumn<Supplier, String> colactionSupplier;

    @FXML private ComboBox<String> export_combo_sup;
    @FXML private TextField search_input_supplier;

    // Backing lists
    private final ObservableList<Client> listClient = FXCollections.observableArrayList();
    private final ObservableList<Supplier> listSupplier = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initExportCombos();
        setupClientTableColumns();
        setupSupplierTableColumns();

        refreshTableClient();
        refreshTableSupplier(); // ✅ FIXED: now exists as no-arg method

        wireExport(export_combo, "Clients List", "Clients", CLIENTS_QUERY);
        wireExport(export_combo_sup, "Suppliers List", "Suppliers", SUPPLIERS_QUERY);

        setupLiveSearch(search_input_client, TableClient, listClient, Client::getName);
        setupLiveSearch(search_input_supplier, TableSupplier, listSupplier, Supplier::getNameSupplier);
    }

    private void initExportCombos() {
        export_combo.setItems(EXPORT_OPTIONS);
        export_combo_sup.setItems(EXPORT_OPTIONS);
    }

    private void wireExport(ComboBox<String> combo, String title, String sheetName, String query) {
        combo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            if ("PDF".equals(newVal)) exportToPDF(title, query);
            else exportToExcel(sheetName, query, title);
        });
    }

    // ========================= OPEN POPUPS =========================
    @FXML
    void openAddClient(MouseEvent event) throws IOException {
        openNewWindow("Add client", "add_new_client");
    }

    @FXML
    void openAddSupplier(MouseEvent event) throws IOException {
        openNewWindow("Add supplier", "add_new_supplier");
    }

    // ========================= CLIENTS =========================
    private void setupClientTableColumns() {
        colidClient.setCellValueFactory(new PropertyValueFactory<>("id"));
        colnameClient.setCellValueFactory(new PropertyValueFactory<>("name"));
        coltypeClient.setCellValueFactory(new PropertyValueFactory<>("type"));
        colemailClient.setCellValueFactory(new PropertyValueFactory<>("email"));
        colphoneClient.setCellValueFactory(new PropertyValueFactory<>("phone"));

        actionClient.setCellFactory(createActionCellFactory(
                TableClient,
                this::refreshTableClient,
                this::openClientDetails,
                this::openEditClient,
                client -> client.delete(),
                "Delete Confirmation",
                "Are you sure you want to delete this client?",
                "Delete Client",
                "Client deleted successfully"
        ));
    }

    public void refreshTableClient() {
        listClient.setAll(fetchClients());
        TableClient.setItems(listClient);
    }

    @FXML
    public void refreshTable(MouseEvent mouseEvent) {
        refreshTableClient();
    }

    private ObservableList<Client> fetchClients() {
        return fetchList(CLIENTS_QUERY, rs -> {
            Client client = new Client();
            client.setId(rs.getInt("id"));
            client.setName(rs.getString("name"));
            client.setType(rs.getString("type"));
            client.setPhone(rs.getString("phone"));
            client.setEmail(rs.getString("email"));
            client.setCreated_at(rs.getTimestamp("created_at"));
            client.setUpdated_at(rs.getTimestamp("updated_at"));
            return client;
        }, "Failed to load clients");
    }

    private void openClientDetails(Client client) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(
                    "/com/dfms/dairy_farm_management_system/popups/client_details.fxml"));
            Scene scene = new Scene(loader.load());
            ClientDetailsController controller = loader.getController();
            controller.fetchClient(client);
            showStage("Client Details", scene);
        } catch (IOException e) {
            logAndAlert("Failed to open client details", e);
        }
    }

    private void openEditClient(Client client) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(
                    "/com/dfms/dairy_farm_management_system/popups/add_new_client.fxml"));
            Scene scene = new Scene(loader.load());
            NewClientController controller = loader.getController();
            controller.setUpdate(true);
            controller.fetchClient(client.getId(), client.getName(), client.getEmail(), client.getPhone(), client.getType());
            showStage("Update Client", scene);
        } catch (IOException e) {
            logAndAlert("Failed to open edit client", e);
        }
    }

    // ========================= SUPPLIERS =========================
    private void setupSupplierTableColumns() {
        colidSupplier.setCellValueFactory(new PropertyValueFactory<>("id"));
        colnameSupplier.setCellValueFactory(new PropertyValueFactory<>("nameSupplier"));
        coltypeSupplier.setCellValueFactory(new PropertyValueFactory<>("typeSupplier"));
        colemailSupplier.setCellValueFactory(new PropertyValueFactory<>("emailSupplier"));
        colphoneSupplier.setCellValueFactory(new PropertyValueFactory<>("phoneSupplier"));

        colactionSupplier.setCellFactory(createActionCellFactory(
                TableSupplier,
                this::refreshTableSupplier,
                this::openSupplierDetails,
                this::openEditSupplier,
                supplier -> supplier.delete(),
                "Delete Confirmation",
                "Are you sure you want to delete this supplier?",
                "Delete Supplier",
                "Supplier deleted successfully"
        ));
    }

    // ✅ THIS is the method your initialize() needs (no args)
    private void refreshTableSupplier() {
        listSupplier.setAll(fetchSuppliers());
        TableSupplier.setItems(listSupplier);
    }

    // ✅ Keep FXML handler for refresh button
    @FXML
    void refreshTableSupplier(MouseEvent event) {
        refreshTableSupplier();
    }

    private ObservableList<Supplier> fetchSuppliers() {
        return fetchList(SUPPLIERS_QUERY, rs -> {
            Supplier supplier = new Supplier();
            supplier.setId(rs.getInt("id"));
            supplier.setNameSupplier(rs.getString("name"));
            supplier.setTypeSupplier(rs.getString("type"));
            supplier.setPhoneSupplier(rs.getString("phone"));
            supplier.setEmailSupplier(rs.getString("email"));
            supplier.setCreated_at(rs.getTimestamp("created_at"));
            supplier.setUpdated_at(rs.getTimestamp("updated_at"));
            return supplier;
        }, "Failed to load suppliers");
    }

    private void openSupplierDetails(Supplier supplier) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(
                    "/com/dfms/dairy_farm_management_system/popups/supplier_details.fxml"));
            Scene scene = new Scene(loader.load());
            SupplierDetailsController controller = loader.getController();
            controller.fetchSupplier(supplier);
            showStage("Supplier Details", scene);
        } catch (IOException e) {
            logAndAlert("Failed to open supplier details", e);
        }
    }

    private void openEditSupplier(Supplier supplier) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(
                    "/com/dfms/dairy_farm_management_system/popups/add_new_supplier.fxml"));
            Scene scene = new Scene(loader.load());
            NewSupplierController controller = loader.getController();
            controller.setUpdate(true);
            controller.fetchSupplier(supplier);
            showStage("Update Supplier", scene);
        } catch (IOException e) {
            logAndAlert("Failed to open edit supplier", e);
        }
    }

    // ========================= GENERIC DB FETCH =========================
    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private <T> ObservableList<T> fetchList(String query, RowMapper<T> mapper, String errorMsg) {
        ObservableList<T> out = FXCollections.observableArrayList();

        try (Connection connection = DBConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) out.add(mapper.map(resultSet));
        } catch (SQLException e) {
            logAndAlert(errorMsg, e);
        }

        return out;
    }

    // ========================= GENERIC ACTION CELL =========================
    private <T> Callback<TableColumn<T, String>, TableCell<T, String>> createActionCellFactory(
            TableView<T> table,
            Runnable refreshAfter,
            Consumer<T> onView,
            Consumer<T> onEdit,
            Consumer<T> onDelete,
            String confirmTitle,
            String confirmHeader,
            String infoTitle,
            String infoHeader
    ) {
        return (TableColumn<T, String> param) -> new TableCell<>() {
            private final Image imgEdit = loadIcon("/images/edit.png");
            private final Image imgDelete = loadIcon("/images/delete.png");
            private final Image imgView = loadIcon("/images/eye.png");

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                ImageView ivEdit = iconView(imgEdit);
                ImageView ivDelete = iconView(imgDelete);
                ImageView ivView = iconView(imgView);

                HBox manage = buildActionBox(ivEdit, ivDelete, ivView);

                ivDelete.setOnMouseClicked(e -> {
                    T rowItem = getCurrentRowItem(table);
                    if (rowItem == null) return;

                    if (confirmDelete(confirmTitle, confirmHeader)) {
                        try {
                            onDelete.accept(rowItem);
                            refreshAfter.run();
                            info(infoTitle, infoHeader);
                        } catch (Exception ex) {
                            logAndAlert("Delete failed", ex);
                        }
                    }
                });

                ivView.setOnMouseClicked(e -> {
                    T rowItem = getCurrentRowItem(table);
                    if (rowItem == null) return;
                    onView.accept(rowItem);
                });

                ivEdit.setOnMouseClicked(e -> {
                    T rowItem = getCurrentRowItem(table);
                    if (rowItem == null) return;
                    onEdit.accept(rowItem);
                });

                setGraphic(manage);
                setText(null);
            }
        };
    }

    // ========================= SEARCH (GENERIC) =========================
    private <T> void setupLiveSearch(TextField searchField,
                                     TableView<T> table,
                                     ObservableList<T> baseList,
                                     Function<T, String> textExtractor) {

        FilteredList<T> filtered = new FilteredList<>(baseList, p -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = (newVal == null) ? "" : newVal.trim().toLowerCase();
            filtered.setPredicate(item -> {
                if (filter.isEmpty()) return true;
                String text = textExtractor.apply(item);
                return text != null && text.toLowerCase().contains(filter);
            });
        });

        SortedList<T> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
    }

    @FXML
    void search_client(MouseEvent event) { /* already live */ }

    @FXML
    void search_supplier(MouseEvent event) { /* already live */ }

    // ========================= EXPORTS =========================
    private void exportToPDF(String typeList, String query) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(null);
        if (file == null) return;

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            Font font = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
            Paragraph paragraph = new Paragraph(typeList, font);
            paragraph.setAlignment(Element.ALIGN_CENTER);
            document.add(paragraph);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            addPdfHeader(table, "ID", "Name", "Type", "Phone", "Email");

            try (Connection connection = DBConfig.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(query)) {
                while (rs.next()) {
                    table.addCell(rs.getString("id"));
                    table.addCell(rs.getString("name"));
                    table.addCell(rs.getString("type"));
                    table.addCell(rs.getString("phone"));
                    table.addCell(rs.getString("email"));
                }
            }

            document.add(table);
            displayAlert("Success", typeList + " exported successfully", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            logAndAlert("Export PDF failed", e);
        } finally {
            if (document.isOpen()) document.close();
        }
    }

    private void addPdfHeader(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }
    }

    private void exportToExcel(String sheetName, String query, String typeList) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"),
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = fileChooser.showSaveDialog(null);
        if (file == null) return;

        try (Workbook workbook = new XSSFWorkbook();
             Connection connection = DBConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query);
             FileOutputStream out = new FileOutputStream(file)) {

            Sheet sheet = workbook.createSheet(sheetName);

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Name");
            header.createCell(2).setCellValue("Type");
            header.createCell(3).setCellValue("Phone");
            header.createCell(4).setCellValue("Email");

            int rowNum = 1;
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rs.getString("id"));
                row.createCell(1).setCellValue(rs.getString("name"));
                row.createCell(2).setCellValue(rs.getString("type"));
                row.createCell(3).setCellValue(rs.getString("phone"));
                row.createCell(4).setCellValue(rs.getString("email"));
            }

            workbook.write(out);
            displayAlert("Success", typeList + " exported successfully", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            logAndAlert("Export Excel failed", e);
        }
    }

    // ========================= SMALL HELPERS =========================
    private Image loadIcon(String path) {
        try {
            return new Image(getClass().getResourceAsStream(path));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Missing icon: " + path, e);
            return null;
        }
    }

    private ImageView iconView(Image img) {
        ImageView iv = new ImageView(img);
        iv.setStyle(ICON_STYLE);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setCache(true);
        return iv;
    }

    private HBox buildActionBox(ImageView... icons) {
        HBox box = new HBox(icons);
        box.setStyle("-fx-alignment:center");
        for (ImageView iv : icons) {
            HBox.setMargin(iv, new Insets(1, 1, 0, 3));
        }
        return box;
    }

    private boolean confirmDelete(String title, String header) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void info(String title, String header) {
        Alert alertInfo = new Alert(Alert.AlertType.INFORMATION);
        alertInfo.setTitle(title);
        alertInfo.setHeaderText(header);
        alertInfo.showAndWait();
    }

    private void showStage(String title, Scene scene) {
        Stage stage = new Stage();
        stage.getIcons().add(new Image(APP_ICON_PATH));
        stage.setTitle(title);
        stage.setResizable(false);
        stage.setScene(scene);
        centerScreen(stage);
        stage.show();
    }

    private <T> T getCurrentRowItem(TableView<T> table) {
        return table.getSelectionModel().getSelectedItem();
    }

    private void logAndAlert(String msg, Exception e) {
        LOGGER.log(Level.SEVERE, msg, e);
        displayAlert(ERROR_TITLE, msg, Alert.AlertType.ERROR);
    }
}
