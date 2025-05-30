package com.example.lms.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Result {
    private int id;
    private String studentName;
    private String courseName;
    private String moduleName;
    private String term;
    private String publishedAt;
    private Map<String, String> grades;
    private String finalGrade;
    public String assignment;

    public Result(int id, String studentName, String courseName, String moduleName, String term, String publishedAt, String assignment) {
        this.id = id;
        this.studentName = studentName;
        this.courseName = courseName;
        this.moduleName = moduleName;
        this.term = term;
        this.publishedAt = publishedAt;
        this.grades = new HashMap<>();
        this.finalGrade = "";
        this.assignment = assignment;
    }

    public void addGrade(String title, String grade) {
        grades.put(title, grade);
    }

    public void computeFinalGrade() {
        int total = 0;
        int count = 0;
        for (String value : grades.values()) {
            try {
                total += Integer.parseInt(value);
                count++;
            } catch (NumberFormatException ignored) {
            }
        }
        this.finalGrade = (count > 0) ? String.valueOf(total / count) : "N/A";
    }


    public String getAssignmentTitles() {
        return String.valueOf(new ArrayList<>(grades.keySet()));
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getTerm() {
        return term;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public String getFinalGrade() {
        return finalGrade;
    }

    public Map<String, String> getGrades() {
        return grades;
    }

    public void setFinalGrade(String grade) {
        this.finalGrade = grade;
    }

}
