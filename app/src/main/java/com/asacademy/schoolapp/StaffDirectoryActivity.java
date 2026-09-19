package com.asacademy.schoolapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.asacademy.schoolapp.adapters.StaffAdapter;
import com.asacademy.schoolapp.models.StaffMember;
import com.asacademy.schoolapp.network.ApiClient;

import java.util.ArrayList;

public class StaffDirectoryActivity extends AppCompatActivity {

    private ApiClient apiClient;
    private RecyclerView rvStaffList;
    private StaffAdapter staffAdapter;
    private ProgressBar progressBar;
    private TextView tvStaffSubHeader;
    private ImageView btnBackStaff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_directory);

        apiClient = ApiClient.getInstance(this);

        initViews();
        loadStaffList();
    }

    private void initViews() {
        btnBackStaff = findViewById(R.id.btnBackStaff);
        tvStaffSubHeader = findViewById(R.id.tvStaffSubHeader);
        progressBar = findViewById(R.id.pbStaff);

        rvStaffList = findViewById(R.id.rvStaffList);
        rvStaffList.setLayoutManager(new LinearLayoutManager(this));
        staffAdapter = new StaffAdapter(this);
        rvStaffList.setAdapter(staffAdapter);

        btnBackStaff.setOnClickListener(v -> finish());
    }

    private void loadStaffList() {
        progressBar.setVisibility(View.VISIBLE);
        apiClient.get("api/mobile/staff", StaffMember.Response.class, new ApiClient.ApiCallback<StaffMember.Response>() {
            @Override
            public void onSuccess(StaffMember.Response result) {
                progressBar.setVisibility(View.GONE);
                if (result != null && result.data != null) {
                    staffAdapter.setStaff(result.data);
                    tvStaffSubHeader.setText(result.data.size() + " Faculty & Staff Members");
                } else {
                    staffAdapter.setStaff(new ArrayList<>());
                    tvStaffSubHeader.setText("0 Staff Members");
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(StaffDirectoryActivity.this, "Error loading staff: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
