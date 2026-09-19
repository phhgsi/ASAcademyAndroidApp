package com.asacademy.schoolapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.asacademy.schoolapp.adapters.AttendanceAdapter;
import com.asacademy.schoolapp.adapters.ClassChipAdapter;
import com.asacademy.schoolapp.models.ApiResponses;
import com.asacademy.schoolapp.models.AttendanceStudent;
import com.asacademy.schoolapp.models.SchoolClass;
import com.asacademy.schoolapp.network.ApiClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceActivity extends AppCompatActivity implements ClassChipAdapter.OnClassSelectedListener {

    private ApiClient apiClient;
    private RecyclerView rvClasses, rvStudents;
    private ClassChipAdapter classAdapter;
    private AttendanceAdapter attendanceAdapter;
    private ProgressBar progressBar;
    private TextView tvAttendanceSubHeader, tvAttendanceCount, btnPickDate;
    private TextView btnMarkAllPresent, btnMarkAllAbsent;
    private Button btnSaveAttendance;
    private ImageView btnBackAttendance;

    private int selectedClassId = 0;
    private String selectedClassName = "";
    private Calendar selectedDate = Calendar.getInstance();
    private final SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        apiClient = ApiClient.getInstance(this);

        initViews();
        loadClasses();
    }

    private void initViews() {
        btnBackAttendance = findViewById(R.id.btnBackAttendance);
        tvAttendanceSubHeader = findViewById(R.id.tvAttendanceSubHeader);
        tvAttendanceCount = findViewById(R.id.tvAttendanceCount);
        btnPickDate = findViewById(R.id.btnPickDate);
        progressBar = findViewById(R.id.pbAttendance);

        btnMarkAllPresent = findViewById(R.id.btnMarkAllPresent);
        btnMarkAllAbsent = findViewById(R.id.btnMarkAllAbsent);
        btnSaveAttendance = findViewById(R.id.btnSaveAttendance);

        rvClasses = findViewById(R.id.rvAttendanceClasses);
        rvClasses.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        classAdapter = new ClassChipAdapter(this, this);
        rvClasses.setAdapter(classAdapter);

        rvStudents = findViewById(R.id.rvAttendanceStudents);
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        attendanceAdapter = new AttendanceAdapter(this);
        rvStudents.setAdapter(attendanceAdapter);

        btnBackAttendance.setOnClickListener(v -> finish());

        btnPickDate.setText(displayDateFormat.format(selectedDate.getTime()));
        btnPickDate.setOnClickListener(v -> showDatePicker());

        btnMarkAllPresent.setOnClickListener(v -> attendanceAdapter.markAll("Present"));
        btnMarkAllAbsent.setOnClickListener(v -> attendanceAdapter.markAll("Absent"));

        btnSaveAttendance.setOnClickListener(v -> saveAttendance());
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            btnPickDate.setText(displayDateFormat.format(selectedDate.getTime()));
            if (selectedClassId > 0) {
                loadStudentsForAttendance();
            }
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadClasses() {
        apiClient.get("api/mobile/classes", ApiResponses.ClassListResponse.class, new ApiClient.ApiCallback<ApiResponses.ClassListResponse>() {
            @Override
            public void onSuccess(ApiResponses.ClassListResponse result) {
                if (result != null && result.data != null && !result.data.isEmpty()) {
                    classAdapter.setClasses(result.data);
                    // Automatically select first class
                    if (!result.data.isEmpty()) {
                        onClassSelected(result.data.get(0).id);
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AttendanceActivity.this, "Error loading classes: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClassSelected(int classId) {
        selectedClassId = classId;
        classAdapter.setSelectedClassId(classId);
        selectedClassName = "Class " + classId;
        tvAttendanceSubHeader.setText(selectedClassName + " • " + displayDateFormat.format(selectedDate.getTime()));
        loadStudentsForAttendance();
    }

    private void loadStudentsForAttendance() {
        if (selectedClassId <= 0) return;

        progressBar.setVisibility(View.VISIBLE);
        String dateStr = apiDateFormat.format(selectedDate.getTime());
        String url = "api/mobile/attendance/students?classId=" + selectedClassId + "&date=" + dateStr;

        apiClient.get(url, AttendanceStudent.Response.class, new ApiClient.ApiCallback<AttendanceStudent.Response>() {
            @Override
            public void onSuccess(AttendanceStudent.Response result) {
                progressBar.setVisibility(View.GONE);
                if (result != null && result.data != null) {
                    attendanceAdapter.setStudents(result.data);
                    tvAttendanceCount.setText(result.data.size() + " Students");
                } else {
                    attendanceAdapter.setStudents(new ArrayList<>());
                    tvAttendanceCount.setText("0 Students");
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AttendanceActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAttendance() {
        List<AttendanceStudent> list = attendanceAdapter.getStudents();
        if (list.isEmpty()) {
            Toast.makeText(this, "No students to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSaveAttendance.setEnabled(false);

        List<Map<String, Object>> items = new ArrayList<>();
        for (AttendanceStudent s : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("studentId", s.studentId);
            item.put("status", s.status != null ? s.status : "Present");
            items.add(item);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("classId", selectedClassId);
        payload.put("date", apiDateFormat.format(selectedDate.getTime()));
        payload.put("items", items);

        apiClient.post("api/mobile/attendance/save-batch", payload, ApiResponses.BasicResponse.class, new ApiClient.ApiCallback<ApiResponses.BasicResponse>() {
            @Override
            public void onSuccess(ApiResponses.BasicResponse result) {
                progressBar.setVisibility(View.GONE);
                btnSaveAttendance.setEnabled(true);
                if (result != null && result.success) {
                    Toast.makeText(AttendanceActivity.this, "✅ " + result.message, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(AttendanceActivity.this, "Error saving: " + (result != null ? result.message : "Unknown"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                btnSaveAttendance.setEnabled(true);
                Toast.makeText(AttendanceActivity.this, "Failed to save attendance: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
