package com.example.lms.controller;

import com.example.lms.connection.DBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    /**
     * Handles the login action when the "Login" button is clicked.
     */
    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Login Failed", "Please enter both username and password.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // SQL query to fetch user role based on credentials
            String query = "SELECT role, name FROM users.users WHERE name = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Get user details from the result set
                String role = rs.getString("role").toLowerCase(); // Ensure lowercase for consistency
                String name = rs.getString("name");

                showAlert(Alert.AlertType.INFORMATION, "Login Successful", "Welcome, " + name + " (" + role + ")");

                // Load the dashboard and pass user information
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/lms/dashboard.fxml"));
                Parent dashboardRoot = loader.load();

                // Get the controller and set user role and name
                Dashboard controller = loader.getController();
                controller.setUserRole(role, name);

                // Switch to dashboard scene
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(dashboardRoot));
                stage.show();

            } else {
                // Invalid credentials
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not connect to the database.");
        }
    }

    /**
     * Utility method to show alert pop-ups.
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
