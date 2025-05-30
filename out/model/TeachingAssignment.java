package com.example.lms.model;

public class TeachingAssignment {
    private String lecturerName;
    private String courseName;
    private String moduleName;
    private String studentName;
    private String studentModule;

    public TeachingAssignment(String lecturerName, String courseName, String moduleName,
                              String studentName, String studentModule) {
        this.lecturerName = lecturerName;
        this.courseName = courseName;
        this.moduleName = moduleName;
        this.studentName = studentName;
        this.studentModule = studentModule;
    }

    public String getLecturerName() { return lecturerName; }
    public String getCourseName() { return courseName; }
    public String getModuleName() { return moduleName; }
    public String getStudentName() { return studentName; }
    public String getStudentModule() { return studentModule; }
}
