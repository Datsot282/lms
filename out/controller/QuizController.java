package com.example.lms.controller;

import com.example.lms.connection.DBConnection;
import com.example.lms.model.Question;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class QuizController {

    @FXML private TableView<Question> questionTable;
    @FXML private TableColumn<Question, String> questionCol;
    @FXML private TableColumn<Question, String> optionACol;
    @FXML private TableColumn<Question, String> optionBCol;
    @FXML private TableColumn<Question, String> optionCCol;
    @FXML private TableColumn<Question, String> optionDCol;
    @FXML private TableColumn<Question, String> answerCol;

    @FXML
    private TableColumn<Question, Void> actionColumn;


    @FXML private TextField questionField;
    @FXML private TextField optionAField;
    @FXML private TextField optionBField;
    @FXML private TextField optionCField;
    @FXML private TextField optionDField;
    @FXML
    private TextField studentNameField;

    @FXML private ComboBox<String> correctOptionCombo;

    @FXML private Button submitButton;

    @FXML private ComboBox<String> assignmentComboBox;
    private Map<String, Integer> assignmentMap = new HashMap<>();

    @FXML private TextField timeLimitField;
    @FXML private Button generateQuizButton;
    @FXML private TableColumn<Question, Void> studentAnswerCol;

    private String loggedInRole;
    private String loggedInName;


    @FXML private Button addAssignmentButton;
    @FXML private Button addQuestionButton;

    @FXML
    private ComboBox<String> studentComboBox;
    private Map<String, Integer> studentMap = new HashMap<>();

    private final Button gradeBtn = new Button("Grade");
    private final HBox gradePane = new HBox(5, gradeBtn);


    private final ObservableList<Question> questionList = FXCollections.observableArrayList();
    private final Map<Integer, String> studentAnswers = new HashMap<>();

    @FXML
    public void initialize() {
        // Setup radio buttons
        studentAnswerCol.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(null));  // Required for rendering
        studentAnswerCol.setCellFactory(col -> new TableCell<Question, Void>() {
            private final ToggleGroup group = new ToggleGroup();
            private final RadioButton aBtn = new RadioButton("A");
            private final RadioButton bBtn = new RadioButton("B");
            private final RadioButton cBtn = new RadioButton("C");
            private final RadioButton dBtn = new RadioButton("D");
            private final HBox hBox = new HBox(5, aBtn, bBtn, cBtn, dBtn);

            {
                aBtn.setToggleGroup(group);
                bBtn.setToggleGroup(group);
                cBtn.setToggleGroup(group);
                dBtn.setToggleGroup(group);

                group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
                    if (newToggle != null && getTableRow() != null && getIndex() < getTableView().getItems().size()) {
                        Question q = getTableView().getItems().get(getIndex());
                        studentAnswers.put(q.getId(), ((RadioButton) newToggle).getText());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || !isStudent()) {
                    setGraphic(null);
                } else {
                    Question q = getTableView().getItems().get(getIndex());
                    String selected = studentAnswers.get(q.getId());
                    if ("A".equals(selected)) group.selectToggle(aBtn);
                    else if ("B".equals(selected)) group.selectToggle(bBtn);
                    else if ("C".equals(selected)) group.selectToggle(cBtn);
                    else if ("D".equals(selected)) group.selectToggle(dBtn);
                    else group.selectToggle(null);
                    setGraphic(hBox);
                }
            }
        });
        // Setup columns
        questionCol.setCellValueFactory(new PropertyValueFactory<>("questionText"));
        optionACol.setCellValueFactory(new PropertyValueFactory<>("optionA"));
        optionBCol.setCellValueFactory(new PropertyValueFactory<>("optionB"));
        optionCCol.setCellValueFactory(new PropertyValueFactory<>("optionC"));
        optionDCol.setCellValueFactory(new PropertyValueFactory<>("optionD"));
        answerCol.setCellValueFactory(new PropertyValueFactory<>("correctOption"));
        correctOptionCombo.setItems(FXCollections.observableArrayList("A", "B", "C", "D"));
        questionTable.setItems(questionList);
        //studentAnswerCol.setCellValueFactory(new PropertyValueFactory<>("studentAnswer"));
        //studentAnswerCol.setCellValueFactory(cellData -> cellData.getValue().selectedOptionProperty());


        // ComboBox selection handler
        assignmentComboBox.setOnAction(event -> {
            String selectedTitle = assignmentComboBox.getValue();
            if (selectedTitle != null && assignmentMap.containsKey(selectedTitle)) {
                int selectedAssignmentId = assignmentMap.get(selectedTitle);

                int resolvedStudentId = -1;
                if (isStudent() && loggedInName != null && !loggedInName.isEmpty()) {
                    resolvedStudentId = getStudentIdByName(loggedInName);
                }

                loadQuestions(selectedAssignmentId, resolvedStudentId);
            }
        });


        // Setup action buttons
        actionColumn.setCellFactory(col -> new TableCell<Question, Void>() {
            private final Button answerBtn = new Button("Answer");
            private final Button reanswerBtn = new Button("Reanswer");
            private final Button submitBtn = new Button("Submit");
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final Button gradeBtn = new Button("Grade");

            private final HBox studentPane = new HBox(5, answerBtn, reanswerBtn, submitBtn);
            private final HBox instructorPane = new HBox(5, editBtn, deleteBtn);
            private final HBox gradePane = new HBox(5, gradeBtn);

            {
                // Student actions
                answerBtn.setOnAction(e -> {
                    Question q = getTableView().getItems().get(getIndex());
                    String studentName = loggedInName;
                    if (studentName == null || studentName.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "User not logged in or student name is missing.");
                        return;
                    }

                    int studentId = getStudentIdByName(studentName);
                    if (studentId == -1) {
                        showAlert(Alert.AlertType.ERROR, "Invalid student name.");
                    }
                });

                reanswerBtn.setOnAction(e -> {
                    Question q = getTableView().getItems().get(getIndex());
                    System.out.println("Re-answering: " + q.getQuestionText());
                });

                submitBtn.setOnAction(e -> {
                    Question q = getTableView().getItems().get(getIndex());
                    String studentName = loggedInName;

                    if (studentName == null || studentName.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "User not logged in or student name is missing.");
                        return;
                    }

                    int studentId = getStudentIdByName(studentName);
                    if (studentId == -1) {
                        showAlert(Alert.AlertType.ERROR, "Invalid student name.");
                        return;
                    }

                    String selectedTitle = assignmentComboBox.getSelectionModel().getSelectedItem();
                    if (selectedTitle == null) {
                        showAlert(Alert.AlertType.ERROR, "No assignment selected.");
                        return;
                    }

                    int assignmentId = assignmentMap.get(selectedTitle);
                    String selectedAnswer = studentAnswers.get(q.getId());

                    if (selectedAnswer == null || selectedAnswer.isEmpty()) {
                        showAlert(Alert.AlertType.WARNING, "No answer selected for question: " + q.getQuestionText());
                        return;
                    }

                    try (Connection conn = DBConnection.getConnection()) {
                        PreparedStatement stmt = conn.prepareStatement(
                                "INSERT INTO users.submissions (assignment_id, student_name, student_id, question_id, answer, grade) VALUES (?, ?, ?, ?, ?, ?)"
                        );
                        stmt.setInt(1, assignmentId);
                        stmt.setString(2, studentName);
                        stmt.setInt(3, studentId);
                        stmt.setInt(4, q.getId());
                        stmt.setString(5, selectedAnswer);
                        stmt.setString(6, ""); // Grade will be updated later
                        stmt.executeUpdate();

                        showAlert(Alert.AlertType.INFORMATION, "Answer submitted for: " + q.getQuestionText());
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Submission failed for: " + q.getQuestionText());
                    }
                });

                // Instructor actions
                editBtn.setOnAction(e -> {
                    Question q = getTableView().getItems().get(getIndex());
                    System.out.println("Editing: " + q.getQuestionText());
                });

                deleteBtn.setOnAction(e -> {
                    Question q = getTableView().getItems().get(getIndex());
                    questionList.remove(q);
                    System.out.println("Deleted: " + q.getQuestionText());
                });

                gradeBtn.setOnAction(e -> {
                    Question q = getTableView().getItems().get(getIndex());
                    String studentName = studentComboBox.getValue();
                    String assignmentTitle = assignmentComboBox.getValue();

                    if (studentName == null || assignmentTitle == null) {
                        showAlert(Alert.AlertType.WARNING, "Please select a student and assignment first.");
                        return;
                    }

                    studentName = studentName.trim();
                    assignmentTitle = assignmentTitle.trim();

                    int studentId = getStudentIdByName(studentName);
                    if (studentId == -1) {
                        showAlert(Alert.AlertType.ERROR, "Invalid student name.");
                        return;
                    }

                    if (!assignmentMap.containsKey(assignmentTitle)) {
                        showAlert(Alert.AlertType.ERROR, "Invalid assignment selected.");
                        return;
                    }

                    int assignmentId = assignmentMap.get(assignmentTitle);

                    // Debug logs
                    System.out.println("------ DEBUG INFO ------");
                    System.out.println("Student Name     : '" + studentName + "'");
                    System.out.println("Assignment Title : '" + assignmentTitle + "'");
                    System.out.println("Student ID       : " + studentId);
                    System.out.println("Assignment ID    : " + assignmentId);
                    System.out.println("Question ID      : " + q.getId());
                    System.out.println("Question Text    : " + q.getQuestionText());
                    System.out.println("------------------------");

                    try (Connection conn = DBConnection.getConnection()) {
                        // Optional check for existence of submission
                        PreparedStatement testStmt = conn.prepareStatement(
                                "SELECT * FROM users.submissions WHERE assignment_id = ? AND student_id = ? AND question_id = ?"
                        );
                        testStmt.setInt(1, assignmentId);
                        testStmt.setInt(2, studentId);
                        testStmt.setInt(3, q.getId());
                        ResultSet testRs = testStmt.executeQuery();
                        if (!testRs.next()) {
                            System.out.println("NO matching submission found in 'submissions' table.");
                            showAlert(Alert.AlertType.WARNING, "No answer submitted for this question.");
                            return;
                        }

                        // Grade evaluation
                        PreparedStatement stmt = conn.prepareStatement(
                                "SELECT q.correct_option, s.answer " +
                                        "FROM users.questions q " +
                                        "JOIN users.submissions s ON q.id = s.question_id AND q.assignment_id = s.assignment_id " +
                                        "WHERE q.assignment_id = ? AND q.id = ? AND s.student_id = ?"
                        );
                        stmt.setInt(1, assignmentId);
                        stmt.setInt(2, q.getId());
                        stmt.setInt(3, studentId);

                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            String studentAnswer = rs.getString("answer");
                            String correctAnswer = rs.getString("correct_option");

                            String grade = studentAnswer != null && studentAnswer.equalsIgnoreCase(correctAnswer) ? "2" : "0";

                            PreparedStatement updateStmt = conn.prepareStatement(
                                    "UPDATE users.submissions SET grade = ? WHERE student_id = ? AND assignment_id = ? AND question_id = ?"
                            );
                            updateStmt.setString(1, grade);
                            updateStmt.setInt(2, studentId);
                            updateStmt.setInt(3, assignmentId);
                            updateStmt.setInt(4, q.getId());

                            int updated = updateStmt.executeUpdate();
                            System.out.println("Updated rows: " + updated);

                            showAlert(Alert.AlertType.INFORMATION, "Graded: " + q.getQuestionText() + " (Score: " + grade + ")");
                        } else {
                            showAlert(Alert.AlertType.WARNING, "Answer not found for grading.");
                        }

                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Failed to grade: " + q.getQuestionText());
                    }
                });




            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(isStudent() ? studentPane : gradePane);
                }
            }
        });

        loadAssignments();
        if (!isStudent()) {
            loadStudentsWithSubmissions();
        }
    }
    private void filterStudentAnswers() {
        String studentName = studentComboBox.getValue();
        String assignmentTitle = assignmentComboBox.getValue();

        if (studentName == null || assignmentTitle == null) return;

        int studentId = studentMap.get(studentName);
        int assignmentId = assignmentMap.get(assignmentTitle);

        questionList.clear();

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT q.id, q.question_text, q.option_a, q.option_b, q.option_c, q.option_d, q.correct_option, s.answer, s.grade " +
                            "FROM users.questions q " +
                            "JOIN users.submissions s ON q.id = s.question_id " +
                            "WHERE s.assignment_id = ? AND s.student_id = ?"
            );
            stmt.setInt(1, assignmentId);
            stmt.setInt(2, studentId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Question q = new Question(
                        rs.getInt("id"),
                        rs.getString("question_text"),
                        rs.getString("option_a"),
                        rs.getString("option_b"),
                        rs.getString("option_c"),
                        rs.getString("option_d"),
                        rs.getString("correct_option"),
                        rs.getString("answer")
                );
                // Optionally store answer/grade if you need it
               // q.setSelectedOption(rs.getString("answer"));
                questionList.add(q);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadStudentsWithSubmissions() {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT DISTINCT student_id, student_name FROM users.submissions");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("student_id");
                String name = rs.getString("student_name");
                studentComboBox.getItems().add(name);
                studentMap.put(name, id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private Map<String, Integer> getModulesWithCourseId() {
        Map<String, Integer> moduleMap = new HashMap<>();
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM users.module");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                moduleMap.put(rs.getString("name"), rs.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return moduleMap;
    }
    private int getCourseIdByModuleId(int moduleId) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT course_id FROM users.module WHERE id = ?");
            stmt.setInt(1, moduleId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("course_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void loadAssignments() {
        assignmentMap.clear();
        assignmentComboBox.getItems().clear();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, title FROM users.assignments WHERE file_path IS NULL")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                assignmentMap.put(title, id);
                assignmentComboBox.getItems().add(title);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setUserContext(String name, String role) {
        this.loggedInName = name;
        this.loggedInRole = role;

        //gradeBtn.setDisable(isStudent);
        answerCol.setVisible(!isStudent());
        //studentNameField.setVisible(!isStudent());
        //assignmentComboBox.setVisible(!isStudent());
        //timeLimitField.setVisible(!isStudent());
        //generateQuizButton.setVisible(!isStudent());
        //answerCol.setDisable(isStudent());
        addAssignmentButton.setDisable(isStudent());
        questionField.setDisable(isStudent());
        optionAField.setDisable(isStudent());
        optionBField.setDisable(isStudent());
        optionCField.setDisable(isStudent());
        optionDField.setDisable(isStudent());
        correctOptionCombo.setDisable(isStudent());
        addQuestionButton.setDisable(isStudent());

    }

    private boolean isStudent() {
        return "student".equalsIgnoreCase(loggedInRole);

    }

    @FXML
    private void handleGenerateQuiz() {
        if (!isStudent()) {
            showAlert(Alert.AlertType.ERROR, "Only students can generate a quiz.");
            return;
        }

        int assignmentId = 0;
        int timeLimit;

        try {
            String selectedTitle = assignmentComboBox.getSelectionModel().getSelectedItem();
            if (selectedTitle != null && assignmentMap.containsKey(selectedTitle)) {
                assignmentId = assignmentMap.get(selectedTitle);
            }

            timeLimit = Integer.parseInt(timeLimitField.getText());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Assignment ID or Time Limit.");
            return;
        }

        if (timeLimit <= 0) {
            showAlert(Alert.AlertType.ERROR, "Time Limit must be a positive integer.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmtQuiz = conn.prepareStatement(
                    "INSERT INTO users.assignment_quiz (assignment_id, time_limit) VALUES (?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS
            );
            stmtQuiz.setInt(1, assignmentId);
            stmtQuiz.setInt(2, timeLimit);
            stmtQuiz.executeUpdate();

            ResultSet generatedKeys = stmtQuiz.getGeneratedKeys();
            if (generatedKeys.next()) {
                int quizId = generatedKeys.getInt(1);

                PreparedStatement stmtQuestions = conn.prepareStatement(
                        "SELECT id FROM users.questions WHERE assignment_id = ? ORDER BY RANDOM() LIMIT 15"
                );
                stmtQuestions.setInt(1, assignmentId);
                ResultSet rs = stmtQuestions.executeQuery();

                PreparedStatement stmtLink = conn.prepareStatement(
                        "INSERT INTO users.assignment_quiz_questions (quiz_id, question_id) VALUES (?, ?)"
                );
                while (rs.next()) {
                    int questionId = rs.getInt("id");
                    stmtLink.setInt(1, quizId);
                    stmtLink.setInt(2, questionId);
                    stmtLink.executeUpdate();
                }

                showAlert(Alert.AlertType.INFORMATION, "Quiz Generated Successfully for Assignment ID " + assignmentId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadStudentQuiz(int assignmentId) {
        questionList.clear();
        studentAnswers.clear();

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmtQuiz = conn.prepareStatement(
                    "SELECT id, time_limit FROM users.assignment_quiz WHERE assignment_id = ? ORDER BY created_at DESC LIMIT 1"
            );
            stmtQuiz.setInt(1, assignmentId);
            ResultSet quizResult = stmtQuiz.executeQuery();

            if (quizResult.next()) {
                int quizId = quizResult.getInt("id");
                int timeLimit = quizResult.getInt("time_limit");

                PreparedStatement stmtQuestions = conn.prepareStatement(
                        "SELECT q.id, q.question_text, q.option_a, q.option_b, q.option_c, q.option_d, q.correct_option, " +
                                "s.answer AS student_answer " +
                                "FROM users.questions q " +
                                "LEFT JOIN users.submissions s ON q.id = s.question_id AND s.assignment_id = ? AND s.student_id = ? " +
                                "WHERE q.assignment_id = ?");
                stmtQuestions.setInt(1, quizId);
                ResultSet rs = stmtQuestions.executeQuery();

                while (rs.next()) {
                    Question question = new Question(
                            rs.getInt("id"),
                            rs.getString("question_text"),
                            rs.getString("option_a"),
                            rs.getString("option_b"),
                            rs.getString("option_c"),
                            rs.getString("option_d"),
                            rs.getString("correct_option"),
                            rs.getString("student_answer"));
                    //question.setStudentAnswer(rs.getString("answer"));
                    questionList.add(question);
                }

                startQuizTimer(timeLimit);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void startQuizTimer(int minutes) {
        final int[] secondsRemaining = {minutes * 60};

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            secondsRemaining[0]--;
            int minutesPart = secondsRemaining[0] / 60;
            int secondsPart = secondsRemaining[0] % 60;
            System.out.println(String.format("%02d:%02d", minutesPart, secondsPart));

            if (secondsRemaining[0] <= 0) {
                showAlert(Alert.AlertType.INFORMATION, "Time's up! Quiz submitted automatically.");
                handleSubmitQuiz();
                ((Timeline) event.getSource()).stop();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void loadQuestions(int assignmentId, int studentId) {
        questionList.clear();
        studentAnswers.clear(); // Clear previous answers

        String sql = "SELECT q.id, q.question_text, q.option_a, q.option_b, q.option_c, q.option_d, q.correct_option, " +
                "s.answer AS student_answer " +
                "FROM users.questions q " +
                "LEFT JOIN users.submissions s ON q.id = s.question_id AND s.assignment_id = ? AND s.student_id = ? " +
                "WHERE q.assignment_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, assignmentId);
            stmt.setInt(2, studentId);
            stmt.setInt(3, assignmentId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int questionId = rs.getInt("id");
                String studentAnswer = rs.getString("student_answer");

                Question question = new Question(
                        questionId,
                        rs.getString("question_text"),
                        rs.getString("option_a"),
                        rs.getString("option_b"),
                        rs.getString("option_c"),
                        rs.getString("option_d"),
                        rs.getString("correct_option"),
                        rs.getString("student_answer"));
                //question.setStudentAnswer(studentAnswer);

                if (studentAnswer != null) {
                    studentAnswers.put(questionId, studentAnswer); // ✅ Sync the map here
                }

                questionList.add(question);
            }

            questionTable.setItems(FXCollections.observableArrayList(questionList));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddAssignment() {
        if (isStudent()) {
            showAlert(Alert.AlertType.ERROR, "Only lecturers can add assignments.");
            return;
        }
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Add New Assignment");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField assignmentTitle = new TextField();
        assignmentTitle.setPromptText("Enter Assignment Title");

        TextField timeLimitField = new TextField();
        timeLimitField.setPromptText("Enter Time Limit (in minutes)");

        TextField gradeField = new TextField();
        gradeField.setPromptText("Enter Grade (numeric)");

        ComboBox<String> moduleComboBox = new ComboBox<>();
        Map<String, Integer> moduleMap = new HashMap<>();

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM users.module");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                int id = rs.getInt("id");
                moduleMap.put(name, id);
                moduleComboBox.getItems().add(name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        VBox vbox = new VBox(10, assignmentTitle, timeLimitField, moduleComboBox, gradeField);
        dialog.getDialogPane().setContent(vbox);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return assignmentTitle.getText() + "," + timeLimitField.getText() + "," +
                        moduleComboBox.getValue() + "," + gradeField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(data -> {
            String[] values = data.split(",", -1);
            if (values.length < 4 || Arrays.stream(values).anyMatch(String::isEmpty)) {
                showAlert(Alert.AlertType.ERROR, "Please fill all fields correctly.");
                return;
            }

            String title = values[0];
            int timeLimit;
            double grade;

            try {
                timeLimit = Integer.parseInt(values[1]);
                grade = Double.parseDouble(values[3]);
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid time limit or grade value.");
                return;
            }

            String selectedModuleName = values[2];
            int moduleId = moduleMap.get(selectedModuleName);
            int courseId = -1;
            int lecturerId = getLecturerIdByName(loggedInName);

            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement stmtCourse = conn.prepareStatement("SELECT course_id FROM users.module WHERE id = ?");
                stmtCourse.setInt(1, moduleId);
                ResultSet rsCourse = stmtCourse.executeQuery();
                if (rsCourse.next()) {
                    courseId = rsCourse.getInt("course_id");
                }

                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO users.assignments (title, lecturer_id, course_id, module_id, grade) VALUES (?, ?, ?, ?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS
                );
                stmt.setString(1, title);
                stmt.setInt(2, lecturerId);
                stmt.setInt(3, courseId);
                stmt.setInt(4, moduleId);
                stmt.setDouble(5, grade);
                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int assignmentId = rs.getInt(1);

                    PreparedStatement stmtQuiz = conn.prepareStatement(
                            "INSERT INTO users.assignment_quiz (assignment_id, time_limit) VALUES (?, ?)"
                    );
                    stmtQuiz.setInt(1, assignmentId);
                    stmtQuiz.setInt(2, timeLimit);
                    stmtQuiz.executeUpdate();

                    PreparedStatement stmtNotify = conn.prepareStatement(
                            "INSERT INTO users.notifications (assignment_id, message) VALUES (?, ?)"
                    );
                    stmtNotify.setInt(1, assignmentId);
                    stmtNotify.setString(2, "New Assignment Available: " + title);
                    stmtNotify.executeUpdate();

                    showAlert(Alert.AlertType.INFORMATION, "Assignment and Quiz Created Successfully!");
                    loadAssignments();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Failed to create assignment.");
            }
        });
    }

    @FXML
    private void handleAddQuestionToAssignment() {
        // Get input values directly from the fields
        String questionText = questionField.getText();
        String optionA = optionAField.getText();
        String optionB = optionBField.getText();
        String optionC = optionCField.getText();
        String optionD = optionDField.getText();
        String correctOption = correctOptionCombo.getValue(); // assuming this combo box is used for correct answer

        // Validation to ensure all fields are filled
        if (questionText.isEmpty() || optionA.isEmpty() || optionB.isEmpty() || optionC.isEmpty() || optionD.isEmpty() || correctOption == null) {
            showAlert(Alert.AlertType.ERROR, "Please fill in all fields correctly.");
            return;
        }

        // Get the assignment ID from the selected assignment
        String selectedAssignment = assignmentComboBox.getSelectionModel().getSelectedItem();
        if (selectedAssignment == null) {
            showAlert(Alert.AlertType.ERROR, "Please select an assignment.");
            return;
        }

        int assignmentId = assignmentMap.get(selectedAssignment);

        // Insert the question into the database
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO users.questions (assignment_id, question_text, option_a, option_b, option_c, option_d, correct_option) VALUES (?, ?, ?, ?, ?, ?, ?)"
            );
            stmt.setInt(1, assignmentId);
            stmt.setString(2, questionText);
            stmt.setString(3, optionA);
            stmt.setString(4, optionB);
            stmt.setString(5, optionC);
            stmt.setString(6, optionD);
            stmt.setString(7, correctOption);

            stmt.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Question added successfully to assignment " + selectedAssignment);

            // Optionally clear the fields after adding the question
            questionField.clear();
            optionAField.clear();
            optionBField.clear();
            optionCField.clear();
            optionDField.clear();
            correctOptionCombo.getSelectionModel().clearSelection();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Failed to add question.");
        }
    }

    private void handleSubmitQuiz() {
        String studentName = studentNameField.getText();
        if (studentName.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Student name is required.");
            return;
        }

        int studentId = getStudentIdByName(studentName);
        if (studentId == -1) {
            showAlert(Alert.AlertType.ERROR, "Invalid student name.");
            return;
        }

        String selectedTitle = assignmentComboBox.getSelectionModel().getSelectedItem();
        if (selectedTitle == null) {
            showAlert(Alert.AlertType.ERROR, "No assignment selected.");
            return;
        }

        int assignmentId = assignmentMap.get(selectedTitle);

        try (Connection conn = DBConnection.getConnection()) {
            boolean allAnswered = true;

            for (Question q : questionList) {
                int questionId = q.getId();
                String answer = studentAnswers.get(questionId);

                if (answer == null || answer.isEmpty()) {
                    allAnswered = false;
                    continue; // skip unanswered question
                }

                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO users.submissions (assignment_id, student_name, student_id, question_id, answer, grade) VALUES (?, ?, ?, ?, ?, ?)"
                );
                stmt.setInt(1, assignmentId);
                stmt.setString(2, studentName);
                stmt.setInt(3, studentId);
                stmt.setInt(4, questionId);
                stmt.setString(5, answer);
                stmt.setString(6, ""); // Grade will be calculated later
                stmt.executeUpdate();
            }

            if (allAnswered) {
                showAlert(Alert.AlertType.INFORMATION, "Quiz submitted successfully!");
            } else {
                showAlert(Alert.AlertType.WARNING, "Quiz submitted with some unanswered questions.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error submitting quiz.");
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

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
