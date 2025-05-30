package com.example.lms.controller;

import com.example.lms.model.Course;
import com.example.lms.model.Module;
import com.example.lms.connection.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.sql.*;

public class ModuleController {

    @FXML private TableView<Module> moduleTable;
    @FXML private TableColumn<Module, Integer> idColumn;
    @FXML private TableColumn<Module, String> nameColumn;
    @FXML private TableColumn<Module, String> courseColumn;
    @FXML private TableColumn<Module, Integer> yearColumn;
    @FXML private TableColumn<Module, Integer> semesterColumn;
    @FXML private TableColumn<Module, Void>       actionColumn;

    private ObservableList<Module> moduleList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        loadModules();
        addActionButtonsToTable();
    }
    private void addActionButtonsToTable() {
        actionColumn.setCellFactory(param -> new TableCell<Module, Void>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final Button viewStudentsButton = new Button("Students");
            private final Button viewLecturerButton = new Button("Lecturer");

            private final HBox buttonBox = new HBox(5, editButton, deleteButton, viewStudentsButton, viewLecturerButton);

            {
                // Set actions for buttons
                editButton.setOnAction(event -> {
                    Module module = getTableView().getItems().get(getIndex());
                    handleEditModule(module);
                });

                deleteButton.setOnAction(event -> {
                    Module module = getTableView().getItems().get(getIndex());
                    handleDeleteModule(module);
                });

                viewStudentsButton.setOnAction(event -> {
                    Module module = getTableView().getItems().get(getIndex());
                    handleViewStudents(module);
                });

                viewLecturerButton.setOnAction(event -> {
                    Module module = getTableView().getItems().get(getIndex());
                    handleViewLecturer(module);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttonBox);
                }
            }
        });
    }
    private void handleDeleteModule(Module module) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Are you sure you want to delete module: " + module.getName() + "?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String deleteSQL = "DELETE FROM users.module WHERE id = ?";

                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {

                    pstmt.setInt(1, module.getId());
                    pstmt.executeUpdate();

                } catch (SQLException e) {
                    e.printStackTrace();
                }

                loadModules();
            }
        });
    }

    private void handleEditModule(Module module) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Module");

        Label nameLabel = new Label("Module Name:");
        TextField nameField = new TextField(module.getName());

        Label courseLabel = new Label("Course:");
        ComboBox<String> courseCombo = new ComboBox<>();

        Label yearLabel = new Label("Year:");
        Spinner<Integer> yearSpinner = new Spinner<>(1, 5, module.getYear());

        Label semesterLabel = new Label("Semester:");
        Spinner<Integer> semesterSpinner = new Spinner<>(1, 2, module.getSemester());

        ObservableList<String> courseNames = FXCollections.observableArrayList();
        ObservableList<Integer> courseIds = FXCollections.observableArrayList();

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM users.course ORDER BY name")) {

            while (rs.next()) {
                courseIds.add(rs.getInt("id"));
                courseNames.add(rs.getString("name"));
            }
            courseCombo.setItems(courseNames);

            int index = courseIds.indexOf(module.getCourseId());
            if (index >= 0) {
                courseCombo.getSelectionModel().select(index);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(courseLabel, 0, 1);
        grid.add(courseCombo, 1, 1);
        grid.add(yearLabel, 0, 2);
        grid.add(yearSpinner, 1, 2);
        grid.add(semesterLabel, 0, 3);
        grid.add(semesterSpinner, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                int selectedIndex = courseCombo.getSelectionModel().getSelectedIndex();
                int courseId = selectedIndex >= 0 ? courseIds.get(selectedIndex) : -1;
                int year = yearSpinner.getValue();
                int semester = semesterSpinner.getValue();

                if (name.isEmpty() || courseId == -1) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Please fill all fields.", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }

                String updateSQL = "UPDATE users.module SET name = ?, course_id = ?, year = ?, semester = ? WHERE id = ?";

                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {

                    pstmt.setString(1, name);
                    pstmt.setInt(2, courseId);
                    pstmt.setInt(3, year);
                    pstmt.setInt(4, semester);
                    pstmt.setInt(5, module.getId());
                    pstmt.executeUpdate();

                } catch (SQLException e) {
                    e.printStackTrace();
                }

                loadModules();
            }
        });
    }

    private void handleViewStudents(Module module) {
        String query = "SELECT s.name FROM users.student_module sm " +
                "JOIN users.student s ON sm.student_id = s.id " +
                "WHERE sm.module_id = ?";

        StringBuilder studentList = new StringBuilder();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, module.getId());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                studentList.append("- ").append(rs.getString("name")).append("\n");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Students Enrolled");
        alert.setHeaderText("Students enrolled in: " + module.getName());
        alert.setContentText(studentList.length() > 0 ? studentList.toString() : "No students enrolled.");
        alert.showAndWait();
    }

    private void handleViewLecturer(Module module) {
        String query = "SELECT lm.name " +
                "FROM users.course_lecturer l " +
                "JOIN users.lecturer lm ON l.lecturer_id = lm.id " +
                "WHERE l.lecturer_id = ?";

        String lecturerName = "No lecturer assigned.";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, module.getId());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                lecturerName = rs.getString("name");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Assigned Lecturer");
        alert.setHeaderText("Lecturer for: " + module.getName());
        alert.setContentText(lecturerName);
        alert.showAndWait();
    }


    private void loadModules() {
        moduleList.clear();
        String query =
                "SELECT " +
                        "m.id AS module_id, " +
                        "m.name AS module_name, " +
                        "m.year, " +
                        "m.semester, " +
                        "m.course_id, " +
                        "c.name AS course_name " +
                        "FROM users.module m " +
                        "INNER JOIN users.course c ON m.course_id = c.id " +
                        "ORDER BY m.id";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Module module = new Module(
                        rs.getInt("module_id"),
                        rs.getString("module_name"),
                        rs.getInt("year"),
                        rs.getInt("semester"),
                        rs.getInt("course_id"),
                        rs.getString("course_name")
                );
                moduleList.add(module);
            }

            idColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getId()).asObject());
            nameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName()));
            courseColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCourseName()));
            yearColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getYear()).asObject());
            semesterColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getSemester()).asObject());

            moduleTable.setItems(moduleList);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @FXML
    public void handleAddModule() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Module");

        Label nameLabel = new Label("Module Name:");
        TextField nameField = new TextField();

        Label courseLabel = new Label("Course:");
        ComboBox<String> courseCombo = new ComboBox<>();

        Label yearLabel = new Label("Year:");
        Spinner<Integer> yearSpinner = new Spinner<>(1, 5, 1);

        Label semesterLabel = new Label("Semester:");
        Spinner<Integer> semesterSpinner = new Spinner<>(1, 2, 1);

        // Fetch course names and IDs
        ObservableList<String> courseNames = FXCollections.observableArrayList();
        ObservableList<Integer> courseIds = FXCollections.observableArrayList();

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM users.course ORDER BY name")) {

            while (rs.next()) {
                courseIds.add(rs.getInt("id"));
                courseNames.add(rs.getString("name"));
            }
            courseCombo.setItems(courseNames);

        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(courseLabel, 0, 1);
        grid.add(courseCombo, 1, 1);
        grid.add(yearLabel, 0, 2);
        grid.add(yearSpinner, 1, 2);
        grid.add(semesterLabel, 0, 3);
        grid.add(semesterSpinner, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                int selectedIndex = courseCombo.getSelectionModel().getSelectedIndex();
                int courseId = selectedIndex >= 0 ? courseIds.get(selectedIndex) : -1;
                int year = yearSpinner.getValue();
                int semester = semesterSpinner.getValue();

                if (name.isEmpty() || courseId == -1) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Please fill all fields.", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }

                String insertSQL = "INSERT INTO users.module (name, course_id, year, semester) VALUES (?, ?, ?, ?)";

                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

                    pstmt.setString(1, name);
                    pstmt.setInt(2, courseId);
                    pstmt.setInt(3, year);
                    pstmt.setInt(4, semester);
                    pstmt.executeUpdate();

                } catch (SQLException e) {
                    e.printStackTrace();
                }

                loadModules(); // Refresh table
            }
        });
    }

}
