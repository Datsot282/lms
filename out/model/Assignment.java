package com.example.lms.model;

import java.time.LocalDateTime;

public class Assignment {
    private int id;
    private String title;
    private String description;
    private String filePath;
    private LocalDateTime deadline;
    private String uploadedAt;
    private String grade;


    public Assignment(int id, String title, String description, String filePath, LocalDateTime deadline, String uploadedAt, String grade) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.filePath = filePath;
        this.deadline = deadline;
        this.uploadedAt = uploadedAt;
        this.grade = grade;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getFilePath() { return filePath; }
    public LocalDateTime getDeadline() { return deadline; }
    public String getUploadedAt() { return uploadedAt; }
    public String getGrade() { return grade; }

}
