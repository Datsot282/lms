package com.example.lms.controller;

import com.example.lms.connection.DBConnection;
import com.example.lms.model.Announcement;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.sql.*;
import java.time.LocalDateTime;

public class AnnouncementController {

    @FXML private TableView<Announcement> table;
    @FXML private TableColumn<Announcement, Integer>       idCol;
    @FXML private TableColumn<Announcement, String>        titleCol;
    @FXML private TableColumn<Announcement, LocalDateTime> timeCol;
    @FXML private TableColumn<Announcement, Void>          actionCol;

    private final ObservableList<Announcement> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idCol   .setCellValueFactory(new PropertyValueFactory<>("id"));
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        timeCol .setCellValueFactory(new PropertyValueFactory<>("postedAt"));

        actionCol.setCellFactory(col -> new TableCell<Announcement, Void>() {
            private final Button editBtn   = new Button("Edit");
            private final Button delBtn    = new Button("Delete");
            {
                editBtn.setOnAction(e -> {
                    Announcement a = getTableView().getItems().get(getIndex());
                    showDialog(a, true);
                });
                delBtn.setOnAction(e -> {
                    Announcement a = getTableView().getItems().get(getIndex());
                    delete(a);
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : new HBox(5, editBtn, delBtn));
            }
        });

        load();
    }

    private void load() {
        data.clear();
        String sql = "SELECT id,title,content,posted_at FROM users.announcement ORDER BY posted_at DESC";
        try (Connection c = DBConnection.getConnection();
             Statement  s = c.createStatement();
             ResultSet  r = s.executeQuery(sql)) {
            while (r.next()) {
                data.add(new Announcement(
                        r.getInt("id"),
                        r.getString("title"),
                        r.getString("content"),
                        r.getTimestamp("posted_at").toLocalDateTime()
                ));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            alert("Error","Could not load announcements.");
        }
        table.setItems(data);
    }

    @FXML private void onNew() {
        showDialog(null, false);
    }

    private void showDialog(Announcement existing, boolean isEdit) {
        Dialog<Announcement> dlg = new Dialog<>();
        dlg.setTitle(isEdit ? "Edit Notice" : "New Notice");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);

        TextField  titleF = new TextField();
        TextArea   contentA = new TextArea(); contentA.setPrefRowCount(4);

        if (isEdit && existing!=null) {
            titleF  .setText(existing.getTitle());
            contentA.setText(existing.getContent());
        }

        form.add(new Label("Title:"),   0,0); form.add(titleF,   1,0);
        form.add(new Label("Content:"), 0,1); form.add(contentA,1,1);
        dlg.getDialogPane().setContent(form);

        dlg.setResultConverter(b -> {
            if (b==ButtonType.OK) {
                String t = titleF.getText().trim();
                String c = contentA.getText().trim();
                if (t.isEmpty()||c.isEmpty()) {
                    alert("Validation","Both fields are required.");
                    return null;
                }
                if (isEdit) {
                    update(existing.getId(), t, c);
                    existing.titleProperty().set(t);
                    existing.contentProperty().set(c);
                    return existing;
                } else {
                    Announcement a = new Announcement(0, t, c, LocalDateTime.now());
                    insert(a);
                    return a;
                }
            }
            return null;
        });

        dlg.showAndWait().ifPresent(a -> {
            if (!isEdit) data.add(a);
            table.refresh();
        });
    }

    private void insert(Announcement a) {
        String sql = "INSERT INTO users.announcement(title,content,posted_at) VALUES(?,?,NOW())";
        try (Connection c=DBConnection.getConnection();
             PreparedStatement p=c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1,a.getTitle());
            p.setString(2,a.getContent());
            p.executeUpdate();
            try (ResultSet k=p.getGeneratedKeys()) {
                if (k.next()) a.idProperty().set(k.getInt(1));
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            alert("Error","Failed to add notice.");
        }
        load();
    }

    private void update(int id, String t, String c) {
        String sql = "UPDATE users.announcement SET title=?,content=? WHERE id=?";
        try (Connection conn=DBConnection.getConnection();
             PreparedStatement p=conn.prepareStatement(sql)) {
            p.setString(1,t);
            p.setString(2,c);
            p.setInt(3,id);
            p.executeUpdate();
        } catch(SQLException ex){
            ex.printStackTrace();
            alert("Error","Failed to update notice.");
        }
        load();
    }

    private void delete(Announcement a) {
        String sql = "DELETE FROM users.announcement WHERE id=?";
        try (Connection c=DBConnection.getConnection();
             PreparedStatement p=c.prepareStatement(sql)) {
            p.setInt(1,a.getId());
            p.executeUpdate();
            data.remove(a);
        } catch(SQLException ex){
            ex.printStackTrace();
            alert("Error","Failed to delete notice.");
        }
    }

    private void alert(String hdr, String txt) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(hdr); a.setHeaderText(null); a.setContentText(txt);
        a.showAndWait();
    }
}
