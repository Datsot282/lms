package com.example.lms.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class Announcement {
    private final IntegerProperty id;
    private final StringProperty title;
    private final StringProperty content;
    private final ObjectProperty<LocalDateTime> postedAt;

    public Announcement(int id, String title, String content, LocalDateTime postedAt) {
        this.id       = new SimpleIntegerProperty(id);
        this.title    = new SimpleStringProperty(title);
        this.content  = new SimpleStringProperty(content);
        this.postedAt = new SimpleObjectProperty<>(postedAt);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getTitle() { return title.get(); }
    public StringProperty titleProperty() { return title; }

    public String getContent() { return content.get(); }
    public StringProperty contentProperty() { return content; }

    public LocalDateTime getPostedAt() { return postedAt.get(); }
    public ObjectProperty<LocalDateTime> postedAtProperty() { return postedAt; }
}
