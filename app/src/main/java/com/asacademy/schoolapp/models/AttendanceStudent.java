package com.asacademy.schoolapp.models;

import java.io.Serializable;
import java.util.List;

public class AttendanceStudent implements Serializable {
    public int studentId;
    public String scholarNumber;
    public Integer rollNumber;
    public String name;
    public String nameHindi;
    public String photoUrl;
    public String status; // "Present", "Absent", "Leave"

    public static class Response implements Serializable {
        public boolean success;
        public String date;
        public int classId;
        public int totalStudents;
        public List<AttendanceStudent> data;
    }
}
