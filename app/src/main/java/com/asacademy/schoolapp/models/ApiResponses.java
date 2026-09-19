package com.asacademy.schoolapp.models;

import java.util.ArrayList;
import java.util.List;

public class ApiResponses {

    public static class SimpleResponse {
        public boolean success;
        public String message = "";
        public int id;
    }

    public static class BasicResponse extends SimpleResponse {}

    public static class LoginResponse {
        public boolean success;
        public String message = "";
        public User user;
    }

    public static class ClassListResponse {
        public boolean success;
        public List<SchoolClass> data = new ArrayList<>();
    }

    public static class StudentListResponse {
        public boolean success;
        public int count;
        public List<Student> data = new ArrayList<>();
    }

    public static class SingleStudentResponse {
        public boolean success;
        public Student data;
    }

    public static class PreviousStudentListResponse {
        public boolean success;
        public int count;
        public List<PreviousStudent> data = new ArrayList<>();
    }

    public static class PhotoUploadResponse {
        public boolean success;
        public String message = "";
        public String photoUrl = "";
    }

    public static class CollectFeeResponse {
        public boolean success;
        public String message = "";
        public String receiptNumber = "";
        public String studentName = "";
        public String date = "";
    }

    public static class ExamListResponse {
        public boolean success;
        public List<Exam> data = new ArrayList<>();
    }
}
