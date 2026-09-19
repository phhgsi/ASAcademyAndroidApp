package com.asacademy.schoolapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.asacademy.schoolapp.adapters.FeeItemAdapter;
import com.asacademy.schoolapp.models.ApiResponses;
import com.asacademy.schoolapp.models.FeeSummary;
import com.asacademy.schoolapp.models.Student;
import com.asacademy.schoolapp.network.ApiClient;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CollectFeeActivity extends AppCompatActivity {

    private Student student;
    private ApiClient apiClient;

    private TextView tvStudentHeader, tvTotalAmount, tvTotalPaid, tvTotalBalance;
    private RecyclerView rvFeeItems;
    private FeeItemAdapter feeAdapter;

    private EditText etCollectAmount, etRemarks;
    private Spinner spPaymentMethod;
    private Button btnSubmitFee;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_fee);

        apiClient = ApiClient.getInstance(this);

        student = (Student) getIntent().getSerializableExtra("student");
        if (student == null) {
            Toast.makeText(this, "Student record missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadFeeSummary();
    }

    private void initViews() {
        tvStudentHeader = findViewById(R.id.tvStudentHeader);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvTotalPaid = findViewById(R.id.tvTotalPaid);
        tvTotalBalance = findViewById(R.id.tvTotalBalance);

        rvFeeItems = findViewById(R.id.rvFeeItems);
        rvFeeItems.setLayoutManager(new LinearLayoutManager(this));
        feeAdapter = new FeeItemAdapter(this);
        rvFeeItems.setAdapter(feeAdapter);

        etCollectAmount = findViewById(R.id.etCollectAmount);
        etRemarks = findViewById(R.id.etRemarks);
        spPaymentMethod = findViewById(R.id.spPaymentMethod);
        btnSubmitFee = findViewById(R.id.btnSubmitFee);
        progressBar = findViewById(R.id.progressBar);

        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{"Cash", "UPI / Online", "Cheque", "Bank Transfer"});
        spPaymentMethod.setAdapter(methodAdapter);

        tvStudentHeader.setText(student.fullName + " (" + student.scholarNumber + " • " + (student.className != null ? student.className : "") + ")");

        btnSubmitFee.setOnClickListener(v -> submitFeeCollection());
    }

    private void loadFeeSummary() {
        progressBar.setVisibility(View.VISIBLE);
        apiClient.get("api/mobile/fees/" + student.id, FeeSummary.class, new ApiClient.ApiCallback<FeeSummary>() {
            @Override
            public void onSuccess(FeeSummary result) {
                progressBar.setVisibility(View.GONE);
                if (result != null && result.summary != null) {
                    tvTotalAmount.setText(String.format(Locale.getDefault(), "₹%.0f", result.summary.totalAmount));
                    tvTotalPaid.setText(String.format(Locale.getDefault(), "₹%.0f", result.summary.totalPaid));
                    tvTotalBalance.setText(String.format(Locale.getDefault(), "₹%.0f", result.summary.totalBalance));

                    if (result.summary.totalBalance > 0) {
                        etCollectAmount.setText(String.format(Locale.getDefault(), "%.0f", result.summary.totalBalance));
                    }

                    feeAdapter.setFeeItems(result.fees);
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CollectFeeActivity.this, "Error loading fees: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void submitFeeCollection() {
        String amountStr = etCollectAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount to collect.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) throw new Exception();
        } catch (Exception e) {
            Toast.makeText(this, "Enter a valid positive fee amount.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmitFee.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("studentId", student.id);
        body.put("amount", amount);
        body.put("paymentMethod", spPaymentMethod.getSelectedItem().toString());
        body.put("remarks", etRemarks.getText().toString().trim());

        apiClient.post("api/mobile/fees/collect", body, ApiResponses.CollectFeeResponse.class, new ApiClient.ApiCallback<ApiResponses.CollectFeeResponse>() {
            @Override
            public void onSuccess(ApiResponses.CollectFeeResponse result) {
                progressBar.setVisibility(View.GONE);
                btnSubmitFee.setEnabled(true);
                if (result.success) {
                    showReceiptDialog(result, amount);
                    loadFeeSummary(); // Refresh
                } else {
                    Toast.makeText(CollectFeeActivity.this, result.message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                btnSubmitFee.setEnabled(true);
                Toast.makeText(CollectFeeActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showReceiptDialog(ApiResponses.CollectFeeResponse res, double amount) {
        androidx.appcompat.app.AlertDialog.Builder b = new androidx.appcompat.app.AlertDialog.Builder(this);
        b.setTitle("🎉 Fee Collection Successful");
        String msg = "Receipt No: " + res.receiptNumber + "\n"
                   + "Student: " + student.fullName + " (" + student.scholarNumber + ")\n"
                   + "Amount Paid: ₹" + String.format(Locale.getDefault(), "%.0f", amount) + "\n"
                   + "Payment Mode: " + spPaymentMethod.getSelectedItem().toString() + "\n"
                   + "Date: " + res.date;
        b.setMessage(msg);

        b.setPositiveButton("📲 Share on WhatsApp", (dialog, which) -> {
            if (student.mobile != null && !student.mobile.isEmpty()) {
                String cleanNum = student.mobile.replaceAll("[^0-9]", "");
                if (!cleanNum.startsWith("91") && cleanNum.length() == 10) {
                    cleanNum = "91" + cleanNum;
                }

                String waText = "🎓 *A.S. ACADEMY - FEE RECEIPT*\n\n"
                              + "Dear Parent,\n"
                              + "Fee payment for *" + student.fullName + "* has been successfully received.\n\n"
                              + "📋 *Receipt No:* " + res.receiptNumber + "\n"
                              + "💳 *Amount Paid:* ₹" + String.format(Locale.getDefault(), "%.0f", amount) + "\n"
                              + "🗓️ *Date:* " + res.date + "\n"
                              + "🏫 *Class:* " + (student.className != null ? student.className : "-") + "\n\n"
                              + "Thank you,\n*A.S. Academy H.S. School*";

                android.content.Intent waIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://api.whatsapp.com/send?phone=" + cleanNum + "&text=" + android.net.Uri.encode(waText)));
                startActivity(waIntent);
            } else {
                Toast.makeText(CollectFeeActivity.this, "No mobile number on file to share.", Toast.LENGTH_SHORT).show();
            }
        });

        b.setNegativeButton("Done", null);
        b.show();
    }
}
