package com.example.lms.model;

public class StudentProgress {
    private String studentName;
    private String courseName;
    private int year;
    private int semester;
    private double averageGrade;

    public StudentProgress(String studentName, String courseName, int year, int semester, double averageGrade) {
        this.studentName = studentName;
        this.courseName = courseName;
        this.year = year;
        this.semester = semester;
        this.averageGrade = averageGrade;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getCourseName() {
        return courseName;
    }

    public int getYear() {
        return year;
    }

    public int getSemester() {
        return semester;
    }

    public double getAverageGrade() {
        return averageGrade;
    }
}
