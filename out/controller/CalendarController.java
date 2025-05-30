package com.example.lms.controller;

import com.example.lms.connection.DBConnection;
import com.example.lms.model.CalendarEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CalendarController {

    @FXML private TableView<CalendarEntry> calendarTable;
    @FXML private TableColumn<CalendarEntry, Integer>    idCol;
    @FXML private TableColumn<CalendarEntry, LocalDate>  dateCol;
    @FXML private TableColumn<CalendarEntry, String>     titleCol;
    @FXML private TableColumn<CalendarEntry, String>     descCol;
    @FXML private TableColumn<CalendarEntry, Void>       actionCol;

    private final ObservableList<CalendarEntry> entries = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idCol   .setCellValueFactory(new PropertyValueFactory<>("id"));
        dateCol .setCellValueFactory(new PropertyValueFactory<>("eventDate"));
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        descCol .setCellValueFactory(new PropertyValueFactory<>("description"));

        actionCol.setCellFactory(col -> new TableCell<CalendarEntry, Void>() {
            private final Button editBtn   = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            {
                editBtn.setOnAction(e -> {
                    CalendarEntry ce = getTableView().getItems().get(getIndex());
                    showDialog(ce, true);
                });
                deleteBtn.setOnAction(e -> {
                    CalendarEntry ce = getTableView().getItems().get(getIndex());
                    deleteEntry(ce);
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : new HBox(5, editBtn, deleteBtn));
            }
        });

        loadEntries();
    }

    private void loadEntries() {
        entries.clear();
        String sql = "SELECT id,event_date,title,description,created_at FROM users.calendar ORDER BY event_date";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {

            while (rs.next()) {
                entries.add(new CalendarEntry(
                        rs.getInt("id"),
                        rs.getDate("event_date").toLocalDate(),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error","Cannot load calendar.");
        }
        calendarTable.setItems(entries);
    }

    @FXML
    private void handleAddEvent() {
        showDialog(null, false);
    }

    private void showDialog(CalendarEntry existing, boolean isEdit) {
        Dialog<CalendarEntry> dlg = new Dialog<>();
        dlg.setTitle(isEdit ? "Edit Event" : "Add Event");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        DatePicker   datePicker = new DatePicker();
        TextField    titleF     = new TextField();
        TextArea     descA      = new TextArea(); descA.setPrefRowCount(3);
        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);
        form.add(new Label("Date:"),        0,0); form.add(datePicker,1,0);
        form.add(new Label("Title:"),       0,1); form.add(titleF,    1,1);
        form.add(new Label("Description:"), 0,2); form.add(descA,     1,2);

        if (isEdit && existing!=null) {
            datePicker.setValue(existing.getEventDate());
            titleF    .setText(existing.getTitle());
            descA     .setText(existing.getDescription());
        }

        dlg.getDialogPane().setContent(form);

        dlg.setResultConverter(btn -> {
            if (btn==ButtonType.OK) {
                LocalDate dt = datePicker.getValue();
                String    t  = titleF.getText().trim();
                String    d  = descA.getText().trim();
                if (dt==null||t.isEmpty()) {
                    showAlert("Validation","Date and Title are required.");
                    return null;
                }
                if (isEdit) {
                    updateEntry(existing.getId(), dt, t, d);
                    existing.eventDateProperty().set(dt);
                    existing.titleProperty().set(t);
                    existing.descriptionProperty().set(d);
                    return existing;
                } else {
                    CalendarEntry ce = new CalendarEntry(0, dt, t, d, LocalDateTime.now());
                    insertEntry(ce);
                    return ce;
                }
            }
            return null;
        });

        dlg.showAndWait().ifPresent(ce->{
            if (!isEdit) entries.add(ce);
            calendarTable.refresh();
        });
    }

    private void insertEntry(CalendarEntry ce) {
        String sql = "INSERT INTO users.calendar(event_date,title,description,created_at) VALUES(?,?,?,NOW())";
        try (Connection c=DBConnection.getConnection();
             PreparedStatement p=c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setDate(1, Date.valueOf(ce.getEventDate()));
            p.setString(2,ce.getTitle());
            p.setString(3,ce.getDescription());
            p.executeUpdate();
            try (ResultSet k = p.getGeneratedKeys()) {
                if (k.next()) ce.idProperty().set(k.getInt(1));
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            showAlert("Error","Failed to add event.");
        }
        loadEntries();
    }

    private void updateEntry(int id, LocalDate dt, String t, String d) {
        String sql = "UPDATE users.calendar SET event_date=?,title=?,description=? WHERE id=?";
        try (Connection c=DBConnection.getConnection();
             PreparedStatement p=c.prepareStatement(sql)) {
            p.setDate(1,Date.valueOf(dt));
            p.setString(2,t);
            p.setString(3,d);
            p.setInt(4,id);
            p.executeUpdate();
        } catch(SQLException ex){
            ex.printStackTrace();
            showAlert("Error","Failed to update event.");
        }
        loadEntries();
    }

    private void deleteEntry(CalendarEntry ce) {
        String sql = "DELETE FROM users.calendar WHERE id=?";
        try (Connection c=DBConnection.getConnection();
             PreparedStatement p=c.prepareStatement(sql)) {
            p.setInt(1, ce.getId());
            p.executeUpdate();
            entries.remove(ce);
        } catch(SQLException ex){
            ex.printStackTrace();
            showAlert("Error","Failed to delete event.");
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
