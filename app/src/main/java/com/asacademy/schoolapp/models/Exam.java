package com.asacademy.schoolapp.models;

import java.io.Serializable;

public class Exam implements Serializable {
    public int id;
    public String examName = "";
    public String term = "";
    public String startDate = "";
    public String endDate = "";
    public int totalMarks;
    public int passingMarks;
}
