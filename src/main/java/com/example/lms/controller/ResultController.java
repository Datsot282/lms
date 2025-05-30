package com.example.lms.controller;

import com.example.lms.model.Result;
import com.example.lms.connection.DBConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class ResultController {

    @FXML private TableView<Result> resultTable;
    @FXML private TableColumn<Result, Integer> idCol;
    @FXML private TableColumn<Result, String> studentCol;
    @FXML private TableColumn<Result, String> courseCol;
    @FXML private TableColumn<Result, String> gradeCol;
    @FXML private TableColumn<Result, String> termCol;
    @FXML private TableColumn<Result, String> assignmentsCol;
    @FXML private TableColumn<Result, String> dateCol;

    private ObservableList<Result> resultList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        studentCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStudentName()));
        courseCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCourseName()));
        gradeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFinalGrade()));
        termCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTerm()));
        assignmentsCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.join(", ", cellData.getValue().getAssignmentTitles())));
        dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPublishedAt()));

        loadResults();
    }

    private void loadResults() {
        resultList.clear();
        int idCounter = 1;

        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT " +
                    "s.student_name, s.grade, s.assignment_id, " +
                    "a.title, a.file_path, a.module_id, a.course_id, a.uploaded_at, " +
                    "m.name AS module_name, m.year AS term, " +
                    "c.name AS course_name " +
                    "FROM users.submissions s " +
                    "JOIN users.assignments a ON s.assignment_id = a.id " +
                    "JOIN users.module m ON a.module_id = m.id " +
                    "JOIN users.course c ON a.course_id = c.id " +
                    "ORDER BY s.student_name, a.module_id";

            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            class AssignmentKey {
                String studentName;
                int assignmentId;

                AssignmentKey(String studentName, int assignmentId) {
                    this.studentName = studentName;
                    this.assignmentId = assignmentId;
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    AssignmentKey that = (AssignmentKey) o;
                    return assignmentId == that.assignmentId &&
                            studentName.equals(that.studentName);
                }

                @Override
                public int hashCode() {
                    return studentName.hashCode() * 31 + assignmentId;
                }
            }

            Map<AssignmentKey, Integer> gradeMap = new HashMap<>();
            Map<AssignmentKey, Result> resultMap = new LinkedHashMap<>();

            while (rs.next()) {
                String studentName = rs.getString("student_name");
                String gradeStr = rs.getString("grade");
                int assignmentId = rs.getInt("assignment_id");
                String assignmentTitle = rs.getString("title");
                String filePath = rs.getString("file_path");
                String moduleName = rs.getString("module_name");
                String courseName = rs.getString("course_name");
                String term = rs.getString("term");
                String publishedAt = rs.getString("uploaded_at");

                AssignmentKey key = new AssignmentKey(studentName, assignmentId);

                // Parse grade
                int grade = 0;
                try {
                    grade = Integer.parseInt(gradeStr);
                } catch (NumberFormatException ignored) {}

                // Sum grades per student-assignment
                gradeMap.put(key, gradeMap.getOrDefault(key, 0) + grade);

                // Store metadata once
                if (!resultMap.containsKey(key)) {
                    String typePrefix = (filePath == null || filePath.trim().isEmpty()) ? "Quiz: " : "Assignment: ";
                    String fullTitle = typePrefix + assignmentTitle;

                    Result result = new Result(
                            idCounter++,
                            studentName,
                            courseName,
                            moduleName,
                            term,
                            publishedAt,
                            fullTitle
                    );

                    resultMap.put(key, result);
                }
            }

            // Finalize results
            for (Map.Entry<AssignmentKey, Result> entry : resultMap.entrySet()) {
                AssignmentKey key = entry.getKey();
                Result result = entry.getValue();
                int totalGrade = gradeMap.getOrDefault(key, 0);

                result.addGrade(result.assignment, String.valueOf(totalGrade));
                result.setFinalGrade(String.valueOf(totalGrade));  // <-- Directly assign total
                resultList.add(result);
            }

            resultTable.setItems(resultList);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Failed to load results: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddResult() {
        Result selected = resultTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select a student to add Midterm/Final Exam grade.");
            return;
        }

        ChoiceDialog<String> typeDialog = new ChoiceDialog<>("Midterm", "Midterm", "Final Exam");
        typeDialog.setTitle("Select Assessment Type");
        typeDialog.setHeaderText("Select the type of assessment to add:");
        typeDialog.setContentText("Type:");

        typeDialog.showAndWait().ifPresent(type -> {
            TextInputDialog gradeDialog = new TextInputDialog();
            gradeDialog.setTitle("Enter Grade");
            gradeDialog.setHeaderText("Enter " + type + " grade for " + selected.getStudentName());
            gradeDialog.setContentText("Grade:");

            gradeDialog.showAndWait().ifPresent(grade -> {
                try (Connection conn = DBConnection.getConnection()) {

                    // Step 1: Get student_id
                    int studentId = getStudentIdByName(conn, selected.getStudentName());
                    if (studentId == -1) throw new SQLException("Student not found.");

                    // Step 2: Get course_id from course name
                    int courseId = getCourseIdByName(conn, selected.getCourseName());

                    // Step 3: Parse term and use current year
                    int semester = Integer.parseInt(selected.getTerm());
                    int year = LocalDate.now().getYear();

                    // Step 4: Get or create the module
                    int moduleId = getOrCreateModule(conn, type, courseId, year, semester);

                    // Step 5: Get or create the assignment (now includes lecturer_id)
                    int assignmentId = getOrCreateAssignment(conn, type, courseId, moduleId);

                    // Step 6: Retrieve username from student_id
                    String username = null;
                    String usernameQuery = "SELECT name FROM users.student WHERE id = ?";
                    PreparedStatement unameStmt = conn.prepareStatement(usernameQuery);
                    unameStmt.setInt(1, studentId);
                    ResultSet unameRs = unameStmt.executeQuery();
                    if (unameRs.next()) {
                        username = unameRs.getString("name");
                    } else {
                        throw new SQLException("Username not found for student ID " + studentId);
                    }

                    // Step 7: Insert submission with username
                    String insert = "INSERT INTO users.submissions (student_id, assignment_id, grade, student_name) VALUES (?, ?, ?, ?)";
                    PreparedStatement stmt = conn.prepareStatement(insert);
                    stmt.setInt(1, studentId);
                    stmt.setInt(2, assignmentId);
                    stmt.setString(3, grade);
                    stmt.setString(4, username);
                    stmt.executeUpdate();

                    showAlert(Alert.AlertType.INFORMATION, type + " grade added.");
                    loadResults();

                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Database error: " + e.getMessage());
                }
            });
        });
    }



    private int getStudentIdByName(Connection conn, String name) throws SQLException {
        String sql = "SELECT id FROM users.student WHERE name = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, name);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) return rs.getInt("id");
        return -1;
    }

    private int getCourseIdByName(Connection conn, String courseName) throws SQLException {
        String sql = "SELECT id FROM users.course WHERE name = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, courseName);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) return rs.getInt("id");
        return -1;
    }

    private int getOrCreateModule(Connection conn, String name, int courseId, int year,  int semester) throws SQLException {
        String sql = "SELECT id FROM users.module WHERE name = ? AND course_id = ? AND year = ? AND semester = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, name);
        stmt.setInt(2, courseId);
        stmt.setInt(3, year);
        stmt.setInt(4, semester);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) return rs.getInt("id");

        // Insert if not found
        sql = "INSERT INTO users.module (name, course_id, year, semester) VALUES (?, ?, ?, ?) RETURNING id";
        stmt = conn.prepareStatement(sql);
        stmt.setString(1, name);
        stmt.setInt(2, courseId);
        stmt.setInt(3, year);
        stmt.setInt(4, semester);
        rs = stmt.executeQuery();
        if (rs.next()) return rs.getInt("id");
        throw new SQLException("Failed to create module");
    }


    private int getOrCreateAssignment(Connection conn, String title, int courseId, int moduleId) throws SQLException {
        String sql = "SELECT id FROM users.assignments WHERE title = ? AND course_id = ? AND module_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, title);
        stmt.setInt(2, courseId);
        stmt.setInt(3, moduleId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) return rs.getInt("id");

        // Get lecturer_id from course_lecturer
        int lecturerId = -1;
        sql = "SELECT lecturer_id FROM users.course_lecturer WHERE course_id = ?";
        stmt = conn.prepareStatement(sql);
        stmt.setInt(1, courseId);
        rs = stmt.executeQuery();
        if (rs.next()) {
            lecturerId = rs.getInt("lecturer_id");
        } else {
            throw new SQLException("No lecturer found for course_id: " + courseId);
        }

        // Insert new assignment with lecturer_id
        sql = "INSERT INTO users.assignments (title, course_id, module_id, lecturer_id) VALUES (?, ?, ?, ?) RETURNING id";
        stmt = conn.prepareStatement(sql);
        stmt.setString(1, title);
        stmt.setInt(2, courseId);
        stmt.setInt(3, moduleId);
        stmt.setInt(4, lecturerId);
        rs = stmt.executeQuery();
        if (rs.next()) return rs.getInt("id");

        throw new SQLException("Failed to create assignment");
    }


 /*   private int getFixedAssignmentId(String type) {
        switch (type) {
            case "Midterm":
                return 1000; // Change as needed
            case "Final Exam":
                return 1001;
            default:
                return -1;
        }
    }*/

    private int getModuleIdByName(Connection conn, String moduleName) throws SQLException {
        String query = "SELECT id FROM module WHERE name = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, moduleName);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) return rs.getInt("id");
        return -1;
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
