package com.dfms.dairy_farm_management_system.controllers.pop_ups_controllers;

import com.dfms.dairy_farm_management_system.Main;
import com.dfms.dairy_farm_management_system.models.Employee;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.ResourceBundle;

import static com.dfms.dairy_farm_management_system.connection.DBConfig.getConnection;
import static com.dfms.dairy_farm_management_system.helpers.Helper.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EmployeeDetailsController implements Initializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmployeeDetailsController.class);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // No initialization required for this controller
    }

    @FXML
    private Button print_btn;
    @FXML
    private Label header;
    @FXML
    private Label address;

    @FXML
    private Label cin;

    @FXML
    private Label contract_type;

    @FXML
    private Label email;

    @FXML
    private Label first_name;

    @FXML
    private Label gender;

    @FXML
    private Label last_name;

    @FXML
    private Label phone;

    @FXML
    private Label recruitment_date;

    @FXML
    private Label role;

    @FXML
    private Label salary;

    private Employee current_employee = null;

    public void fetchEmployee(Employee employee) {
        UpdateEmployeeController controller = new UpdateEmployeeController();
        current_employee = controller.getEmployee(employee.getCin());

        String sql = "SELECT * FROM employees WHERE cin = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, employee.getCin());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    header.setText(resultSet.getString("first_name") + " " + resultSet.getString("last_name"));
                    email.setText(resultSet.getString("email"));
                    first_name.setText(resultSet.getString("first_name"));
                    last_name.setText(resultSet.getString("last_name"));
                    salary.setText(String.valueOf(resultSet.getInt("salary")));
                    address.setText(resultSet.getString("address"));
                    cin.setText(resultSet.getString("cin"));
                    phone.setText(resultSet.getString("phone"));
                    contract_type.setText(resultSet.getString("contract_type"));
                    recruitment_date.setText(resultSet.getString("hire_date"));
                }
            }

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }


    public String getRole(int id) {
        String roleName = "";

        String sql = "SELECT name FROM roles WHERE id = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    roleName = rs.getString("name");
                }
            }

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }

        return roleName;
    }


    @FXML
    void printEmployeeDetails(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try {
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();
                addMetaData(document);
                addTitlePage(document);
                addContent(document);
                addImage(document);
                addOutro(document);
                document.close();
                displayAlert("Success", "Employee details saved successfully", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                e.printStackTrace();
                displayAlert("Error", "Error while saving employee details", Alert.AlertType.ERROR);
            }
        }
    }


    // iText allows to add metadata to the PDF which can be viewed in your Adobe
    // Reader
    // under File -> Properties
    private void addMetaData(Document document) {
        document.addTitle("Employee Details");
        document.addSubject("Print all employee details");
        document.addKeywords("employee, details, pdf");
        document.addAuthor("GRASS LAND DAIRY");
        document.addCreator("GRASS LAND DAIRY");
    }

    private void addTitlePage(Document document) throws DocumentException {
        Paragraph preface = new Paragraph();

        // We add one empty line
        addEmptyLine(preface, 1);

        // Lets write a big header
        Paragraph title = new Paragraph("EMPLOYEE INOFRMATION REPPORT", new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, BaseColor.BLUE));
        //center the title
        title.setAlignment(Element.ALIGN_CENTER);
        preface.add(title);

        addEmptyLine(preface, 1);

        String address = "GRASS LAND DAIRY\n" +
                "Souss massa, Taroudant\n" +
                "TEL: +212 20 1234567\n" +
                "EMAIL:grass.land.dairy@gmail.com\n" +
                "DATE: " + new Date().toString();

        preface.add(new Paragraph(address, new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK)));

        addEmptyLine(preface, 4);

        String intro = "Here is all the information about " + current_employee.getFirstName() + " " + current_employee.getLastName() + ":";
        preface.add(new Paragraph(intro, new Font(Font.FontFamily.HELVETICA, 14, Font.BOLDITALIC, BaseColor.BLACK)));

        addEmptyLine(preface, 1);

        document.add(preface);
    }

    private void addContent(Document document) throws DocumentException {
        int MAX_LENGTH = 22;
        Paragraph preface = new Paragraph();
        String full_name = formatString("Full Name:", MAX_LENGTH) + ": " + current_employee.getFirstName() + " " + current_employee.getLastName();
        String email = formatString("Email:", MAX_LENGTH) + ": " + current_employee.getEmail();
        String phone = formatString("Phone:", MAX_LENGTH) + ": " + current_employee.getPhone();
        String address = formatString("Address:", MAX_LENGTH) + ": " + current_employee.getAddress();
        String cin = formatString("CIN:", MAX_LENGTH) + ": " + current_employee.getCin();
        String contract_type = formatString("Contract Type:", MAX_LENGTH) + ": " + current_employee.getContractType();
        String hire_date = formatString("Hire Date:", MAX_LENGTH) + ": " + current_employee.getHireDate();
        String salary = formatString("Salary:", MAX_LENGTH) + ": " + current_employee.getSalary();

        preface.add(new Paragraph(full_name, new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK)));
        preface.add(new Paragraph(email, new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK)));
        preface.add(new Paragraph(phone, new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK)));
        preface.add(new Paragraph(address, new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK)));
        preface.add(new Paragraph(cin, new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK)));
        preface.add(new Paragraph(salary, new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK)));
        preface.add(new Paragraph(contract_type, new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK)));
        preface.add(new Paragraph(hire_date, new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK)));
        document.add(preface);
    }

    private void addImage(Document document) {
        Image image = null;
        try {
            image = Image.getInstance(Main.class.getResource("/images/logo.png"));
            image.scaleAbsolute(100, 100);

            //put the image at the end of the document
            image.setAbsolutePosition(450, 650);
            document.add(image);
        } catch (IOException | DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    private void addOutro(Document document) throws DocumentException {
        Paragraph preface = new Paragraph();
        addEmptyLine(preface, 1);
        String outro = "Thank you for using our application\n" +
                "For more information please contact us at grass.land.dairy@gmail.com";
        preface.add(new Paragraph(outro, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.BLACK)));
        addEmptyLine(preface, 1);
        document.add(preface);
    }

    private void addEmptyLine(Paragraph paragraph, int number) {
        for (int i = 0; i < number; i++) {
            paragraph.add(new Paragraph(" "));
        }
    }

    //print the employee details
    public void printEmployeeToConsole(Employee employee) {
        if (employee == null) {
            LOGGER.warn("printEmployeeToConsole called with null employee");
            return;
        }

        LOGGER.info("Employee Details");
        LOGGER.info("First Name: {}", employee.getFirstName());
        LOGGER.info("Last Name: {}", employee.getLastName());
        LOGGER.info("Email: {}", employee.getEmail());
        LOGGER.info("Phone: {}", employee.getPhone());
        LOGGER.info("Address: {}", employee.getAddress());
        LOGGER.info("CIN: {}", employee.getCin());
        LOGGER.info("Salary: {}", employee.getSalary());
        LOGGER.info("Recruitment Date: {}", employee.getHireDate());
        LOGGER.info("Contract Type: {}", employee.getContractType());
    }

}