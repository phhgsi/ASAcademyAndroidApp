package com.asacademy.schoolapp.models;

import java.io.Serializable;
import java.util.List;

public class StaffMember implements Serializable {
    public int id;
    public String employeeCode;
    public String fullName;
    public String designation;
    public String department;
    public String qualification;
    public String mobile;
    public String email;
    public String photoUrl;

    public static class Response implements Serializable {
        public boolean success;
        public List<StaffMember> data;
    }
}
