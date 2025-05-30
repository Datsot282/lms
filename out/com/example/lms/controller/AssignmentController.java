package com.example.lms.controller;

import com.example.lms.connection.DBConnection;
import com.example.lms.model.Assignment;
import com.example.lms.model.ModuleInfo;
import com.example.lms.model.Submission;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AssignmentController {

    @FXML private TableView<Assignment> assignmentTable;
    @FXML private TableColumn<Assignment, String> titleCol;
    @FXML private TableColumn<Assignment, String> descCol;
    @FXML private TableColumn<Assignment, LocalDateTime> deadlineCol;
    @FXML private TableColumn<Assignment, String> uploadedCol;
    @FXML private TableColumn<Assignment, String> gradeCol;
    @FXML private TableColumn<Assignment, Void> actionCol;

    @FXML private TableView<Submission> submissionTable;
    @FXML private TableColumn<Submission, String> studentCol;
    @FXML private TableColumn<Submission, String> fileCol;
    @FXML private TableColumn<Submission, String> submittedCol;
    @FXML private TableColumn<Submission, String> gradeValCol;
    @FXML private Button uploadBtn;
    @FXML private Button submit1Btn;
    @FXML private Button gradeBtn;
    @FXML private final Button openBtn = new Button("Open");
    @FXML private final Button viewSubsBtn = new Button("Submissions");

    private final ObservableList<Assignment> assignmentList = FXCollections.observableArrayList();
    private final ObservableList<Submission> submissionList = FXCollections.observableArrayList();

    private String loggedInRole;
    private String loggedInName;

    @FXML
    public void initialize() {
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        deadlineCol.setCellValueFactory(new PropertyValueFactory<>("deadline"));
        uploadedCol.setCellValueFactory(new PropertyValueFactory<>("uploadedAt"));
        gradeCol.setCellValueFactory(cellData -> {
            String grade = cellData.getValue().getGrade();
            return new SimpleStringProperty(grade != null ? grade : "Not graded");
        });




        assignmentTable.setItems(assignmentList);
        assignmentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                loadSubmissionsForAssignment(newSel.getId());
            }
        });

        // Setup Submission Table
        studentCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        fileCol.setCellValueFactory(new PropertyValueFactory<>("filePath"));
        submittedCol.setCellValueFactory(new PropertyValueFactory<>("submittedAt"));
        gradeValCol.setCellValueFactory(new PropertyValueFactory<>("grade"));
        submissionTable.setItems(submissionList);

        addAssignmentActions();
        loadAssignments();
    }

    private void loadAssignments() {
        assignmentList.clear();
        String query = "SELECT * FROM users.assignments WHERE file_path IS NOT NULL AND file_path <> '' ORDER BY uploaded_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Timestamp deadlineTs = rs.getTimestamp("deadline");
                Timestamp uploadedAtTs = rs.getTimestamp("uploaded_at");

                assignmentList.add(new Assignment(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("file_path"),
                        (deadlineTs != null) ? deadlineTs.toLocalDateTime() : null,
                        (uploadedAtTs != null) ? uploadedAtTs.toLocalDateTime().toString() : "",
                        rs.getString("grade") // ✅ new field
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addAssignmentActions() {
        actionCol.setCellFactory(new Callback<TableColumn<Assignment, Void>, TableCell<Assignment, Void>>() {
            @Override
            public TableCell<Assignment, Void> call(final TableColumn<Assignment, Void> param) {
                return new TableCell<Assignment, Void>() {
                    private final Button openBtn = new Button("Open");
                    private final Button viewSubsBtn = new Button("Submissions");


                    {
                        openBtn.setOnAction(e -> {
                            Assignment assignment = getTableView().getItems().get(getIndex());
                            try {
                                Desktop.getDesktop().open(new File(assignment.getFilePath()));
                            } catch (IOException ex) {
                                showAlert(Alert.AlertType.ERROR, "Cannot open file.");
                            }

                        });

                        viewSubsBtn.setOnAction(e -> {
                            Assignment assignment = getTableView().getItems().get(getIndex());
                            loadSubmissionsForAssignment(assignment.getId());
                            submissionTable.setItems(submissionList);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox box = new HBox(5, openBtn, viewSubsBtn);
                            setGraphic(box);
                        }
                    }
                };
            }
        });
    }
    public void setUserContext(String name, String role) {
        this.loggedInName = name;
        this.loggedInRole = role;

        boolean isStudent = isStudent();

        uploadBtn.setDisable(isStudent);
        gradeBtn.setDisable(isStudent);
        openBtn.setDisable(isStudent);
        viewSubsBtn.setDisable(isStudent);
        //submit1Btn.setDisable(isStudent);
        //assignmentTable.setEditable(!isStudent);
        //assignmentTable.setDisable(isStudent);
        // Set column editability
        //descCol.setEditable(!isStudent);
        //deadlineCol.setEditable(!isStudent);
        //uploadedCol.setEditable(!isStudent);
        //gradeCol.setEditable(!isStudent);
    }

    private boolean isStudent() {
        return "student".equalsIgnoreCase(loggedInRole);
    }

    private boolean isLecturer() {
        return "lecturer".equalsIgnoreCase(loggedInRole);
    }
    @FXML
    private void handleUploadAssignment() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Assignment File");
        File selectedFile = fileChooser.showOpenDialog(new Stage());

        if (selectedFile == null) return;

        // Prompt for title and description
        String title = promptText("Enter Assignment Title");
        String desc = promptText("Enter Assignment Description");

        if (title == null || desc == null) return;

        try (Connection conn = DBConnection.getConnection()) {
            // Get lecturer ID
            int lecturerId = -1;
            String lecturerName = this.loggedInName;

            PreparedStatement idStmt = conn.prepareStatement(
                    "SELECT id FROM users.lecturer WHERE name = ?"
            );
            idStmt.setString(1, lecturerName);
            ResultSet idRs = idStmt.executeQuery();
            if (idRs.next()) {
                lecturerId = idRs.getInt("id");
            } else {
                showAlert(Alert.AlertType.ERROR, "Lecturer not found.");
                return;
            }

            // Fetch assigned modules
            String sql = "SELECT m.id AS module_id, m.name AS module_name, m.course_id, " +
                    "       c.name AS course_name " +
                    "FROM users.module m " +
                    "JOIN users.course_lecturer cl ON m.course_id = cl.course_id " +
                    "JOIN users.course c ON c.id = m.course_id " +
                    "WHERE cl.lecturer_id = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, lecturerId);
            ResultSet rs = stmt.executeQuery();

            List<ModuleInfo> modules = new ArrayList<>();
            while (rs.next()) {
                modules.add(new ModuleInfo(
                        rs.getInt("module_id"),
                        rs.getInt("course_id"),
                        rs.getString("module_name"),
                        rs.getString("course_name")
                ));
            }

            if (modules.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "No modules assigned to you.");
                return;
            }

            // Ask the user to choose a module
            ChoiceDialog<ModuleInfo> moduleDialog = new ChoiceDialog<>(modules.get(0), modules);
            moduleDialog.setTitle("Select Module");
            moduleDialog.setHeaderText("Choose the module to assign to:");
            Optional<ModuleInfo> result = moduleDialog.showAndWait();

            if (!result.isPresent()) return;
            ModuleInfo selected = result.get();

            // Prompt for assignment grade
            String gradeStr = promptText("Enter Assignment Grade (e.g., 100)");
            if (gradeStr == null || !gradeStr.matches("\\d+(\\.\\d+)?")) {
                showAlert(Alert.AlertType.ERROR, "Invalid grade. Please enter a numeric value.");
                return;
            }
            double grade = Double.parseDouble(gradeStr);

            // Ask for deadline using DatePicker + Time input in custom dialog
            Dialog<LocalDateTime> deadlineDialog = new Dialog<>();
            deadlineDialog.setTitle("Set Deadline");

            DatePicker datePicker = new DatePicker();
            TextField timeField = new TextField("12:00");

            VBox dialogContent = new VBox(10, new Label("Select Deadline Date:"), datePicker,
                    new Label("Enter Deadline Time (HH:mm):"), timeField);
            dialogContent.setPadding(new Insets(10));

            deadlineDialog.getDialogPane().setContent(dialogContent);
            deadlineDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            deadlineDialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    LocalDate date = datePicker.getValue();
                    String timeText = timeField.getText();
                    if (date != null && timeText.matches("\\d{2}:\\d{2}")) {
                        try {
                            return LocalDateTime.parse(date + "T" + timeText);
                        } catch (Exception e) {
                            return null;
                        }
                    }
                }
                return null;
            });

            Optional<LocalDateTime> deadlineResult = deadlineDialog.showAndWait();

            if (!deadlineResult.isPresent()) {
                showAlert(Alert.AlertType.ERROR, "Invalid or no deadline provided.");
                return;
            }

            LocalDateTime deadline = deadlineResult.get();

            // Insert assignment
            String insertSql =
                    "INSERT INTO users.assignments " +
                            "(title, description, file_path, deadline, lecturer_id, course_id, module_id, grade) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";


            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setString(1, title);
            insertStmt.setString(2, desc);
            insertStmt.setString(3, selectedFile.getAbsolutePath());
            insertStmt.setTimestamp(4, Timestamp.valueOf(deadline));
            insertStmt.setInt(5, lecturerId);
            insertStmt.setInt(6, selected.courseId);
            insertStmt.setInt(7, selected.moduleId);
            insertStmt.setDouble(8, grade);  // Grade inserted here
            insertStmt.executeUpdate();

            loadAssignments();
            showAlert(Alert.AlertType.INFORMATION, "Assignment uploaded successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Failed to upload assignment.");
        }
    }

    @FXML
    private void handleSubmitWork() {
        Assignment selected = assignmentTable.getSelectionModel().getSelectedItem();
        if (selected == null || LocalDateTime.now().isAfter(selected.getDeadline())) {
            showAlert(Alert.AlertType.WARNING, "Please select a valid assignment before deadline.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Your Work");
        File selectedFile = fileChooser.showOpenDialog(new Stage());

        if (selectedFile != null) {
            String studentName = loggedInName;
            if (studentName == null || studentName.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Logged-in user name is missing.");
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                // 1. Get student ID from student name
                int studentId = -1;
                PreparedStatement studentStmt = conn.prepareStatement(
                        "SELECT id FROM users.student WHERE name = ?"
                );
                studentStmt.setString(1, studentName);
                ResultSet studentRs = studentStmt.executeQuery();
                if (studentRs.next()) {
                    studentId = studentRs.getInt("id");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Student not found.");
                    return;
                }

                // 2. Check if assignment exists
                PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT 1 FROM users.assignments WHERE id = ?"
                );
                checkStmt.setInt(1, selected.getId());
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next()) {
                    showAlert(Alert.AlertType.ERROR, "Assignment ID not found in the database.");
                    return;
                }

                // 3. Proceed with submission, now including student_id
                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO users.submissions (assignment_id, student_id, student_name, file_path) VALUES (?, ?, ?, ?)"
                );
                stmt.setInt(1, selected.getId());
                stmt.setInt(2, studentId);  // <-- NEW
                stmt.setString(3, studentName);
                stmt.setString(4, selectedFile.getAbsolutePath());
                stmt.executeUpdate();

                loadSubmissionsForAssignment(selected.getId());
                showAlert(Alert.AlertType.INFORMATION, "Submission Successful");

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Database error: " + e.getMessage());
            }
        }
    }


    @FXML
    private void handleGradeSubmission() {
        Submission selected = submissionTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog gradeDialog = new TextInputDialog(selected.getGrade());
        gradeDialog.setTitle("Grade Submission");
        gradeDialog.setHeaderText("Grade for: " + selected.getStudentName());
        gradeDialog.setContentText("Enter grade:");

        gradeDialog.showAndWait().ifPresent(grade -> {
            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE users.submissions SET grade = ? WHERE id = ?"
                );
                stmt.setString(1, grade);
                stmt.setInt(2, selected.getId());
                stmt.executeUpdate();

                selected.setGrade(grade);
                submissionTable.refresh();
                showAlert(Alert.AlertType.INFORMATION, "Grade updated!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void loadSubmissionsForAssignment(int assignmentId) {
        submissionList.clear();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users.submissions WHERE assignment_id = ?")) {
            stmt.setInt(1, assignmentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                submissionList.add(new Submission(
                        rs.getInt("id"),
                        rs.getInt("assignment_id"),
                        rs.getString("student_name"),
                        rs.getString("file_path"),
                        rs.getTimestamp("submitted_at").toString(),
                        rs.getString("grade")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String promptText(String header) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText(header);
        return dialog.showAndWait().orElse(null);
    }

    private void showAlert(Alert.AlertType type, String content) {
        Alert alert = new Alert(type, content);
        alert.showAndWait();
    }
}
