package com.dfms.dairy_farm_management_system.controllers;

import com.dfms.dairy_farm_management_system.Main;
import com.dfms.dairy_farm_management_system.connection.DBConfig;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.*;
import com.dfms.dairy_farm_management_system.models.AnimalSale;
import com.dfms.dairy_farm_management_system.models.MilkSale;
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
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.dfms.dairy_farm_management_system.connection.DBConfig.disconnect;
import static com.dfms.dairy_farm_management_system.connection.DBConfig.getConnection;
import static com.dfms.dairy_farm_management_system.helpers.Helper.*;

public class SalesController implements Initializable {

    private static final String SUCCESS_TITLE = "Success";
    private static final String ERROR_TITLE = "Error";

    private static final String SAVE_AS_TITLE = "Save As";
    private static final String PRICE_LABEL = "Price";
    private static final String CLIENT_LABEL = "Client";
    private static final String COLUMN_SALE_DATE = "sale_date";
    private static final String COLUMN_QUANTITY = "quantity";

    private static final String APP_LOGO_PATH = "file:src/main/resources/images/logo.png";
    private static final String ICON_STYLE =
            "-fx-background-color: transparent;" +
                    "-fx-cursor: hand;" +
                    "-fx-size:15px;";

    @FXML
    private TableView<MilkSale> MilkSaleTable;

    @FXML
    private TableColumn<MilkSale, String> action_c;

    @FXML
    private TableColumn<MilkSale, String> client_c;

    @FXML
    private ComboBox<String> combo1;

    @FXML
    private TableColumn<MilkSale, LocalDate> date_c;

    @FXML
    private TableColumn<MilkSale, Float> price_c;

    @FXML
    private TableColumn<MilkSale, Float> quantity_c;

    @FXML
    private TextField search_inpu;

    @FXML
    private ComboBox<String> combo;

    private final ObservableList<String> exportTypes = FXCollections.observableArrayList("PDF", "Excel");

    @FXML
    private TableColumn<AnimalSale, String> animalis_col;

    @FXML
    private TableColumn<AnimalSale, String> client_col;

    @FXML
    private TableColumn<AnimalSale, String> action_col;

    @FXML
    private TableView<AnimalSale> AnimalSalesTable;

    @FXML
    private TableColumn<AnimalSale, Date> operationdate_col;

    @FXML
    private TableColumn<AnimalSale, Float> price_col;

    @FXML
    private TextField search_input;

    private PreparedStatement statement = null;
    private ResultSet resultSet = null;

    private final Connection connection = getConnection();

    private final Image editImg = new Image(getClass().getResourceAsStream("/images/edit.png"));
    private final Image deleteImg = new Image(getClass().getResourceAsStream("/images/delete.png"));
    private final Image viewImg = new Image(getClass().getResourceAsStream("/images/eye.png"));

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        BasicConfigurator.configure();

        combo.setItems(exportTypes);
        combo1.setItems(exportTypes);

        setupExportCombo(combo, this::exportToPDF, this::exportToExcel);
        setupExportCombo(combo1, this::exportToPDF2, this::exportToExcel2);

        configureAnimalSalesTable();
        configureMilkSalesTable();

        wireLiveSearch(search_input, this::refreshTableAnimalSales, this::getAnimalSale,
                (a, q) -> safeContains(a.getClientName(), q) || safeContains(a.getAnimalId(), q),
                AnimalSalesTable);

        wireLiveSearch(search_inpu, this::refreshTableMilkSales, this::getMilkSale,
                (m, q) -> safeContains(m.getClientName(), q),
                MilkSaleTable);

        try {
            refreshTableAnimalSales();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        refreshTableMilkSales();
    }

    private void setupExportCombo(ComboBox<String> target, Runnable pdfAction, Runnable excelAction) {
        target.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            if (t1 == null) return;
            if ("PDF".equals(t1)) {
                pdfAction.run();
            } else {
                excelAction.run();
            }
        });
    }

    @FXML
    public void openAddNewAnimalSale(MouseEvent mouseEvent) throws IOException {
        openNewWindow("Add New Sale", "add_new_cow_sale");
    }

    @FXML
    public void openAddNewMilkSale(MouseEvent mouseEvent) throws IOException {
        openNewWindow("Add New Sale", "add_new_milk_sale");
    }

    public ObservableList<AnimalSale> getAnimalSale() throws SQLException, ClassNotFoundException {
        ObservableList<AnimalSale> list = FXCollections.observableArrayList();

        String query = "SELECT * FROM `animals_sales`";
        statement = DBConfig.getConnection().prepareStatement(query);
        resultSet = statement.executeQuery();

        while (resultSet.next()) {
            AnimalSale animalSale = new AnimalSale();
            animalSale.setId(resultSet.getInt("id"));
            animalSale.setClientId(resultSet.getInt("client_id"));
            animalSale.setAnimalId(resultSet.getString("animal_id"));
            animalSale.setPrice(resultSet.getFloat(PRICE_LABEL));
            animalSale.setSale_date(resultSet.getDate(COLUMN_SALE_DATE));
            animalSale.setCreated_at(resultSet.getTimestamp("created_at"));
            animalSale.setUpdated_at(resultSet.getTimestamp("updated_at"));
            list.add(animalSale);
        }

        disconnect();
        return list;
    }

    public ObservableList<MilkSale> getMilkSale() throws SQLException {
        ObservableList<MilkSale> list = FXCollections.observableArrayList();

        String selectQuery = "SELECT * FROM milk_sales";
        statement = DBConfig.getConnection().prepareStatement(selectQuery);
        resultSet = statement.executeQuery();

        while (resultSet.next()) {
            MilkSale milkSale = new MilkSale();
            milkSale.setId(resultSet.getInt("id"));
            milkSale.setPrice(resultSet.getFloat(PRICE_LABEL));
            milkSale.setQuantity(resultSet.getFloat(COLUMN_QUANTITY));
            milkSale.setClientId(resultSet.getInt("client_id"));
            milkSale.setSale_date(resultSet.getDate(COLUMN_SALE_DATE));
            list.add(milkSale);
        }

        disconnect();
        return list;
    }

    public void refreshTableAnimalSales() throws SQLException {
        AnimalSalesTable.getItems().clear();
        try {
            AnimalSalesTable.setItems(getAnimalSale());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void refreshTableMilkSales() {
        MilkSaleTable.getItems().clear();
        try {
            MilkSaleTable.setItems(getMilkSale());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void configureAnimalSalesTable() {
        animalis_col.setCellValueFactory(new PropertyValueFactory<>("animalId"));
        price_col.setCellValueFactory(new PropertyValueFactory<>(PRICE_LABEL));
        client_col.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        operationdate_col.setCellValueFactory(new PropertyValueFactory<>(COLUMN_SALE_DATE));

        action_col.setCellFactory(col -> new TableCell<AnimalSale, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                ImageView ivView = createIcon(viewImg);
                ImageView ivEdit = createIcon(editImg);
                ImageView ivDelete = createIcon(deleteImg);

                setGraphic(createManageButtons(ivView, ivEdit, ivDelete));
                setText(null);

                ivDelete.setOnMouseClicked(event -> {
                    AnimalSale selected = AnimalSalesTable.getSelectionModel().getSelectedItem();
                    if (selected == null) return;

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Delete Confirmation");
                    alert.setHeaderText("Are you sure you want to delete this animal sale?");

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isEmpty() || result.get() != ButtonType.OK) return;

                    try {
                        if (selected.delete()) {
                            displayAlert(SUCCESS_TITLE, "Animal Sale deleted successfully", Alert.AlertType.INFORMATION);
                            refreshTableAnimalSales();
                            return;
                        }
                        displayAlert(ERROR_TITLE, "Error while deleting!!!", Alert.AlertType.ERROR);
                    } catch (Exception e) {
                        displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
                    }
                });

                ivEdit.setOnMouseClicked(event -> {
                    AnimalSale selected = AnimalSalesTable.getSelectionModel().getSelectedItem();
                    if (selected == null) return;

                    openPopup(
                            "/com/dfms/dairy_farm_management_system/popups/add_new_cow_sale.fxml",
                            "Update Animal Sale",
                            controller -> {
                                CowSalesController cowSalesController = (CowSalesController) controller;
                                cowSalesController.setUpdate(true);
                                cowSalesController.fetchAnimalSale(selected);
                            }
                    );
                });

                ivView.setOnMouseClicked(event -> {
                    AnimalSale selected = AnimalSalesTable.getSelectionModel().getSelectedItem();
                    if (selected == null) return;

                    openPopup(
                            "/com/dfms/dairy_farm_management_system/popups/animal_sale_details.fxml",
                            "Animal Sale Details",
                            controller -> {
                                AnimalSaleDetailsController c = (AnimalSaleDetailsController) controller;
                                c.fetchAnimalSale(
                                        selected.getId(),
                                        selected.getAnimalId(),
                                        selected.getPrice(),
                                        selected.getClientName(),
                                        selected.getSale_date()
                                );
                            }
                    );
                });
            }
        });
    }

    private void configureMilkSalesTable() {
        quantity_c.setCellValueFactory(new PropertyValueFactory<>(COLUMN_QUANTITY));
        price_c.setCellValueFactory(new PropertyValueFactory<>(PRICE_LABEL));
        client_c.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        date_c.setCellValueFactory(new PropertyValueFactory<>(COLUMN_SALE_DATE));

        Callback<TableColumn<MilkSale, String>, TableCell<MilkSale, String>> cellFactory =
                (TableColumn<MilkSale, String> param) -> new TableCell<MilkSale, String>() {

                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty) {
                            setGraphic(null);
                            setText(null);
                            return;
                        }

                        ImageView ivView = createIcon(viewImg);
                        ImageView ivEdit = createIcon(editImg);
                        ImageView ivDelete = createIcon(deleteImg);

                        setGraphic(createManageButtons(ivView, ivEdit, ivDelete));
                        setText(null);

                        ivDelete.setOnMouseClicked(event -> {
                            MilkSale selected = MilkSaleTable.getSelectionModel().getSelectedItem();
                            if (selected == null) return;

                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle("Delete Confirmation");
                            alert.setHeaderText("Are you sure you want to delete this cow sale?");

                            Optional<ButtonType> result = alert.showAndWait();
                            if (result.isEmpty() || result.get() != ButtonType.OK) return;

                            try {
                                if (selected.delete()) {
                                    displayAlert(SUCCESS_TITLE, "Milk Sale deleted successfully", Alert.AlertType.INFORMATION);
                                    refreshTableMilkSales();
                                    return;
                                }
                                displayAlert(ERROR_TITLE, "Error while deleting!!!", Alert.AlertType.ERROR);
                            } catch (Exception e) {
                                displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
                            }
                        });

                        ivEdit.setOnMouseClicked(event -> {
                            MilkSale milkSale = MilkSaleTable.getSelectionModel().getSelectedItem();
                            if (milkSale == null) return;

                            openPopup(
                                    "/com/dfms/dairy_farm_management_system/popups/add_new_milk_sale.fxml",
                                    "Update Milk Sale",
                                    controller -> {
                                        MilkSalesController milkSalesController = (MilkSalesController) controller;
                                        milkSalesController.setUpdate(true);
                                        milkSalesController.fetchMilkSale(milkSale);
                                    }
                            );
                        });

                        ivView.setOnMouseClicked(event -> {
                            MilkSale milkSale = MilkSaleTable.getSelectionModel().getSelectedItem();
                            if (milkSale == null) return;

                            openPopup(
                                    "/com/dfms/dairy_farm_management_system/popups/milk_sale_details.fxml",
                                    "Animal Sale Details",
                                    controller -> {
                                        MilkSaleDetailsController c = (MilkSaleDetailsController) controller;
                                        c.fetchMilkSale(
                                                milkSale.getId(),
                                                milkSale.getQuantity(),
                                                milkSale.getPrice(),
                                                milkSale.getClientName(),
                                                milkSale.getSale_date()
                                        );
                                    }
                            );
                        });
                    }
                };

        action_c.setCellFactory(cellFactory);
    }

    private ImageView createIcon(Image image) {
        ImageView iv = new ImageView(image);
        iv.setStyle(ICON_STYLE);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setCache(true);
        return iv;
    }

    private HBox createManageButtons(ImageView... icons) {
        HBox managebtn = new HBox(icons);
        managebtn.setStyle("-fx-alignment:center");
        for (ImageView icon : icons) {
            HBox.setMargin(icon, new Insets(1, 1, 0, 3));
        }
        return managebtn;
    }

    private void openPopup(String fxmlPath, String title, Consumer<Object> initController) {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource(fxmlPath));
        try {
            Scene scene = new Scene(fxmlLoader.load());

            Object controller = fxmlLoader.getController();
            if (initController != null) {
                initController.accept(controller);
            }

            Stage stage = new Stage();
            stage.getIcons().add(new Image(APP_LOGO_PATH));
            stage.setTitle(title);
            stage.setResizable(false);
            stage.setScene(scene);
            centerScreen(stage);
            stage.show();
        } catch (IOException e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private interface ListSupplier<T> {
        ObservableList<T> get() throws Exception;
    }

    private interface MatchPredicate<T> {
        boolean match(T item, String query);
    }

    private <T> void wireLiveSearch(
            TextField field,
            ThrowingRunnable refreshAction,
            ListSupplier<T> supplier,
            MatchPredicate<T> matcher,
            TableView<T> table
    ) {
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            String q = newValue == null ? "" : newValue.trim();
            if (q.isEmpty()) {
                try {
                    refreshAction.run();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return;
            }
            ObservableList<T> filtered = FXCollections.observableArrayList();
            try {
                for (T item : supplier.get()) {
                    if (matcher.match(item, q)) {
                        filtered.add(item);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            table.setItems(filtered);
        });
    }

    boolean safeContains(String text, String query) {
        return safeLower(text).contains(safeLower(query));
    }

    String safeLower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    private File chooseSaveFile(String title, FileChooser.ExtensionFilter... filters) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().addAll(filters);
        return fileChooser.showSaveDialog(null);
    }

    private void exportExcelCommon(String sheetName, String query, String[] headers, BiConsumer<Row, ResultSet> writeRow) {
        File file = chooseSaveFile(
                SAVE_AS_TITLE,
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"),
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        if (file == null) return;

        try (
                Workbook workbook = new XSSFWorkbook();
                FileOutputStream out = new FileOutputStream(file);
                Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(query)
        ) {
            Sheet sheet = workbook.createSheet(sheetName);

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            int rowNum = 1;
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                writeRow.accept(row, rs);
            }

            workbook.write(out);
            displayAlert(SUCCESS_TITLE, sheetName + " exported successfully", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void exportPdfCommon(String titleText, String descText, String[] headers, Consumer<PdfPTable> fillRows) {
        File file = chooseSaveFile(SAVE_AS_TITLE, new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        if (file == null) return;

        try {
            Document document = new Document();
            document.setPageSize(PageSize.A4.rotate());
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            Paragraph title = new Paragraph(
                    titleText,
                    FontFactory.getFont(FontFactory.COURIER_BOLD, 20, BaseColor.BLACK)
            );
            Paragraph text = new Paragraph(
                    descText,
                    FontFactory.getFont(FontFactory.COURIER, 14, BaseColor.BLACK)
            );

            title.setAlignment(Element.ALIGN_CENTER);
            text.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30);
            text.setSpacingAfter(30);

            document.add(title);
            document.add(text);

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            table.setSpacingBefore(11f);
            table.setSpacingAfter(11f);

            float[] widths = new float[headers.length];
            for (int i = 0; i < headers.length; i++) widths[i] = 2f;
            table.setWidths(widths);

            for (String h : headers) {
                addPdfHeaderCell(table, h);
            }

            table.getDefaultCell().setPadding(3);
            table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
            table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);

            fillRows.accept(table);

            document.add(table);
            document.close();

            displayAlert(SUCCESS_TITLE, titleText + " exported successfully", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void addPdfHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(
                text,
                FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)
        ));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addPdfCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text));
        cell.setPadding(5);
        table.addCell(cell);
    }

    void exportToExcel() {
        String query =
                "SELECT ms.id AS sale_id, " +
                        "       ms.animal_id AS animal_id, " +
                        "       ms.price AS price, " +
                        "       c.name AS client_name, " +
                        "       ms.sale_date AS sale_date " +
                        "FROM animals_sales ms " +
                        "JOIN clients c ON ms.client_id = c.id";

        String[] headers = {"Sale ID", "Animal ID", PRICE_LABEL, CLIENT_LABEL, "Date"};

        exportExcelCommon("Animal Sales", query, headers, (row, rs) -> {
            try {
                row.createCell(0).setCellValue(rs.getString("sale_id"));
                row.createCell(1).setCellValue(rs.getString("animal_id"));
                row.createCell(2).setCellValue(rs.getString(PRICE_LABEL));
                row.createCell(3).setCellValue(rs.getString("client_name"));
                row.createCell(4).setCellValue(rs.getString(COLUMN_SALE_DATE));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    void exportToExcel2() {
        String query =
                "SELECT ms.id AS sale_id, " +
                        "       ms.quantity AS quantity, " +
                        "       ms.price AS price, " +
                        "       c.name AS client_name, " +
                        "       ms.sale_date AS sale_date " +
                        "FROM milk_sales ms " +
                        "JOIN clients c ON ms.client_id = c.id";

        String[] headers = {"Sale ID", COLUMN_QUANTITY, PRICE_LABEL, CLIENT_LABEL, "Date"};

        exportExcelCommon("Milk Sales", query, headers, (row, rs) -> {
            try {
                row.createCell(0).setCellValue(rs.getString("sale_id"));
                row.createCell(1).setCellValue(rs.getString(COLUMN_QUANTITY));
                row.createCell(2).setCellValue(rs.getString(PRICE_LABEL));
                row.createCell(3).setCellValue(rs.getString("client_name"));
                row.createCell(4).setCellValue(rs.getString(COLUMN_SALE_DATE));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    void exportToPDF() {
        String[] headers = {"Animal ID", PRICE_LABEL, CLIENT_LABEL, "Sale's date"};

        exportPdfCommon(
                "Animal Sales List",
                "This is the list of the animal sales",
                headers,
                table -> {
                    ObservableList<AnimalSale> animalSales = AnimalSalesTable.getItems();
                    CowSalesController controller = new CowSalesController();

                    for (AnimalSale animalSale : animalSales) {
                        AnimalSale sale = controller.getSale(animalSale.getId());
                        addPdfCell(table, String.valueOf(sale.getAnimalId()));
                        addPdfCell(table, String.valueOf(sale.getPrice()));
                        addPdfCell(table, sale.getClientName());
                        addPdfCell(table, String.valueOf(sale.getSale_date()));
                    }
                }
        );
    }

    void exportToPDF2() {
        String[] headers = {COLUMN_QUANTITY, PRICE_LABEL, CLIENT_LABEL, "Sale's date"};

        exportPdfCommon(
                "Milk Sales List",
                "This is the list of the milk sales",
                headers,
                table -> {
                    ObservableList<MilkSale> milkSales = MilkSaleTable.getItems();
                    MilkSalesController controller = new MilkSalesController();

                    for (MilkSale milkSale : milkSales) {
                        MilkSale sale = controller.getSale(milkSale.getId());
                        addPdfCell(table, String.valueOf(sale.getQuantity()));
                        addPdfCell(table, String.valueOf(sale.getPrice()));
                        addPdfCell(table, sale.getClientName());
                        addPdfCell(table, String.valueOf(sale.getSale_date()));
                    }
                }
        );
    }

    @FXML
    void refreshTable(MouseEvent event) throws SQLException {
        refreshTableAnimalSales();
    }

    @FXML
    void refreshTable2(MouseEvent event) {
        refreshTableMilkSales();
    }
}