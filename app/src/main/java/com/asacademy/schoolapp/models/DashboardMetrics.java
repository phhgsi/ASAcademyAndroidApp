package com.asacademy.schoolapp.models;

import java.io.Serializable;

public class DashboardMetrics implements Serializable {
    public boolean success;
    public MetricsData data;

    public static class MetricsData implements Serializable {
        public int totalStudents;
        public int primaryStudents;
        public int secondaryStudents;
        public double todayCollection;
        public int todayReceipts;
        public AttendanceOverview todayAttendance;
        public int totalArchive;
        public int totalStaff;
    }

    public static class AttendanceOverview implements Serializable {
        public int marked;
        public int present;
        public int absent;
        public double percentage;
    }
}
