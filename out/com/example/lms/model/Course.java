package com.example.lms.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class Course {
    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty description;
    private final IntegerProperty credits;
    private final ObjectProperty<LocalDateTime> createdAt;

    public Course(int id, String name, String description, int credits, LocalDateTime createdAt) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.description = new SimpleStringProperty(description);
        this.credits = new SimpleIntegerProperty(credits);
        this.createdAt = new SimpleObjectProperty<>(createdAt);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getDescription() { return description.get(); }
    public StringProperty descriptionProperty() { return description; }

    public int getCredits() { return credits.get(); }
    public IntegerProperty creditsProperty() { return credits; }

    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }
}
