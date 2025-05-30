package com.example.lms.model;

public class Module {
    private int id;
    private String name;
    private int year;
    private int semester;
    private int courseId;
    private String courseName; // Derived via JOIN

    public Module(int id, String name, int year, int semester, int courseId, String courseName) {
        this.id = id;
        this.name = name;
        this.year = year;
        this.semester = semester;
        this.courseId = courseId;
        this.courseName = courseName;
    }

    // Getters and Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public int getYear() { return year; }
    public int getSemester() { return semester; }
    public int getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setYear(int year) { this.year = year; }
    public void setSemester(int semester) { this.semester = semester; }
    public void setCourseId(int courseId) { this.courseId = courseId; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
}
