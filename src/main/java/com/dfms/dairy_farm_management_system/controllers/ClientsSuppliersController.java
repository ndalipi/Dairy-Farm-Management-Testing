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

import static com.dfms.dairy_farm_management_system.helpers.Helper.*;

public class ClientsSuppliersController implements Initializable {

    private static final String APP_ICON_PATH = "file:src/main/resources/images/logo.png";
    private static final String ICON_STYLE = "-fx-background-color: transparent;-fx-cursor: hand;-fx-size:15px;";
    private static final String ERROR_TITLE = "Error";

    private static final String CLIENTS_QUERY = "SELECT * FROM clients";
    private static final String SUPPLIERS_QUERY = "SELECT * FROM suppliers";

    private final ObservableList<String> exportFormats = FXCollections.observableArrayList("PDF", "Excel");

    // -------------------- CLIENT TABLE --------------------
    @FXML private TableView<Client> TableClient;
    @FXML private TableColumn<Client, Integer> colidClient;
    @FXML private TableColumn<Client, String> colnameClient;
    @FXML private TableColumn<Client, String> coltypeClient;
    @FXML private TableColumn<Client, String> colemailClient;
    @FXML private TableColumn<Client, String> colphoneClient;
    @FXML private TableColumn<Client, String> actionClient;
    @FXML private ComboBox<String> export_combo;
    @FXML private TextField search_input_client;

    // -------------------- SUPPLIER TABLE --------------------
    @FXML private TableView<Supplier> TableSupplier;
    @FXML private TableColumn<Supplier, Integer> colidSupplier;
    @FXML private TableColumn<Supplier, String> colnameSupplier;
    @FXML private TableColumn<Supplier, String> coltypeSupplier;
    @FXML private TableColumn<Supplier, String> colemailSupplier;
    @FXML private TableColumn<Supplier, String> colphoneSupplier;
    @FXML private TableColumn<Supplier, String> colactionSupplier;
    @FXML private ComboBox<String> export_combo_sup;
    @FXML private TextField search_input_supplier;

    private final ObservableList<Client> listClient = FXCollections.observableArrayList();
    private final ObservableList<Supplier> listSupplier = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        export_combo.setItems(exportFormats);
        export_combo_sup.setItems(exportFormats);

        configureClientTable();
        configureSupplierTable();

        refreshClients();
        refreshSuppliers();

        wireExport(export_combo, "Clients", "Clients List", CLIENTS_QUERY);
        wireExport(export_combo_sup, "Suppliers", "Suppliers List", SUPPLIERS_QUERY);
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

    // ========================= LOAD / REFRESH =========================

    private void refreshClients() {
        listClient.clear();
        listClient.addAll(fetchClients());
        TableClient.setItems(listClient);
    }

    private void refreshSuppliers() {
        listSupplier.clear();
        listSupplier.addAll(fetchSuppliers());
        TableSupplier.setItems(listSupplier);
    }

    public ObservableList<Client> fetchClients() {
        ObservableList<Client> out = FXCollections.observableArrayList();

        try (Connection connection = DBConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(CLIENTS_QUERY);
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
            e.printStackTrace();
        }

        return out;
    }

    public ObservableList<Supplier> fetchSuppliers() {
        ObservableList<Supplier> out = FXCollections.observableArrayList();

        try (Connection connection = DBConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(SUPPLIERS_QUERY);
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
            e.printStackTrace();
        }

        return out;
    }

    // Buttons used by UI
    public void refreshTableClient() { refreshClients(); }
    void refreshTableSupplier() { refreshSuppliers(); }

    @FXML
    public void refreshTable(MouseEvent mouseEvent) { refreshClients(); }

    @FXML
    void refreshTableSupplier(MouseEvent event) { refreshSuppliers(); }

    // ========================= TABLE CONFIG =========================

    private void configureClientTable() {
        colidClient.setCellValueFactory(new PropertyValueFactory<>("id"));
        colnameClient.setCellValueFactory(new PropertyValueFactory<>("name"));
        coltypeClient.setCellValueFactory(new PropertyValueFactory<>("type"));
        colemailClient.setCellValueFactory(new PropertyValueFactory<>("email"));
        colphoneClient.setCellValueFactory(new PropertyValueFactory<>("phone"));

        actionClient.setCellFactory(createClientActionCellFactory());
    }

    private void configureSupplierTable() {
        colidSupplier.setCellValueFactory(new PropertyValueFactory<>("id"));
        colnameSupplier.setCellValueFactory(new PropertyValueFactory<>("nameSupplier"));
        coltypeSupplier.setCellValueFactory(new PropertyValueFactory<>("typeSupplier"));
        colemailSupplier.setCellValueFactory(new PropertyValueFactory<>("emailSupplier"));
        colphoneSupplier.setCellValueFactory(new PropertyValueFactory<>("phoneSupplier"));

        colactionSupplier.setCellFactory(createSupplierActionCellFactory());
    }

    private Callback<TableColumn<Client, String>, TableCell<Client, String>> createClientActionCellFactory() {
        return param -> new TableCell<>() {
            private final Image imgEdit = new Image(getClass().getResourceAsStream("/images/edit.png"));
            private final Image imgDelete = new Image(getClass().getResourceAsStream("/images/delete.png"));
            private final Image imgView = new Image(getClass().getResourceAsStream("/images/eye.png"));

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                ImageView ivEdit = styledIcon(imgEdit);
                ImageView ivDelete = styledIcon(imgDelete);
                ImageView ivView = styledIcon(imgView);

                HBox manage = new HBox(ivEdit, ivDelete, ivView);
                manage.setStyle("-fx-alignment:center");
                HBox.setMargin(ivEdit, new Insets(1, 1, 0, 3));
                HBox.setMargin(ivDelete, new Insets(1, 1, 0, 3));
                HBox.setMargin(ivView, new Insets(1, 1, 0, 3));

                ivDelete.setOnMouseClicked(e -> {
                    Client client = getTableView().getItems().get(getIndex());
                    if (confirmDelete("Delete Confirmation", "Are you sure you want to delete this client?")) {
                        client.delete();
                        refreshClients();
                        info("Delete Client", "Client deleted successfully");
                    }
                });

                ivView.setOnMouseClicked(e -> {
                    Client client = getTableView().getItems().get(getIndex());
                    openClientDetails(client);
                });

                ivEdit.setOnMouseClicked(e -> {
                    Client client = getTableView().getItems().get(getIndex());
                    openEditClient(client);
                });

                setGraphic(manage);
                setText(null);
            }
        };
    }

    private Callback<TableColumn<Supplier, String>, TableCell<Supplier, String>> createSupplierActionCellFactory() {
        return param -> new TableCell<>() {
            private final Image imgEdit = new Image(getClass().getResourceAsStream("/images/edit.png"));
            private final Image imgDelete = new Image(getClass().getResourceAsStream("/images/delete.png"));
            private final Image imgView = new Image(getClass().getResourceAsStream("/images/eye.png"));

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                ImageView ivEdit = styledIcon(imgEdit);
                ImageView ivDelete = styledIcon(imgDelete);
                ImageView ivView = styledIcon(imgView);

                HBox manage = new HBox(ivEdit, ivDelete, ivView);
                manage.setStyle("-fx-alignment:center");
                HBox.setMargin(ivEdit, new Insets(1, 1, 0, 3));
                HBox.setMargin(ivDelete, new Insets(1, 1, 0, 3));
                HBox.setMargin(ivView, new Insets(1, 1, 0, 3));

                ivDelete.setOnMouseClicked(e -> {
                    Supplier supplier = getTableView().getItems().get(getIndex());
                    if (confirmDelete("Delete Confirmation", "Are you sure you want to delete this supplier?")) {
                        supplier.delete();
                        refreshSuppliers();
                        info("Delete Supplier", "Supplier deleted successfully");
                    }
                });

                ivView.setOnMouseClicked(e -> {
                    Supplier supplier = getTableView().getItems().get(getIndex());
                    openSupplierDetails(supplier);
                });

                ivEdit.setOnMouseClicked(e -> {
                    Supplier supplier = getTableView().getItems().get(getIndex());
                    openEditSupplier(supplier);
                });

                setGraphic(manage);
                setText(null);
            }
        };
    }

    private ImageView styledIcon(Image img) {
        ImageView iv = new ImageView(img);
        iv.setStyle(ICON_STYLE);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setCache(true);
        return iv;
    }

    private boolean confirmDelete(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void info(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(message);
        a.showAndWait();
    }

    private void openClientDetails(Client client) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(
                    "/com/dfms/dairy_farm_management_system/popups/client_details.fxml"));
            Scene scene = new Scene(loader.load());
            ClientDetailsController controller = loader.getController();
            controller.fetchClient(client);

            Stage stage = new Stage();
            stage.getIcons().add(new Image(APP_ICON_PATH));
            stage.setTitle("Client Details");
            stage.setResizable(false);
            stage.setScene(scene);
            centerScreen(stage);
            stage.show();
        } catch (IOException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void openSupplierDetails(Supplier supplier) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(
                    "/com/dfms/dairy_farm_management_system/popups/supplier_details.fxml"));
            Scene scene = new Scene(loader.load());
            SupplierDetailsController controller = loader.getController();
            controller.fetchSupplier(supplier);

            Stage stage = new Stage();
            stage.getIcons().add(new Image(APP_ICON_PATH));
            stage.setTitle("Supplier Details");
            stage.setResizable(false);
            stage.setScene(scene);
            centerScreen(stage);
            stage.show();
        } catch (IOException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
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

            Stage stage = new Stage();
            stage.getIcons().add(new Image(APP_ICON_PATH));
            stage.setTitle("Update Client");
            stage.setResizable(false);
            stage.setScene(scene);
            centerScreen(stage);
            stage.show();
        } catch (IOException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
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

            Stage stage = new Stage();
            stage.getIcons().add(new Image(APP_ICON_PATH));
            stage.setTitle("Update Supplier");
            stage.setResizable(false);
            stage.setScene(scene);
            centerScreen(stage);
            stage.show();
        } catch (IOException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    // ========================= LIVE SEARCH =========================

    @FXML
    void search_client(MouseEvent event) {
        FilteredList<Client> filtered = new FilteredList<>(listClient, p -> true);
        search_input_client.textProperty().addListener((obs, oldV, newV) -> {
            filtered.setPredicate(c -> {
                if (newV == null || newV.trim().isEmpty()) return true;
                return c.getName() != null && c.getName().toLowerCase().contains(newV.toLowerCase());
            });
        });
        SortedList<Client> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(TableClient.comparatorProperty());
        TableClient.setItems(sorted);
    }

    @FXML
    void search_supplier(MouseEvent event) {
        FilteredList<Supplier> filtered = new FilteredList<>(listSupplier, p -> true);
        search_input_supplier.textProperty().addListener((obs, oldV, newV) -> {
            filtered.setPredicate(s -> {
                if (newV == null || newV.trim().isEmpty()) return true;
                return s.getNameSupplier() != null && s.getNameSupplier().toLowerCase().contains(newV.toLowerCase());
            });
        });
        SortedList<Supplier> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(TableSupplier.comparatorProperty());
        TableSupplier.setItems(sorted);
    }

    // ========================= EXPORT (SHARED) =========================

    private void wireExport(ComboBox<String> combo, String sheetName, String title, String query) {
        combo.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            if ("PDF".equals(newV)) {
                exportToPDF(title, query);
            } else {
                exportToExcel(sheetName, query, title);
            }
            combo.getSelectionModel().clearSelection();
        });
    }

    private void exportToPDF(String title, String query) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save As");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = chooser.showSaveDialog(null);
        if (file == null) return;

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            Font font = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
            Paragraph paragraph = new Paragraph(title, font);
            paragraph.setAlignment(Element.ALIGN_CENTER);
            document.add(paragraph);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            addPdfHeader(table);

            try (Connection connection = DBConfig.getConnection();
                 PreparedStatement ps = connection.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    table.addCell(rs.getString("id"));
                    table.addCell(rs.getString("name"));
                    table.addCell(rs.getString("type"));
                    table.addCell(rs.getString("phone"));
                    table.addCell(rs.getString("email"));
                }
            }

            document.add(table);
            displayAlert("Success", title + " exported successfully", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        } finally {
            if (document.isOpen()) document.close();
        }
    }

    private void addPdfHeader(PdfPTable table) {
        PdfPCell c1 = new PdfPCell(new Phrase("ID"));
        c1.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase("Name"));
        c2.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase("Type"));
        c3.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase("Phone"));
        c4.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(c4);

        PdfPCell c5 = new PdfPCell(new Phrase("Email"));
        c5.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(c5);
    }

    private void exportToExcel(String sheetName, String query, String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save As");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"),
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = chooser.showSaveDialog(null);
        if (file == null) return;

        try (Workbook workbook = new XSSFWorkbook();
             Connection connection = DBConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery();
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
            displayAlert("Success", title + " exported successfully", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}
