package com.example.lms.controller;

import com.example.lms.connection.DBConnection;
import com.example.lms.model.StudentProgress;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;

public class StatisticsController {

    @FXML private ComboBox<String> courseFilter;
    @FXML private ComboBox<Integer> yearFilter;
    @FXML private ComboBox<Integer> semesterFilter;

    @FXML private TableView<StudentProgress> progressTable;
    @FXML private TableColumn<StudentProgress, String> studentCol;
    @FXML private TableColumn<StudentProgress, String> courseCol;
    @FXML private TableColumn<StudentProgress, Integer> yearCol;
    @FXML private TableColumn<StudentProgress, Integer> semesterCol;
    @FXML private TableColumn<StudentProgress, Double> gradeCol;

    @FXML private ListView<String> chatListView;
    @FXML private TextField chatInput;
    @FXML private ComboBox<String> recipientDropdown;
    @FXML private ListView<String> notificationsListView;

    private String loggedInName;
    private String loggedInRole;

    private final ObservableList<StudentProgress> progressList = FXCollections.observableArrayList();
    private final ObservableList<String> chatMessages = FXCollections.observableArrayList();
    private final ObservableList<String> notifications = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        studentCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        courseCol.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));
        semesterCol.setCellValueFactory(new PropertyValueFactory<>("semester"));
        gradeCol.setCellValueFactory(new PropertyValueFactory<>("averageGrade"));
        progressTable.setItems(progressList);

        chatListView.setItems(chatMessages);
        notificationsListView.setItems(notifications);

        loadFilters();
        loadProgressData();
        loadChatMessages();
        loadNotifications();
        markNotificationsAsRead();
    }

    private void loadFilters() {
        try (Connection conn = DBConnection.getConnection()) {
            courseFilter.getItems().clear();
            yearFilter.getItems().clear();
            semesterFilter.getItems().clear();

            ResultSet rs = conn.createStatement().executeQuery("SELECT DISTINCT name FROM users.course");
            while (rs.next()) courseFilter.getItems().add(rs.getString("name"));

            rs = conn.createStatement().executeQuery("SELECT DISTINCT year_enrolled FROM users.student");
            while (rs.next()) yearFilter.getItems().add(rs.getInt("year_enrolled"));

            rs = conn.createStatement().executeQuery("SELECT DISTINCT current_semester FROM users.student");
            while (rs.next()) semesterFilter.getItems().add(rs.getInt("current_semester"));

            loadRecipients();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRecipients() {
        recipientDropdown.getItems().clear();
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT name FROM users.users WHERE name <> ?");
            stmt.setString(1, loggedInName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) recipientDropdown.getItems().add(rs.getString("name"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void applyFilters() {
        if (courseFilter.getValue() == null || yearFilter.getValue() == null || semesterFilter.getValue() == null) return;
        loadProgressData();
        loadChatMessages();
        loadRecipients();
    }

    private void loadProgressData() {
        progressList.clear();

        StringBuilder query = new StringBuilder(
                "SELECT MIN(u.name) AS student_name, MIN(c.name) AS course_name, " +
                        "MIN(s.year_enrolled) AS year_enrolled, MIN(s.current_semester) AS current_semester, " +
                        "a.id AS assignment_id, SUM(CAST(sub.grade AS FLOAT)) AS total_grade " +
                        "FROM users.student_module sm " +
                        "JOIN users.assignments a ON a.module_id = sm.module_id " +
                        "LEFT JOIN users.submissions sub ON sub.assignment_id = a.id AND sub.student_id = sm.student_id " +
                        "JOIN users.users u ON sm.student_id = u.id " +
                        "JOIN users.student s ON sm.student_id = s.id " +
                        "JOIN users.course c ON a.course_id = c.id WHERE 1=1 ");

        if (courseFilter.getValue() != null) query.append("AND c.name = ? ");
        if (yearFilter.getValue() != null) query.append("AND s.year_enrolled = ? ");
        if (semesterFilter.getValue() != null) query.append("AND s.current_semester = ? ");
        query.append("GROUP BY sm.student_id, a.id ORDER BY sm.student_id, a.id LIMIT 20");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {

            int idx = 1;
            if (courseFilter.getValue() != null) stmt.setString(idx++, courseFilter.getValue());
            if (yearFilter.getValue() != null) stmt.setInt(idx++, yearFilter.getValue());
            if (semesterFilter.getValue() != null) stmt.setInt(idx++, semesterFilter.getValue());

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                progressList.add(new StudentProgress(
                        rs.getString("student_name"),
                        rs.getString("course_name"),
                        rs.getInt("year_enrolled"),
                        rs.getInt("current_semester"),
                        rs.getDouble("total_grade")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadChatMessages() {
        chatMessages.clear();
        if (courseFilter.getValue() == null || yearFilter.getValue() == null || semesterFilter.getValue() == null)
            return;

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT sender_name, message, timestamp, recipient_name " +
                            "FROM users.class_chat " +
                            "WHERE course_id = (SELECT id FROM users.course WHERE name = ?) " +
                            "AND year = ? AND semester = ? " +
                            "AND (recipient_name IS NULL OR sender_name = ? OR recipient_name = ?) " +
                            "ORDER BY timestamp"
            );
            stmt.setString(1, courseFilter.getValue());
            stmt.setInt(2, yearFilter.getValue());
            stmt.setInt(3, semesterFilter.getValue());
            stmt.setString(4, loggedInName);
            stmt.setString(5, loggedInName);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String sender = rs.getString("sender_name");
                String text = rs.getString("message");
                Timestamp timestamp = rs.getTimestamp("timestamp");
                String recipient = rs.getString("recipient_name");

                String formatted = (recipient != null)
                        ? String.format("[%s] (Private) %s: %s", timestamp, sender, text)
                        : String.format("[%s] %s: %s", timestamp, sender, text);
                chatMessages.add(formatted);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void sendMessage() {
        String message = chatInput.getText().trim();
        if (message.isEmpty() || courseFilter.getValue() == null || yearFilter.getValue() == null || semesterFilter.getValue() == null)
            return;

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO users.class_chat (course_id, year, semester, sender_name, recipient_name, message) " +
                            "VALUES ((SELECT id FROM users.course WHERE name = ?), ?, ?, ?, ?, ?)"
            );
            stmt.setString(1, courseFilter.getValue());
            stmt.setInt(2, yearFilter.getValue());
            stmt.setInt(3, semesterFilter.getValue());
            stmt.setString(4, loggedInName);
            stmt.setString(5, recipientDropdown.getValue()); // null for public
            stmt.setString(6, message);
            stmt.executeUpdate();

            chatInput.clear();
            recipientDropdown.setValue(null);
            loadChatMessages(); // auto-refresh chat

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void loadNotifications() {
        notifications.clear();
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT n.message, n.status, n.created_at " +
                            "FROM users.notifications n " +
                            "JOIN users.submissions s ON n.assignment_id = s.assignment_id " +
                            "JOIN users.student st ON s.student_id = st.id " +
                            "JOIN users.users u ON st.id = u.id " +
                            "WHERE u.name = ? ORDER BY n.created_at DESC"
            );
            stmt.setString(1, loggedInName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String formatted = String.format("[%s] %s - %s",
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getString("message"),
                        rs.getString("status"));
                notifications.add(formatted);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void markNotificationsAsRead() {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE users.notifications SET status = 'read' " +
                            "WHERE assignment_id IN (SELECT s.assignment_id FROM users.submissions s " +
                            "JOIN users.student st ON s.student_id = st.id " +
                            "JOIN users.users u ON st.id = u.id WHERE u.name = ?)"
            );
            stmt.setString(1, loggedInName);
            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setUserContext(String name, String role) {
        this.loggedInName = name;
        this.loggedInRole = role;
    }
}
