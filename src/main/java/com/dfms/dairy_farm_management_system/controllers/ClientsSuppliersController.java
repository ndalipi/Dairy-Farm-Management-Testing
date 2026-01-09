package com.dfms.dairy_farm_management_system.controllers;

import com.dfms.dairy_farm_management_system.Main;
import com.dfms.dairy_farm_management_system.connection.DBConfig;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.ClientDetailsController;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.NewClientController;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.NewSupplierController;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.SupplierDetailsController;
import com.dfms.dairy_farm_management_system.models.Client;
import com.dfms.dairy_farm_management_system.models.Supplier;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
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
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dfms.dairy_farm_management_system.helpers.Helper.*;

public class ClientsSuppliersController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(ClientsSuppliersController.class.getName());

    private static final String APP_ICON_PATH = "file:src/main/resources/images/logo.png";
    private static final String ERROR_TITLE = "Error";
    private static final String ICON_STYLE = "-fx-background-color: transparent;-fx-cursor: hand;-fx-size:15px;";

    private static final ObservableList<String> EXPORT_OPTIONS =
            FXCollections.observableArrayList("PDF", "Excel");

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

    // Backing lists (kept fresh)
    private final ObservableList<Client> listClient = FXCollections.observableArrayList();
    private final ObservableList<Supplier> listSupplier = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        export_combo.setItems(EXPORT_OPTIONS);
        export_combo_sup.setItems(EXPORT_OPTIONS);

        setupClientTableColumns();
        setupSupplierTableColumns();

        refreshTableClient();
        refreshTableSupplier();

        // Export listeners
        export_combo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String query = "SELECT * FROM `clients`";
            if ("PDF".equals(newVal)) exportToPDF("Clients List", query);
            else exportToExcel("Clients", query, "Clients List");
        });

        export_combo_sup.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String query = "SELECT * FROM `suppliers`";
            if ("PDF".equals(newVal)) exportToPDF("Suppliers List", query);
            else exportToExcel("Suppliers", query, "Suppliers List");
        });

        // Live search hooks (no duplication)
        setupLiveSearch(search_input_client, TableClient, listClient, Client::getName);
        setupLiveSearch(search_input_supplier, TableSupplier, listSupplier, Supplier::getNameSupplier);
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

        actionClient.setCellFactory(createActionCellFactoryForClients());
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
        ObservableList<Client> out = FXCollections.observableArrayList();
        String selectQuery = "SELECT * FROM `clients`";

        try (Connection connection = DBConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectQuery);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Client client = new Client();
                client.setId(resultSet.getInt("id"));
                client.setName(resultSet.getString("name"));
                client.setType(resultSet.getString("type"));
                client.setPhone(resultSet.getString("phone"));
                client.setEmail(resultSet.getString("email"));
                client.setCreated_at(resultSet.getTimestamp("created_at"));
                client.setUpdated_at(resultSet.getTimestamp("updated_at"));
                out.add(client);
            }

        } catch (SQLException e) {
            logAndAlert("Failed to load clients", e);
        }

        return out;
    }

    private Callback<TableColumn<Client, String>, TableCell<Client, String>> createActionCellFactoryForClients() {
        return (TableColumn<Client, String> param) -> new TableCell<>() {
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
                    Client client = getCurrentRowItem(TableClient);
                    if (client == null) return;

                    if (confirmDelete("Delete Confirmation", "Are you sure you want to delete this client?")) {
                        client.delete();
                        refreshTableClient();
                        info("Delete Client", "Client deleted successfully");
                    }
                });

                ivView.setOnMouseClicked(e -> {
                    Client client = getCurrentRowItem(TableClient);
                    if (client == null) return;
                    openClientDetails(client);
                });

                ivEdit.setOnMouseClicked(e -> {
                    Client client = getCurrentRowItem(TableClient);
                    if (client == null) return;
                    openEditClient(client);
                });

                setGraphic(manage);
                setText(null);
            }
        };
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

        colactionSupplier.setCellFactory(createActionCellFactoryForSuppliers());
    }

    private void refreshTableSupplier() {
        listSupplier.setAll(fetchSuppliers());
        TableSupplier.setItems(listSupplier);
    }

    @FXML
    void refreshTableSupplier(MouseEvent event) {
        refreshTableSupplier();
    }

    private ObservableList<Supplier> fetchSuppliers() {
        ObservableList<Supplier> out = FXCollections.observableArrayList();
        String selectQuery = "SELECT * FROM `suppliers`";

        try (Connection connection = DBConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectQuery);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Supplier supplier = new Supplier();
                supplier.setId(resultSet.getInt("id"));
                supplier.setNameSupplier(resultSet.getString("name"));
                supplier.setTypeSupplier(resultSet.getString("type"));
                supplier.setPhoneSupplier(resultSet.getString("phone"));
                supplier.setEmailSupplier(resultSet.getString("email"));
                supplier.setCreated_at(resultSet.getTimestamp("created_at"));
                supplier.setUpdated_at(resultSet.getTimestamp("updated_at"));
                out.add(supplier);
            }

        } catch (SQLException e) {
            logAndAlert("Failed to load suppliers", e);
        }

        return out;
    }

    private Callback<TableColumn<Supplier, String>, TableCell<Supplier, String>> createActionCellFactoryForSuppliers() {
        return (TableColumn<Supplier, String> param) -> new TableCell<>() {
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
                    Supplier supplier = getCurrentRowItem(TableSupplier);
                    if (supplier == null) return;

                    if (confirmDelete("Delete Confirmation", "Are you sure you want to delete this supplier?")) {
                        supplier.delete();
                        refreshTableSupplier();
                        info("Delete Supplier", "Supplier deleted successfully");
                    }
                });

                ivView.setOnMouseClicked(e -> {
                    Supplier supplier = getCurrentRowItem(TableSupplier);
                    if (supplier == null) return;
                    openSupplierDetails(supplier);
                });

                ivEdit.setOnMouseClicked(e -> {
                    Supplier supplier = getCurrentRowItem(TableSupplier);
                    if (supplier == null) return;
                    openEditSupplier(supplier);
                });

                setGraphic(manage);
                setText(null);
            }
        };
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
    void search_client(MouseEvent event) {
        // live search is already wired in initialize()
    }

    @FXML
    void search_supplier(MouseEvent event) {
        // live search is already wired in initialize()
    }

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

    // ========================= SMALL HELPERS (REDUCE DUPLICATION) =========================
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
