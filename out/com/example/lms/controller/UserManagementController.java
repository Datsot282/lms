package com.example.lms.controller;

import com.example.lms.connection.DBConnection;
import com.example.lms.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.sql.*;
import java.time.LocalDateTime;

public class UserManagementController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> IdColumn;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> surnameColumn;
    @FXML private TableColumn<User, String> idccardnoColumn;
    @FXML private TableColumn<User, String> passwordColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, Void> actionColumn;

    private final ObservableList<User> users = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Wire up columns
        IdColumn       .setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn     .setCellValueFactory(new PropertyValueFactory<>("name"));
        surnameColumn  .setCellValueFactory(new PropertyValueFactory<>("surname"));
        idccardnoColumn.setCellValueFactory(new PropertyValueFactory<>("idcardno"));
        passwordColumn .setCellValueFactory(new PropertyValueFactory<>("password"));
        roleColumn     .setCellValueFactory(new PropertyValueFactory<>("role"));

        // Add Edit/Delete buttons
        actionColumn.setCellFactory(col -> new TableCell<User, Void>() {
            private final Button editBtn   = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox pane        = new HBox(10, editBtn, deleteBtn);
            {
                editBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    showUserDialog(u, true);
                });
                deleteBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    deleteUser(u);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        // Bind list & load
        userTable.setItems(users);
        loadUsers();
    }

    private void loadUsers() {
        users.clear();
        String sql = "SELECT id, name, surname, idcardno, password, role, created_at FROM users.users";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet r = p.executeQuery()) {

            while (r.next()) {
                users.add(new User(
                        r.getInt   ("id"),
                        r.getString("name"),
                        r.getString("surname"),
                        r.getString("idcardno"),
                        r.getString("password"),
                        r.getString("role"),
                        r.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error", "Failed to load users.");
        }
    }

    @FXML
    private void handleAddUser() {
        showUserDialog(null, false);
    }

    private void showUserDialog(User existing, boolean isEdit) {
        Dialog<User> dlg = new Dialog<>();
        dlg.setTitle(isEdit ? "Edit User" : "Add New User");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);

        TextField nameF    = new TextField();
        TextField surnameF = new TextField();
        TextField idcF     = new TextField();
        PasswordField pwF  = new PasswordField();
        ComboBox<String> roleBox = new ComboBox<>(
                FXCollections.observableArrayList("Admin", "Lecturer", "Student")
        );

        if (isEdit) {
            nameF   .setText(existing.getName());
            surnameF.setText(existing.getSurname());
            idcF    .setText(existing.getIdcardno());
            pwF     .setText(existing.getPassword());
            roleBox.setValue(existing.getRole());
        }

        form.add(new Label("Name:"),       0, 0); form.add(nameF,    1, 0);
        form.add(new Label("Surname:"),    0, 1); form.add(surnameF, 1, 1);
        form.add(new Label("ID Card No:"), 0, 2); form.add(idcF,     1, 2);
        form.add(new Label("Password:"),   0, 3); form.add(pwF,      1, 3);
        form.add(new Label("Role:"),       0, 4); form.add(roleBox,  1, 4);

        dlg.getDialogPane().setContent(form);

        dlg.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String n   = nameF.getText().trim();
                String s   = surnameF.getText().trim();
                String idc = idcF.getText().trim();
                String pw  = pwF.getText();
                String rl  = roleBox.getValue();
                if (n.isEmpty()||s.isEmpty()||idc.isEmpty()||pw.isEmpty()||rl==null) {
                    showAlert("Validation", "All fields are required.");
                    return null;
                }
                if (isEdit) {
                    existing.nameProperty()    .set(n);
                    existing.surnameProperty() .set(s);
                    existing.idcardnoProperty().set(idc);
                    existing.passwordProperty().set(pw);
                    existing.roleProperty()    .set(rl);
                    updateUser(existing);
                    return existing;
                } else {
                    User u = new User(0, n, s, idc, pw, rl, LocalDateTime.now());
                    insertUser(u);
                    return u;
                }
            }
            return null;
        });

        dlg.showAndWait().ifPresent(u -> {
            if (!isEdit) users.add(u);
            userTable.refresh();
        });
    }

    private void insertUser(User u) {
        String sql = "INSERT INTO users.users(name, surname, idcardno, password, role, created_at) VALUES(?,?,?,?,?,now())";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            p.setString(1, u.getName());
            p.setString(2, u.getSurname());
            p.setString(3, u.getIdcardno());
            p.setString(4, u.getPassword());
            p.setString(5, u.getRole());
            p.executeUpdate();

            try (ResultSet keys = p.getGeneratedKeys()) {
                if (keys.next()) {
                    int userId = keys.getInt(1);
                    u.idProperty().set(userId);

                    // Add to student or lecturer table
                    if (u.getRole().equalsIgnoreCase("Student")) {
                        String sSql = "INSERT INTO users.student (id, name, year_enrolled, current_year, current_semester) VALUES (?, ?, EXTRACT(YEAR FROM CURRENT_DATE), 1, 1)";
                        try (PreparedStatement ps = c.prepareStatement(sSql)) {
                            ps.setInt(1, userId);
                            ps.setString(2, u.getName());
                            ps.executeUpdate();
                        }
                    } else if (u.getRole().equalsIgnoreCase("Lecturer")) {
                        String lSql = "INSERT INTO users.lecturer (id, name) VALUES (?, ?)";
                        try (PreparedStatement pl = c.prepareStatement(lSql)) {
                            pl.setInt(1, userId);
                            pl.setString(2, u.getName());
                            pl.executeUpdate();
                        }
                    }
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error", "Failed to add user.");
        }
        loadUsers();
    }


    private void updateUser(User u) {
        String sql = "UPDATE users.users SET name=?, surname=?, idcardno=?, password=?, role=? WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, u.getName());
            p.setString(2, u.getSurname());
            p.setString(3, u.getIdcardno());
            p.setString(4, u.getPassword());
            p.setString(5, u.getRole());
            p.setInt   (6, u.getId());
            p.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error", "Failed to update user.");
        }
        loadUsers();
    }

    private void deleteUser(User u) {
        String sql = "DELETE FROM users.users WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, u.getId());
            p.executeUpdate();
            users.remove(u);
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error", "Failed to delete user.");
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
