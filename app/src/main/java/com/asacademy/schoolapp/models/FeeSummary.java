package com.asacademy.schoolapp.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FeeSummary implements Serializable {
    public StudentInfo student = new StudentInfo();
    public SummaryInfo summary = new SummaryInfo();
    public List<FeeItem> fees = new ArrayList<>();
    public List<PaymentItem> payments = new ArrayList<>();

    public static class StudentInfo implements Serializable {
        public int id;
        public String scholarNumber = "";
        public String fullName = "";
        public String className = "";
    }

    public static class SummaryInfo implements Serializable {
        public double totalAmount;
        public double totalPaid;
        public double totalBalance;
    }

    public static class FeeItem implements Serializable {
        public int id;
        public String headName = "";
        public double amount;
        public double paidAmount;
        public double balance;
        public boolean isPaid;
        public String dueDate = "";
    }

    public static class PaymentItem implements Serializable {
        public int id;
        public String receiptNumber = "";
        public double amount;
        public String paymentDate = "";
        public String paymentMethod = "";
        public String headName = "";
    }
}
