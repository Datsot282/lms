package com.example.lms.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class Material {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty filePath = new SimpleStringProperty();
    private final StringProperty fileType = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> uploadedAt = new SimpleObjectProperty<>();

    public Material(int id, String title, String description, String filePath, String fileType, LocalDateTime uploadedAt) {
        this.id.set(id);
        this.title.set(title);
        this.description.set(description);
        this.filePath.set(filePath);
        this.fileType.set(fileType);
        this.uploadedAt.set(uploadedAt);
    }

    public int getId() { return id.get(); }
    public String getTitle() { return title.get(); }
    public String getDescription() { return description.get(); }
    public String getFilePath() { return filePath.get(); }
    public String getFileType() { return fileType.get(); }
    public LocalDateTime getUploadedAt() { return uploadedAt.get(); }

    public IntegerProperty idProperty() { return id; }
    public StringProperty titleProperty() { return title; }
    public StringProperty descriptionProperty() { return description; }
    public StringProperty filePathProperty() { return filePath; }
    public StringProperty fileTypeProperty() { return fileType; }
    public ObjectProperty<LocalDateTime> uploadedAtProperty() { return uploadedAt; }
}
