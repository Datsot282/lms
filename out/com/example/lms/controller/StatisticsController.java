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

        private String loggedInRole;
        private String loggedInName;

        private ObservableList<StudentProgress> progressList = FXCollections.observableArrayList();
        private ObservableList<String> chatMessages = FXCollections.observableArrayList();

        @FXML
        public void initialize() {
            // Setup progress table
            studentCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
            courseCol.setCellValueFactory(new PropertyValueFactory<>("courseName"));
            yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));
            semesterCol.setCellValueFactory(new PropertyValueFactory<>("semester"));
            gradeCol.setCellValueFactory(new PropertyValueFactory<>("averageGrade"));
            progressTable.setItems(progressList);

            // Setup chat
            chatListView.setItems(chatMessages);

            loadFilters();
            loadProgressData();
            loadChatMessages();
        }

        private void loadRecipients() {
            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT name FROM users.users WHERE name <> ?");
                stmt.setString(1, loggedInName);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    recipientDropdown.getItems().add(rs.getString("name"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void loadFilters() {
            try (Connection conn = DBConnection.getConnection()) {
                // Courses
                PreparedStatement courseStmt = conn.prepareStatement("SELECT DISTINCT name FROM users.course");
                ResultSet courseRs = courseStmt.executeQuery();
                while (courseRs.next()) {
                    courseFilter.getItems().add(courseRs.getString("name"));
                }

                // Years
                PreparedStatement yearStmt = conn.prepareStatement("SELECT DISTINCT year_enrolled FROM users.student");
                ResultSet yearRs = yearStmt.executeQuery();
                while (yearRs.next()) {
                    yearFilter.getItems().add(yearRs.getInt("year_enrolled"));
                }

                // Semesters
                PreparedStatement semesterStmt = conn.prepareStatement("SELECT DISTINCT current_semester FROM users.student");
                ResultSet semesterRs = semesterStmt.executeQuery();
                while (semesterRs.next()) {
                    semesterFilter.getItems().add(semesterRs.getInt("current_semester"));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            loadRecipients();
        }

        @FXML
        private void applyFilters() {
            loadProgressData();
            loadChatMessages();
        }

        private void loadProgressData() {
            progressList.clear();

            StringBuilder query = new StringBuilder();
            query.append("SELECT ")
                    .append("  MIN(u.name) AS student_name, ")
                    .append("  MIN(c.name) AS course_name, ")
                    .append("  MIN(s.year_enrolled) AS year_enrolled, ")
                    .append("  MIN(s.current_semester) AS current_semester, ")
                    .append("  a.id AS assignment_id, ")
                    .append("  SUM(CAST(sub.grade AS FLOAT)) AS total_grade ")
                    .append("FROM users.student_module sm ")
                    .append("JOIN users.assignments a ON a.module_id = sm.module_id ")
                    .append("LEFT JOIN users.submissions sub ON sub.assignment_id = a.id ")
                    .append("JOIN users.users u ON sm.student_id = u.id ")
                    .append("JOIN users.student s ON sm.student_id = s.id ")
                    .append("JOIN users.course c ON a.course_id = c.id ")
                    .append("WHERE 1=1 ");


            // Optional filters
            if (courseFilter.getValue() != null) {
                query.append(" AND c.name = ? ");
            }
            if (yearFilter.getValue() != null) {
                query.append(" AND s.year_enrolled = ? ");
            }
            if (semesterFilter.getValue() != null) {
                query.append(" AND s.current_semester = ? ");
            }

            // Final grouping and ordering
            query.append(" GROUP BY sm.student_id, a.id ");
            query.append(" ORDER BY sm.student_id, a.id ");
            query.append(" LIMIT 20");

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query.toString())) {

                int paramIndex = 1;
                if (courseFilter.getValue() != null) {
                    stmt.setString(paramIndex++, courseFilter.getValue());
                }
                if (yearFilter.getValue() != null) {
                    stmt.setInt(paramIndex++, yearFilter.getValue());
                }
                if (semesterFilter.getValue() != null) {
                    stmt.setInt(paramIndex++, semesterFilter.getValue());
                }

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
            if (courseFilter.getValue() == null || yearFilter.getValue() == null || semesterFilter.getValue() == null) return;

            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT u.name, c.message, c.timestamp, c.recipient_id " +
                                "FROM users.class_chat c " +
                                "JOIN users.users u ON u.id = c.user_id " +
                                "WHERE c.course_id = (SELECT id FROM users.course WHERE name = ?) " +
                                "AND c.year = ? AND c.semester = ? " +
                                "AND (c.recipient_id IS NULL OR c.user_id = ? OR c.recipient_id = ?) " +
                                "ORDER BY c.timestamp"
                );
                stmt.setString(1, courseFilter.getValue());
                stmt.setInt(2, yearFilter.getValue());
                stmt.setInt(3, semesterFilter.getValue());
                int currentUserId = getCurrentUserId();
                stmt.setInt(4, currentUserId);
                stmt.setInt(5, currentUserId);


                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String sender = rs.getString("name");
                    String text = rs.getString("message");
                    Timestamp timestamp = rs.getTimestamp("timestamp");
                    int recipientId = rs.getInt("recipient_id");
                    boolean isPrivate = !rs.wasNull();

                    String formatted = isPrivate ?
                            String.format("[%s] (Private) %s: %s", timestamp, sender, text) :
                            String.format("[%s] %s: %s", timestamp, sender, text);

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
                        "INSERT INTO users.class_chat (course_id, year, semester, user_id, recipient_id, message) VALUES " +
                                "((SELECT id FROM users.course WHERE name = ?), ?, ?, ?, ?, ?)"
                );

                stmt.setString(1, courseFilter.getValue());
                stmt.setInt(2, yearFilter.getValue());
                stmt.setInt(3, semesterFilter.getValue());
                stmt.setInt(4, getCurrentUserId());

                String recipientName = recipientDropdown.getValue();
                if (recipientName != null && !recipientName.isEmpty()) {
                    stmt.setInt(5, getUserIdByName(recipientName));
                } else {
                    stmt.setNull(5, java.sql.Types.INTEGER);
                }

                stmt.setString(6, message);
                stmt.executeUpdate();

                chatInput.clear();
                recipientDropdown.setValue(null);
                loadChatMessages();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        private int getStudentIdByName(String name) {
            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT id FROM users.student WHERE name = ?");
                stmt.setString(1, name);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("id");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return -1;
        }

        private int getLecturerIdByName(String name) {
            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT id FROM users.lecturer WHERE name = ?");
                stmt.setString(1, name);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("id");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return -1;
        }
        public void setUserContext(String name, String role) {
            this.loggedInName = name;
            this.loggedInRole = role;

            //gradeBtn.setDisable(isStudent);
            //answerCol.setVisible(!isStudent());
            //studentNameField.setVisible(!isStudent());
            //assignmentComboBox.setVisible(!isStudent());
            //timeLimitField.setVisible(!isStudent());
            //generateQuizButton.setVisible(!isStudent());
            //answerCol.setDisable(isStudent());
            //addAssignmentButton.setDisable(isStudent());
            //questionField.setDisable(isStudent());
            //optionAField.setDisable(isStudent());
            //optionBField.setDisable(isStudent());
            //optionCField.setDisable(isStudent());
            //optionDField.setDisable(isStudent());
            //correctOptionCombo.setDisable(isStudent());
            //addQuestionButton.setDisable(isStudent());

        }

        private int getUserIdByName(String name) {
            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT id FROM users.users WHERE name = ?");
                stmt.setString(1, name);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("id");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return -1;
        }

        private int getCurrentUserId() {
            if (loggedInRole == null || loggedInName == null) return -1;

            String tableName;
            switch (loggedInRole.toLowerCase()) {
                case "student":
                    tableName = "users.student";
                    break;
                case "lecturer":
                    tableName = "users.lecturer";
                    break;
                default:
                    return -1; // Unknown role
            }

            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT id FROM " + tableName + " WHERE name = ?");
                stmt.setString(1, loggedInName);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("id");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return -1;
        }


    }
