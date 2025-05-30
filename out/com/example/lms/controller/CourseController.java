package com.example.lms.controller;

import com.example.lms.connection.DBConnection;
import com.example.lms.model.Course;
import com.example.lms.model.TeachingAssignment;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.*;
import java.time.LocalDateTime;

public class CourseController {

    @FXML private TableView<Course> courseTable;
    @FXML private TableColumn<Course, Integer>    idColumn;
    @FXML private TableColumn<Course, String>     nameColumn;
    @FXML private TableColumn<Course, String>     descColumn;
    @FXML private TableColumn<Course, Integer>    creditsColumn;
    @FXML private TableColumn<Course, Void>       actionColumn;


    private final ObservableList<Course> courses = FXCollections.observableArrayList();

    @FXML private TableView<TeachingAssignment> teachingTable;
    @FXML private TableColumn<TeachingAssignment, String> lecturerCol;
    @FXML private TableColumn<TeachingAssignment, String> courseCol;
    @FXML private TableColumn<TeachingAssignment, String> moduleCol;
    @FXML private TableColumn<TeachingAssignment, String> studentCol;
    @FXML private TableColumn<TeachingAssignment, String> studentModuleCol;

    private void loadTeachingAssignments() {
        ObservableList<TeachingAssignment> list = FXCollections.observableArrayList();
        String sql = "SELECT \n" +
                "    -- Lecturer full name\n" +
                "    lu.name || ' ' || lu.surname AS lecturer_name,\n" +
                "\n" +
                "    -- Course name\n" +
                "    c.name AS course_name,\n" +
                "\n" +
                "    -- Student full name\n" +
                "    su.name || ' ' || su.surname AS student_name,\n" +
                "\n" +
                "    -- Module name\n" +
                "    m.name AS module_name\n" +
                "\n" +
                "FROM \n" +
                "    users.lecturer_module_allocation lma\n" +
                "\n" +
                "-- Join to get lecturer name\n" +
                "JOIN users.users lu ON lu.id = lma.lecturer_id AND lu.role = 'Lecturer'\n" +
                "\n" +
                "-- Join to get course name\n" +
                "JOIN users.course c ON c.id = lma.course_id\n" +
                "\n" +
                "-- Join to student_module for matching modules\n" +
                "JOIN users.student_module sm ON sm.module_id = lma.module_id\n" +
                "\n" +
                "-- Join to get student name\n" +
                "JOIN users.users su ON su.id = sm.student_id AND su.role = 'Student'\n" +
                "\n" +
                "-- Join to get module name\n" +
                "JOIN users.module m ON m.id = sm.module_id\n" +
                "\n" +
                "ORDER BY \n" +
                "    lecturer_name, student_name, module_name;\n";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new TeachingAssignment(

                 rs.getString("lecturer_name"),
                 rs.getString("course_name"),

                 rs.getString("module_name"),
                        rs.getString("student_name"),
                 rs.getString("module_name")


                ));
            }

            teachingTable.setItems(list);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Unable to load teaching assignments.");
        }
    }


    @FXML
    public void initialize() {
        lecturerCol.setCellValueFactory(new PropertyValueFactory<>("lecturerName"));
        courseCol.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        studentCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        moduleCol.setCellValueFactory(new PropertyValueFactory<>("moduleName"));
        studentModuleCol.setCellValueFactory(new PropertyValueFactory<>("studentModule"));

        loadTeachingAssignments();

        idColumn     .setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn   .setCellValueFactory(new PropertyValueFactory<>("name"));
        descColumn   .setCellValueFactory(new PropertyValueFactory<>("description"));
        creditsColumn.setCellValueFactory(new PropertyValueFactory<>("credits"));

        // add Edit/Delete buttons in each row
        actionColumn.setCellFactory(col -> new TableCell<Course, Void>() {
            private final Button editBtn       = new Button("Edit");
            private final Button deleteBtn     = new Button("Delete");
            private final Button assignLectBtn = new Button("Assign Lecturer");
            private final Button assignModLectBtn = new Button("Assign Lecturer Modules");
            private final Button assignModBtn  = new Button("Assign Student Module");
            private final HBox pane = new HBox(5, editBtn, deleteBtn, assignLectBtn, assignModLectBtn, assignModBtn);

            {
                editBtn.setOnAction(e -> {
                    Course c = getTableView().getItems().get(getIndex());
                    showCourseDialog(c, true);
                });
                assignLectBtn.setOnAction(e -> {
                    Course c = getTableView().getItems().get(getIndex());
                    showAssignLecturerDialog(c);
                });

                assignModBtn.setOnAction(e -> {
                    Course c = getTableView().getItems().get(getIndex());
                    showAssignModuleDialog(c);
                });

                assignModLectBtn.setOnAction(e -> {
                    Course c = getTableView().getItems().get(getIndex());
                    showAssignLecturerModuleDialog(c);
                });

                deleteBtn.setOnAction(e -> {
                    Course c = getTableView().getItems().get(getIndex());
                    deleteCourse(c);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        loadCourses();
    }
    private void showAssignModuleDialog(Course course) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Assign Module to Student (Course: " + course.getName() + ")");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> studentDropdown = new ComboBox<>();
        ComboBox<String> moduleDropdown = new ComboBox<>();
        ObservableList<String> students = FXCollections.observableArrayList();
        ObservableList<String> modules  = FXCollections.observableArrayList();

        // Get students
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name, surname FROM users.users WHERE role='Student'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                students.add(rs.getInt("id") + " - " + rs.getString("name") + " " + rs.getString("surname"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to fetch students.");
            return;
        }

        // Get modules
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name FROM users.module WHERE course_id = ?");
        ) {
            ps.setInt(1, course.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    modules.add(rs.getInt("id") + " - " + rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to fetch modules.");
            return;
        }

        studentDropdown.setItems(students);
        moduleDropdown.setItems(modules);

        VBox box = new VBox(10,
                new Label("Select Student:"), studentDropdown,
                new Label("Select Module:"), moduleDropdown
        );
        dlg.getDialogPane().setContent(box);

        dlg.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                if (studentDropdown.getValue() == null || moduleDropdown.getValue() == null) {
                    showAlert("Validation", "Both selections are required.");
                    return null;
                }

                int studentId = Integer.parseInt(studentDropdown.getValue().split(" - ")[0]);
                int moduleId  = Integer.parseInt(moduleDropdown.getValue().split(" - ")[0]);

                assignModuleToStudent(studentId, moduleId);
            }
            return null;
        });

        dlg.showAndWait();
    }

    private void assignModuleToStudent(int studentId, int moduleId) {
        String sql = "INSERT INTO users.student_module(student_id, module_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, moduleId);
            ps.executeUpdate();
            showAlert("Success", "Module assigned to student.");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Could not assign module.");
        }
    }

    private void showAssignLecturerDialog(Course course) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Assign Lecturer to Course: " + course.getName());
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> lecturerDropdown = new ComboBox<>();
        ObservableList<String> lecturers = FXCollections.observableArrayList();

        // Populate lecturers
        String sql = "SELECT id, name, surname FROM users.users WHERE role='Lecturer'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String display = rs.getInt("id") + " - " + rs.getString("name") + " " + rs.getString("surname");
                lecturers.add(display);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Could not fetch lecturers.");
            return;
        }
        lecturerDropdown.setItems(lecturers);

        VBox box = new VBox(10, new Label("Select Lecturer:"), lecturerDropdown);
        dlg.getDialogPane().setContent(box);

        dlg.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String selected = lecturerDropdown.getValue();
                if (selected == null) {
                    showAlert("Validation", "Please select a lecturer.");
                    return null;
                }
                int lecturerId = Integer.parseInt(selected.split(" - ")[0]);
                assignLecturerToCourse(course.getId(), lecturerId);
            }
            return null;
        });

        dlg.showAndWait();
    }

    private void assignLecturerToCourse(int courseId, int lecturerId) {
        String sql = "INSERT INTO users.course_lecturer(course_id, lecturer_id) VALUES(?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ps.setInt(2, lecturerId);
            ps.executeUpdate();
            showAlert("Success", "Lecturer assigned to course.");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Could not assign lecturer.");
        }
    }

    private void showAssignLecturerModuleDialog(Course course) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Assign Modules to Lecturer (Course: " + course.getName() + ")");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> lecturerDropdown = new ComboBox<>();
        ObservableList<String> lecturers = FXCollections.observableArrayList();

        // Get lecturers
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name, surname FROM users.users WHERE role='Lecturer'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lecturers.add(rs.getInt("id") + " - " + rs.getString("name") + " " + rs.getString("surname"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to fetch lecturers.");
            return;
        }
        lecturerDropdown.setItems(lecturers);

        // Get modules for the course
        ListView<String> moduleListView = new ListView<>();
        moduleListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        ObservableList<String> modules = FXCollections.observableArrayList();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name FROM users.module WHERE course_id = ?")) {
            ps.setInt(1, course.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    modules.add(rs.getInt("id") + " - " + rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to fetch modules.");
            return;
        }
        moduleListView.setItems(modules);

        VBox box = new VBox(10,
                new Label("Select Lecturer:"), lecturerDropdown,
                new Label("Select Modules:"), moduleListView);
        dlg.getDialogPane().setContent(box);

        dlg.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String selectedLecturer = lecturerDropdown.getValue();
                if (selectedLecturer == null || moduleListView.getSelectionModel().getSelectedItems().isEmpty()) {
                    showAlert("Validation", "Please select a lecturer and at least one module.");
                    return null;
                }

                int lecturerId = Integer.parseInt(selectedLecturer.split(" - ")[0]);

                for (String moduleItem : moduleListView.getSelectionModel().getSelectedItems()) {
                    int moduleId = Integer.parseInt(moduleItem.split(" - ")[0]);
                    assignModuleToLecturer(course.getId(), moduleId, lecturerId);
                }
            }
            return null;
        });

        dlg.showAndWait();
    }


    private void assignModuleToLecturer(int courseId, int moduleId, int lecturerId) {
        String sql = "INSERT INTO users.lecturer_module_allocation (lecturer_id, module_id, course_id) VALUES (?, ?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lecturerId);
            ps.setInt(2, moduleId);
            ps.setInt(3, courseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to assign module to lecturer.");
        }
    }


    private void loadCourses() {
        courses.clear();
        String sql = "SELECT id, name, description, credits, created_at FROM users.course ORDER BY id";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {

            while (rs.next()) {
                courses.add(new Course(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getInt("credits"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error", "Could not load courses.");
        }
        courseTable.setItems(courses);
    }

    @FXML
    private void handleAddCourse() {
        showCourseDialog(null, false);
    }

    private void showCourseDialog(Course existing, boolean isEdit) {
        Dialog<Course> dlg = new Dialog<>();
        dlg.setTitle(isEdit ? "Edit Course" : "Add New Course");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);

        TextField nameF = new TextField();
        TextArea  descF = new TextArea();
        descF.setPrefRowCount(3);
        TextField creditF = new TextField();

        if (isEdit) {
            nameF .setText(existing.getName());
            descF .setText(existing.getDescription());
            creditF.setText(String.valueOf(existing.getCredits()));
        }

        form.add(new Label("Name:"),        0, 0); form.add(nameF,    1, 0);
        form.add(new Label("Description:"), 0, 1); form.add(descF,    1, 1);
        form.add(new Label("Credits:"),     0, 2); form.add(creditF,  1, 2);

        dlg.getDialogPane().setContent(form);

        dlg.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String name   = nameF.getText().trim();
                String desc   = descF.getText().trim();
                int    creds;
                try {
                    creds = Integer.parseInt(creditF.getText().trim());
                } catch (NumberFormatException e) {
                    showAlert("Validation", "Credits must be an integer.");
                    return null;
                }
                if (name.isEmpty()) {
                    showAlert("Validation", "Name is required.");
                    return null;
                }

                if (isEdit) {
                    updateCourse(existing.getId(), name, desc, creds);
                    existing.nameProperty()       .set(name);
                    existing.descriptionProperty().set(desc);
                    existing.creditsProperty()   .set(creds);
                    return existing;
                } else {
                    Course c = new Course(0, name, desc, creds, LocalDateTime.now());
                    insertCourse(c);
                    return c;
                }
            }
            return null;
        });

        dlg.showAndWait().ifPresent(c -> {
            if (!isEdit) courses.add(c);
            courseTable.refresh();
        });
    }

    private void insertCourse(Course c) {
        String sql = "INSERT INTO users.course(name,description,credits,created_at) VALUES(?,?,?,NOW())";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            p.setString(1, c.getName());
            p.setString(2, c.getDescription());
            p.setInt   (3, c.getCredits());
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                if (keys.next()) c.idProperty().set(keys.getInt(1));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error", "Failed to add course.");
        }
        loadCourses();
    }

    private void updateCourse(int id, String name, String desc, int creds) {
        String sql = "UPDATE users.course SET name=?,description=?,credits=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {

            p.setString(1, name);
            p.setString(2, desc);
            p.setInt   (3, creds);
            p.setInt   (4, id);
            p.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error", "Failed to update course.");
        }
        loadCourses();
    }

    private void deleteCourse(Course c) {
        String sql = "DELETE FROM users.course WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {

            p.setInt(1, c.getId());
            p.executeUpdate();
            courses.remove(c);
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error", "Failed to delete course.");
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
