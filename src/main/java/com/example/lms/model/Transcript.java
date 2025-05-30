package com.example.lms.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class Transcript {
   // private final SimpleIntegerProperty studentId;
    private final SimpleStringProperty studentName;
    private final SimpleStringProperty courseName;
    private final SimpleStringProperty moduleName;
    private final SimpleStringProperty assignmentTitle;
    private final SimpleStringProperty submissionDate;
    private final SimpleStringProperty grade;

    public Transcript(String studentName, String courseName,
                      String moduleName, String assignmentTitle, String submissionDate, String grade) {
        //this.studentId = new SimpleIntegerProperty(studentId);
        this.studentName = new SimpleStringProperty(studentName);
        this.courseName = new SimpleStringProperty(courseName);
        this.moduleName = new SimpleStringProperty(moduleName);
        this.assignmentTitle = new SimpleStringProperty(assignmentTitle);
        this.submissionDate = new SimpleStringProperty(submissionDate);
        this.grade = new SimpleStringProperty(grade);
    }

    //public int getStudentId() { return studentId.get(); }
    public String getStudentName() { return studentName.get(); }
    public String getCourseName() { return courseName.get(); }
    public String getModuleName() { return moduleName.get(); }
    public String getAssignmentTitle() { return assignmentTitle.get(); }
    public String getSubmissionDate() { return submissionDate.get(); }
    public String getGrade() { return grade.get(); }
}
