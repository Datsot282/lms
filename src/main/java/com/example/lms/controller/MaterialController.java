package com.example.lms.controller;

import com.example.lms.connection.DBConnection;
import com.example.lms.model.Material;
import com.example.lms.model.Module;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.stage.Window;


import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;

public class MaterialController {
    @FXML private TableView<Material> table;
    @FXML private TableColumn<Material, Integer> idCol;
    @FXML private TableColumn<Material, String> titleCol, descCol, typeCol;
    @FXML private TableColumn<Material, LocalDateTime> dateCol;
    @FXML private TableColumn<Material, Void> actionCol;

    private final ObservableList<Material> data = FXCollections.observableArrayList();
    private final String uploadDir = "C:/Users/Administrator/Downloads/@SCHOOL/DBITY2S2/OOP2/OOP_GROUP_ASSIGNMENT/lms";

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        titleCol.setCellValueFactory(cellData -> cellData.getValue().titleProperty());
        descCol.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        typeCol.setCellValueFactory(cellData -> cellData.getValue().fileTypeProperty());
        dateCol.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getUploadedAt()));

        addActionButtonsToTable();
        loadData();
    }
    private void addActionButtonsToTable() {
            actionCol.setCellFactory(param -> new TableCell<Material, Void>() {
            private final Button viewButton = new Button("View");
            private final Button downloadButton = new Button("Download");
            private final HBox box = new HBox(5, viewButton, downloadButton);

            {
                viewButton.setOnAction(e -> {
                    Material material = (Material) getTableView().getItems().get(getIndex());
                    try {
                        File file = new File(material.getFilePath());
                        if (file.exists()) {
                            Desktop.getDesktop().open(file);  // Opens with default app
                        } else {
                            showAlert("File not found.");
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        showAlert("Unable to open file.");
                    }
                });

                downloadButton.setOnAction(e -> {
                    Material material = (Material) getTableView().getItems().get(getIndex());
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setInitialFileName(new File(material.getFilePath()).getName());
                    File destFile = fileChooser.showSaveDialog(new Stage());

                    if (destFile != null) {
                        try {
                            Files.copy(Paths.get(material.getFilePath()), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            showAlert("Download completed.");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            showAlert("Download failed.");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }



    private void loadData() {
        data.clear();
        String sql = "SELECT * FROM users.material ORDER BY uploaded_at DESC";
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {
            while (r.next()) {
                data.add(new Material(
                        r.getInt("id"),
                        r.getString("title"),
                        r.getString("description"),
                        r.getString("file_path"),
                        r.getString("file_type"),
                        r.getTimestamp("uploaded_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        table.setItems(data);
    }

    @FXML
    public void onUpload() {
        Dialog<Material> dialog = new Dialog<>();
        dialog.setTitle("Upload Material");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField titleF = new TextField();
        TextArea descA = new TextArea();
        Label fileLbl = new Label("No file selected");
        Button chooseBtn = new Button("Choose File");

        final File[] selectedFile = {null};

        chooseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            Window window = dialog.getDialogPane().getScene().getWindow(); // use correct owner window
            selectedFile[0] = fc.showOpenDialog(window);
            if (selectedFile[0] != null) {
                fileLbl.setText(selectedFile[0].getName());
            }
        });

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleF, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descA, 1, 1);
        grid.add(new Label("File:"), 0, 2);
        grid.add(fileLbl, 1, 2);
        grid.add(chooseBtn, 2, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK && selectedFile[0] != null) {
                try {
                    if (!selectedFile[0].exists()) {
                        showAlert("File Error", "The selected file no longer exists.");
                        return null;
                    }

                    // Ensure the upload directory exists
                    Path uploadPath = Paths.get(uploadDir);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }

                    String destPath = uploadDir + System.currentTimeMillis() + "_" + selectedFile[0].getName();
                    Path destination = Paths.get(destPath);

                    // Copy the file
                    Files.copy(selectedFile[0].toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

                    // Get MIME type
                    String type = Files.probeContentType(destination);

                    // Create Material object
                    Material m = new Material(0, titleF.getText(), descA.getText(), destPath, type, LocalDateTime.now());

                    // Save to database
                    saveMaterial(m);

                    return m;

                } catch (IOException | SQLException ex) {
                    ex.printStackTrace();
                    showAlert("Upload Error", "An error occurred while uploading the file:\n" + ex.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(m -> {
            data.add(m);
            table.refresh();
        });
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }


    private void saveMaterial(Material m) throws SQLException {
        String sql = "INSERT INTO users.material(title, description, file_path, file_type, uploaded_at) VALUES(?,?,?,?,NOW())";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, m.getTitle());
            p.setString(2, m.getDescription());
            p.setString(3, m.getFilePath());
            p.setString(4, m.getFileType());
            p.executeUpdate();

            try (ResultSet k = p.getGeneratedKeys()) {
                if (k.next()) m.idProperty().set(k.getInt(1));
            }
        }
    }
}
