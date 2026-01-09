package com.dfms.dairy_farm_management_system.controllers;

import com.dfms.dairy_farm_management_system.Main;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.UpdateEmployeeController;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.UpdateUserController;
import com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers.UserDetailsController;
import com.dfms.dairy_farm_management_system.models.Employee;
import com.dfms.dairy_farm_management_system.models.User;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.dfms.dairy_farm_management_system.connection.DBConfig.getConnection;
import static com.dfms.dairy_farm_management_system.helpers.Helper.*;

public class UsersController implements Initializable {

    private static final String ICON_STYLE = "-fx-background-color: transparent;-fx-cursor: hand;-fx-size:15px;";
    private static final String ERROR_TITLE = "Error";
    private static final int COLUMNS_COUNT = 9;

    private static final String APP_ICON_PATH = "file:src/main/resources/images/logo.png";
    private static final String UPDATE_USER_FXML = "/com/dfms/dairy_farm_management_system/popups/update_user.fxml";
    private static final String USER_DETAILS_FXML = "/com/dfms/dairy_farm_management_system/popups/user_details.fxml";

    private static final Image EDIT_IMG = new Image(UsersController.class.getResourceAsStream("/images/edit.png"));
    private static final Image DELETE_IMG = new Image(UsersController.class.getResourceAsStream("/images/delete.png"));
    private static final Image VIEW_IMG = new Image(UsersController.class.getResourceAsStream("/images/eye.png"));

    private PreparedStatement preparedStatement;
    private final Connection connection = getConnection();

    @FXML private TableView<User> users_table;
    @FXML private TableColumn<User, String> actions_col;
    @FXML private TableColumn<User, String> col_id;
    @FXML private TableColumn<User, String> email_col;
    @FXML private TableColumn<User, String> first_name_col;
    @FXML private TableColumn<User, String> last_name_col;
    @FXML private TableColumn<User, String> role_col;
    @FXML private Button search_btn;
    @FXML private TextField search_user_input;
    @FXML private ComboBox<String> export_combo;
    @FXML private Button openAddNewEmployeeBtn;
    @FXML private Button new_role_btn;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Important for export
        BasicConfigurator.configure();

        ObservableList<String> list = FXCollections.observableArrayList("PDF", "Excel");
        export_combo.setItems(list);

        displayUsers();
        liveSearch(search_user_input, users_table);

        export_combo.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            if (t1 == null) return;
            if ("PDF".equals(t1)) exportToPDF();
            else exportToExcel();
        });
    }

    // ========================= DATA =========================
    public ObservableList<User> getUsers() {
        ObservableList<User> list = FXCollections.observableArrayList();
        String query = "SELECT id, first_name, last_name, cin, email, gender, phone, salary, address, role, created_at FROM `users`";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                User user = new User();
                user.setId(resultSet.getInt("id"));
                user.setFirstName(resultSet.getString("first_name"));
                user.setLastName(resultSet.getString("last_name"));
                user.setCin(resultSet.getString("cin"));
                user.setEmail(resultSet.getString("email"));
                user.setGender(resultSet.getString("gender"));
                user.setPhone(resultSet.getString("phone"));
                user.setSalary(resultSet.getInt("salary"));
                user.setAdress(resultSet.getString("address"));
                user.setRole(resultSet.getInt("role"));
                user.setCreatedAt(resultSet.getTimestamp("created_at"));
                list.add(user);
            }
        } catch (Exception e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }

        return list;
    }

    // ========================= TABLE =========================
    public void displayUsers() {
        ObservableList<User> users = getUsers();

        col_id.setCellValueFactory(new PropertyValueFactory<>("id"));
        first_name_col.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        last_name_col.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        email_col.setCellValueFactory(new PropertyValueFactory<>("email"));
        role_col.setCellValueFactory(new PropertyValueFactory<>("roleName"));

        actions_col.setCellFactory(createActionsFactory(users));
        users_table.setItems(users);
    }

    private Callback<TableColumn<User, String>, TableCell<User, String>> createActionsFactory(ObservableList<User> users) {
        return (TableColumn<User, String> param) -> new TableCell<>() {

            private final Button edit_btn = iconButton(EDIT_IMG);
            private final Button delete_btn = iconButton(DELETE_IMG);
            private final Button view_details_btn = iconButton(VIEW_IMG);

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                HBox managebtn = new HBox(view_details_btn, edit_btn, delete_btn);
                managebtn.setStyle("-fx-alignment:center");
                setMargins(managebtn);

                setGraphic(managebtn);
                setText(null);

                delete_btn.setOnMouseClicked((MouseEvent event) -> {
                    User user = users.get(getRowIndex(event));
                    if (confirmDelete("Delete user", "Are you sure you want to delete this user?")) {
                        try {
                            user.delete();
                            displayUsers();
                        } catch (Exception e) {
                            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
                        }
                    }
                });

                edit_btn.setOnMouseClicked((MouseEvent event) -> {
                    User user = users.get(getRowIndex(event));
                    openPopupWithUser(UPDATE_USER_FXML, "Update Employee", user, (UpdateUserController c) -> c.initData(user));
                });

                view_details_btn.setOnMouseClicked((MouseEvent event) -> {
                    User user = users.get(getRowIndex(event));
                    openPopupWithUser(USER_DETAILS_FXML, "Employee Details", user, (UserDetailsController c) -> c.initData(user));
                });
            }
        };
    }

    private Button iconButton(Image img) {
        ImageView iv = new ImageView(img);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setCache(true);

        Button btn = new Button();
        btn.setGraphic(iv);
        btn.setStyle(ICON_STYLE);
        return btn;
    }

    private void setMargins(HBox managebtn) {
        for (javafx.scene.Node n : managebtn.getChildren()) {
            HBox.setMargin(n, new Insets(1, 1, 0, 3));
        }
    }

    private boolean confirmDelete(String title, String header) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private <T> void openPopupWithUser(String fxmlPath, String title, User user, ControllerInit<T> initializer) {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource(fxmlPath));
        try {
            Scene scene = new Scene(fxmlLoader.load());
            T controller = fxmlLoader.getController();
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
    private interface ControllerInit<T> {
        void init(T controller);
    }

    public void openAddUser() throws IOException {
        openNewWindow("Add user", "add_new_user");
    }

    @FXML
    public void refreshTable() {
        users_table.getItems().clear();
        displayUsers();
    }

    public void liveSearch(TextField search_input, TableView table) {
        search_input.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                refreshTable();
                return;
            }

            ObservableList<User> filteredList = FXCollections.observableArrayList();
            ObservableList<User> users = getUsers();

            String needle = newValue.toLowerCase();
            for (User user : users) {
                String fn = user.getFirstName() == null ? "" : user.getFirstName().toLowerCase();
                String ln = user.getLastName() == null ? "" : user.getLastName().toLowerCase();
                if (fn.contains(needle) || ln.contains(needle)) {
                    filteredList.add(user);
                }
            }
            table.setItems(filteredList);
        });
    }

    @FXML
    void searchUser(MouseEvent event) {
        liveSearch(this.search_user_input, users_table);
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

            Sheet sheet = workbook.createSheet("Employees");

            Row header = sheet.createRow(1);
            header.createCell(1).setCellValue("First Name");
            header.createCell(2).setCellValue("Last Name");
            header.createCell(3).setCellValue("Email");
            header.createCell(4).setCellValue("Phone");
            header.createCell(5).setCellValue("Address");
            header.createCell(6).setCellValue("CIN");
            header.createCell(7).setCellValue("Gender");
            header.createCell(8).setCellValue("Salary");

            ObservableList<User> users = users_table.getItems();
            UpdateEmployeeController controller = new UpdateEmployeeController();

            for (User user : users) {
                Employee emp = controller.getEmployee(user.getCin());
                if (emp == null) continue;

                Row row = sheet.createRow(sheet.getLastRowNum() + 1);
                row.createCell(1).setCellValue(emp.getFirstName());
                row.createCell(2).setCellValue(emp.getLastName());
                row.createCell(3).setCellValue(emp.getEmail());
                row.createCell(4).setCellValue(emp.getPhone());
                row.createCell(5).setCellValue(emp.getAddress());
                row.createCell(6).setCellValue(emp.getCin());
                row.createCell(7).setCellValue("M".equals(emp.getGender()) ? "Male" : "Female");
                row.createCell(8).setCellValue(String.valueOf(emp.getSalary()));
            }

            workbook.write(fileOutputStream);
            displayAlert("Success", "Employees exported successfully", Alert.AlertType.INFORMATION);

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

            Paragraph title = new Paragraph("Employees List",
                    FontFactory.getFont(FontFactory.COURIER_BOLD, 20, BaseColor.BLACK));
            Paragraph text = new Paragraph("This is the list of the users",
                    FontFactory.getFont(FontFactory.COURIER, 14, BaseColor.BLACK));

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

            addPdfHeader(table);

            ObservableList<User> users = users_table.getItems();
            UpdateEmployeeController controller = new UpdateEmployeeController();

            for (User user : users) {
                Employee emp = controller.getEmployee(user.getCin());
                if (emp == null) continue;

                table.addCell(new PdfPCell(new Paragraph(emp.getFirstName()))).setPadding(5);
                table.addCell(new PdfPCell(new Paragraph(emp.getLastName()))).setPadding(5);
                table.addCell(new PdfPCell(new Paragraph(emp.getEmail()))).setPadding(5);
                table.addCell(new PdfPCell(new Paragraph(emp.getPhone()))).setPadding(5);
                table.addCell(new PdfPCell(new Paragraph(emp.getAddress()))).setPadding(5);
                table.addCell(new PdfPCell(new Paragraph(emp.getCin()))).setPadding(5);
                table.addCell(new PdfPCell(new Paragraph("M".equals(emp.getGender()) ? "Male" : "Female"))).setPadding(5);
                table.addCell(new PdfPCell(new Paragraph(emp.getHireDate() == null ? "" : emp.getHireDate().toString()))).setPadding(5);
                table.addCell(new PdfPCell(new Paragraph(String.valueOf(emp.getSalary())))).setPadding(5);
            }

            document.add(table);
            document.close();
            displayAlert("Success", "Employees exported successfully", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            displayAlert(ERROR_TITLE, e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void addPdfHeader(PdfPTable table) {
        table.addCell(new PdfPCell(new Paragraph("First Name", FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)))).setPadding(5);
        table.addCell(new PdfPCell(new Paragraph("Last Name", FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)))).setPadding(5);
        table.addCell(new PdfPCell(new Paragraph("Email", FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)))).setPadding(5);
        table.addCell(new PdfPCell(new Paragraph("Phone", FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)))).setPadding(5);
        table.addCell(new PdfPCell(new Paragraph("Address", FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)))).setPadding(5);
        table.addCell(new PdfPCell(new Paragraph("CIN", FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)))).setPadding(5);
        table.addCell(new PdfPCell(new Paragraph("Gender", FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)))).setPadding(5);
        table.addCell(new PdfPCell(new Paragraph("Hire Date", FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)))).setPadding(5);
        table.addCell(new PdfPCell(new Paragraph("Salary", FontFactory.getFont(FontFactory.COURIER_BOLD, 12, BaseColor.BLACK)))).setPadding(5);
    }

    @FXML
    void openNewRole(MouseEvent event) throws IOException {
        openNewWindow("Add new role", "add_new_role");
    }
}
