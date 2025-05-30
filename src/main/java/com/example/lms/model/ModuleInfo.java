package com.example.lms.model;

public class ModuleInfo {
    public int moduleId;
    public int courseId;
    public String moduleName;
    public String courseName;

    public ModuleInfo(int moduleId, int courseId, String moduleName, String courseName) {
        this.moduleId = moduleId;
        this.courseId = courseId;
        this.moduleName = moduleName;
        this.courseName = courseName;
    }

    @Override
    public String toString() {
        return moduleName + " (" + courseName + ")";
    }
}
