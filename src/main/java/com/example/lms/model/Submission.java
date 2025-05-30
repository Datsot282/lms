package com.example.lms.model;

public class Submission {
    private int id;
    private int assignmentId;
    private String studentName;
    private String filePath;
    private String submittedAt;
    private String grade;


    public Submission(int id, int assignmentId, String studentName, String filePath, String submittedAt, String grade) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.studentName = studentName;
        this.filePath = filePath;
        this.submittedAt = submittedAt;
        this.grade = grade;
    }

    public int getId() { return id; }
    public int getAssignmentId() { return assignmentId; }
    public String getStudentName() { return studentName; }
    public String getFilePath() { return filePath; }
    public String getSubmittedAt() { return submittedAt; }
    public String getGrade() { return grade; }

    public void setGrade(String grade) { this.grade = grade; }
}
