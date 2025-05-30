package com.example.lms.model;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CalendarEntry {
    private final IntegerProperty id;
    private final ObjectProperty<LocalDate> eventDate;
    private final StringProperty title;
    private final StringProperty description;
    private final ObjectProperty<LocalDateTime> createdAt;

    public CalendarEntry(int id, LocalDate eventDate, String title, String description, LocalDateTime createdAt) {
        this.id = new SimpleIntegerProperty(id);
        this.eventDate = new SimpleObjectProperty<>(eventDate);
        this.title = new SimpleStringProperty(title);
        this.description = new SimpleStringProperty(description);
        this.createdAt = new SimpleObjectProperty<>(createdAt);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public LocalDate getEventDate() { return eventDate.get(); }
    public ObjectProperty<LocalDate> eventDateProperty() { return eventDate; }

    public String getTitle() { return title.get(); }
    public StringProperty titleProperty() { return title; }

    public String getDescription() { return description.get(); }
    public StringProperty descriptionProperty() { return description; }

    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }
}
