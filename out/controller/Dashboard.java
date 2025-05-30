package com.example.lms.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import java.io.IOException;

public class Dashboard {

    @FXML
    private Tab statisticsTab;
    @FXML
    private TabPane tabPane;
    @FXML
    private Tab coursesManagementTab;
    @FXML
    private Tab schoolCalendarTab;
    @FXML
    private Tab announcementsTab;
    @FXML
    private Tab resultsGradesTab;
    @FXML
    private Tab materialsUploadTab;
    @FXML
    private Tab userManagementTab;
    @FXML
    private Tab logoutTab;
    @FXML
    private Tab assignmentsTab;
    @FXML
    private Tab moduleManagementTab;
    @FXML
    private Tab quizTab;
    @FXML
    private Tab transcriptTab;

    // Role and username of the logged-in user
    private String loggedInRole;
    private String loggedInName;

    @FXML
    private void initialize() {
        if (tabPane.getTabs().contains(statisticsTab)) {
            loadTabContent(statisticsTab, "/com/example/lms/statistics.fxml");
        }

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == announcementsTab) {
                loadTabContent(announcementsTab, "/com/example/lms/Notices.fxml");
            } else if (newTab == schoolCalendarTab) {
                loadTabContent(schoolCalendarTab, "/com/example/lms/SchoolCalendar.fxml");
            } else if (newTab == materialsUploadTab) {
                loadTabContent(materialsUploadTab, "/com/example/lms/UploadMaterials.fxml");
            } else if (newTab == resultsGradesTab) {
                loadTabContent(resultsGradesTab, "/com/example/lms/results_view.fxml");
            } else if (newTab == userManagementTab) {
                loadTabContent(userManagementTab, "/com/example/lms/user_management.fxml");
            } else if (newTab == coursesManagementTab) {
                loadTabContent(coursesManagementTab, "/com/example/lms/CoursesManagement.fxml");
            } else if (newTab == assignmentsTab) {
                loadTabContent(assignmentsTab, "/com/example/lms/assignments_view.fxml");
            } else if (newTab == moduleManagementTab) {
                loadTabContent(moduleManagementTab, "/com/example/lms/ModuleManagement.fxml");
            } else if (newTab == quizTab) {
                // Inject name and role into QuizController
                loadTabContent(quizTab, "/com/example/lms/quiz_view.fxml");

            } else if (newTab == transcriptTab) {
                loadTabContent(transcriptTab, "/com/example/lms/transcript_view.fxml");
            } else if ((newTab == logoutTab)) {
                logout();
            }
        });
    }

    public void setUserRole(String role, String name) {
        this.loggedInRole = role;
        this.loggedInName = name;

        if ("admin".equalsIgnoreCase(role)) {
            enableAllTabs();
        } else if ("lecturer".equalsIgnoreCase(role)) {
            enableLecturerTabs();
        } else if ("student".equalsIgnoreCase(role)) {
            enableStudentTabs();
        }
    }

    private void enableAllTabs() {
        statisticsTab.setDisable(false);
        announcementsTab.setDisable(false);
        schoolCalendarTab.setDisable(false);
        materialsUploadTab.setDisable(false);
        resultsGradesTab.setDisable(false);
        userManagementTab.setDisable(false);
        coursesManagementTab.setDisable(false);
        assignmentsTab.setDisable(false);
        moduleManagementTab.setDisable(false);
        quizTab.setDisable(false);
        transcriptTab.setDisable(false);

    }

    private void enableLecturerTabs() {
        statisticsTab.setDisable(false);
        announcementsTab.setDisable(false);
        schoolCalendarTab.setDisable(false);
        materialsUploadTab.setDisable(false);
        resultsGradesTab.setDisable(false);
        userManagementTab.setDisable(true);
        coursesManagementTab.setDisable(false);
        assignmentsTab.setDisable(false);
        moduleManagementTab.setDisable(false);
        quizTab.setDisable(false);
        transcriptTab.setDisable(false);
    }

    private void enableStudentTabs() {
        statisticsTab.setDisable(false);
        announcementsTab.setDisable(true);
        schoolCalendarTab.setDisable(true);
        materialsUploadTab.setDisable(false);
        resultsGradesTab.setDisable(false);
        userManagementTab.setDisable(true);
        coursesManagementTab.setDisable(true);
        assignmentsTab.setDisable(false);
        moduleManagementTab.setDisable(false);
        quizTab.setDisable(false);
        transcriptTab.setDisable(false);
    }

    private void loadTabContent(Tab tab, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            tab.setContent(content);

            // Inject context into QuizController if needed
            if ("/com/example/lms/quiz_view.fxml".equals(fxmlPath)) {
                com.example.lms.controller.QuizController controller = loader.getController();
                controller.setUserContext(loggedInName, loggedInRole);
                //controller.initData(); // Optional: only if you have extra setup logic
            }
            if("/com/example/lms/assignments_view.fxml".equals(fxmlPath)) {
                com.example.lms.controller.AssignmentController controller = loader.getController();
                controller.setUserContext(loggedInName, loggedInRole);

            }
            if("/com/example/lms/transcript_view.fxml".equals(fxmlPath)) {
                com.example.lms.controller.TranscriptController controller = loader.getController();
                controller.setUserContext(loggedInName, loggedInRole);

            }
            if("/com/example/lms/statistics.fxml".equals(fxmlPath)) {
                com.example.lms.controller.StatisticsController controller = loader.getController();
                controller.setUserContext(loggedInName, loggedInRole);

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/lms/login_register.fxml"));
            tabPane.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
