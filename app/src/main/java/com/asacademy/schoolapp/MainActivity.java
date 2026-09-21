package com.asacademy.schoolapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.asacademy.schoolapp.adapters.ClassChipAdapter;
import com.asacademy.schoolapp.adapters.PreviousStudentAdapter;
import com.asacademy.schoolapp.adapters.StudentAdapter;
import com.asacademy.schoolapp.models.ApiResponses;
import com.asacademy.schoolapp.models.Exam;
import com.asacademy.schoolapp.models.PreviousStudent;
import com.asacademy.schoolapp.models.Student;
import com.asacademy.schoolapp.models.User;
import com.asacademy.schoolapp.network.ApiClient;

public class MainActivity extends AppCompatActivity implements
        StudentAdapter.OnStudentClickListener,
        PreviousStudentAdapter.OnPreviousStudentClickListener,
        ClassChipAdapter.OnClassSelectedListener {

    private static final int REQ_EDIT_STUDENT = 1001;

    private User currentUser;
    private ApiClient apiClient;

    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private EditText etSearch;
    private ImageView btnClearSearch;

    // Top Header Views
    private View btnServerConfigMain;
    private TextView tvServerUrlMain, tvSchoolNameMain, tvUserRoleGreeting;
    private ImageView btnQuickRefresh, btnLogoutMain;

    // View Containers for Tabs
    private View containerStudents, containerPrevious, containerExams;

    // Bottom Navigation Elements
    private LinearLayout navStudents, navPrevious, navFees, navExams;
    private ImageView ivNavStudents, ivNavPrevious, ivNavFees, ivNavExams;
    private TextView tvNavStudents, tvNavPrevious, tvNavFees, tvNavExams;

    // 1. Students Tab Components
    private RecyclerView rvClasses, rvStudents;
    private ClassChipAdapter classAdapter;
    private StudentAdapter studentAdapter;
    private int selectedClassId = 0;
    private String selectedActiveWing = "all";
    private TextView pillActiveWingAll, pillActiveWingPrimary, pillActiveWingSecondary;

    // Quick Action & Dashboard Stats Views
    private LinearLayout btnQuickPhotoDesk, btnQuickAttendance, btnQuickAddStudent, btnQuickStaff;
    private TextView tvStatStudents, tvStatTodayFee, tvStatAttendance;

    // 2. Previous Students Tab Components
    private RecyclerView rvPreviousStudents;
    private PreviousStudentAdapter previousAdapter;
    private String selectedArchiveStatus = "all";
    private String selectedArchiveWing = "all";
    private TextView pillAll, pillStudying, pillTc, pill12th;
    private TextView pillWingAll, pillWingPrimary, pillWingSecondary;

    // 3. Exams Tab Components
    private TextView tvExamsContent;

    private final com.asacademy.schoolapp.utils.PhotoUploadManager.UploadListener uploadListener = new com.asacademy.schoolapp.utils.PhotoUploadManager.UploadListener() {
        @Override
        public void onQueueProgress(int remainingCount, int uploadingCount, int completedCount) {}

        @Override
        public void onItemStatusChanged(com.asacademy.schoolapp.utils.PhotoUploadManager.UploadItem item) {
            if (item.status == com.asacademy.schoolapp.utils.PhotoUploadManager.Status.SUCCESS && item.serverPhotoUrl != null) {
                runOnUiThread(() -> {
                    if (item.isPreviousStudent) {
                        if (previousAdapter != null && previousAdapter.getStudents() != null) {
                            for (PreviousStudent ps : previousAdapter.getStudents()) {
                                if (ps.id == item.studentId) {
                                    ps.photoUrl = item.serverPhotoUrl;
                                    previousAdapter.updateStudent(ps);
                                    break;
                                }
                            }
                        }
                    } else {
                        if (studentAdapter != null && studentAdapter.getStudents() != null) {
                            for (Student s : studentAdapter.getStudents()) {
                                if (s.id == item.studentId) {
                                    s.photoUrl = item.serverPhotoUrl;
                                    studentAdapter.updateStudent(s);
                                    break;
                                }
                            }
                        }
                    }
                });
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiClient = ApiClient.getInstance(this);
        currentUser = (User) getIntent().getSerializableExtra("user");

        com.asacademy.schoolapp.utils.PhotoUploadManager.getInstance(this).registerListener(uploadListener);

        initViews();
        setupTopHeader();
        setupNavigation();
        setupSearch();

        // Load initial data & live dashboard stats
        loadClasses();
        loadStudents();
        loadDashboardMetrics();

        // 🚀 Check for GitHub Release updates automatically (silent check)
        com.asacademy.schoolapp.utils.AppUpdateManager.checkForUpdate(this, false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        com.asacademy.schoolapp.utils.PhotoUploadManager.getInstance(this).unregisterListener(uploadListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvServerUrlMain != null && apiClient != null) {
            tvServerUrlMain.setText(apiClient.getBaseUrl());
        }
        loadDashboardMetrics();
    }

    private void initViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);

        // Top Header
        btnServerConfigMain = findViewById(R.id.btnServerConfigMain);
        tvServerUrlMain = findViewById(R.id.tvServerUrlMain);
        tvSchoolNameMain = findViewById(R.id.tvSchoolNameMain);
        tvUserRoleGreeting = findViewById(R.id.tvUserRoleGreeting);
        btnQuickRefresh = findViewById(R.id.btnQuickRefresh);
        btnLogoutMain = findViewById(R.id.btnLogoutMain);

        // View Containers
        containerStudents = findViewById(R.id.containerStudents);
        containerPrevious = findViewById(R.id.containerPrevious);
        containerExams = findViewById(R.id.containerExams);

        // Bottom Navigation Views
        navStudents = findViewById(R.id.navStudents);
        navPrevious = findViewById(R.id.navPrevious);
        navFees = findViewById(R.id.navFees);
        navExams = findViewById(R.id.navExams);

        ivNavStudents = findViewById(R.id.ivNavStudents);
        ivNavPrevious = findViewById(R.id.ivNavPrevious);
        ivNavFees = findViewById(R.id.ivNavFees);
        ivNavExams = findViewById(R.id.ivNavExams);

        tvNavStudents = findViewById(R.id.tvNavStudents);
        tvNavPrevious = findViewById(R.id.tvNavPrevious);
        tvNavFees = findViewById(R.id.tvNavFees);
        tvNavExams = findViewById(R.id.tvNavExams);

        // 1. Students Tab RecyclerViews & Wing Pills
        pillActiveWingAll = findViewById(R.id.pillActiveWingAll);
        pillActiveWingPrimary = findViewById(R.id.pillActiveWingPrimary);
        pillActiveWingSecondary = findViewById(R.id.pillActiveWingSecondary);
        setupActiveWingPills();

        rvClasses = findViewById(R.id.rvClasses);
        rvClasses.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        classAdapter = new ClassChipAdapter(this, this);
        rvClasses.setAdapter(classAdapter);

        rvStudents = findViewById(R.id.rvStudents);
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        studentAdapter = new StudentAdapter(this, this);
        rvStudents.setAdapter(studentAdapter);

        // 2. Previous Students RecyclerView & Status Pills
        rvPreviousStudents = findViewById(R.id.rvPreviousStudents);
        rvPreviousStudents.setLayoutManager(new LinearLayoutManager(this));
        previousAdapter = new PreviousStudentAdapter(this, this);
        rvPreviousStudents.setAdapter(previousAdapter);

        pillAll = findViewById(R.id.pillAll);
        pillStudying = findViewById(R.id.pillStudying);
        pillTc = findViewById(R.id.pillTc);
        pill12th = findViewById(R.id.pill12th);

        pillWingAll = findViewById(R.id.pillWingAll);
        pillWingPrimary = findViewById(R.id.pillWingPrimary);
        pillWingSecondary = findViewById(R.id.pillWingSecondary);

        setupArchivePills();

        // 3. Exams View
        tvExamsContent = findViewById(R.id.tvExamsContent);

        // Quick Actions & Metrics
        btnQuickPhotoDesk = findViewById(R.id.btnQuickPhotoDesk);
        btnQuickAttendance = findViewById(R.id.btnQuickAttendance);
        btnQuickAddStudent = findViewById(R.id.btnQuickAddStudent);
        btnQuickStaff = findViewById(R.id.btnQuickStaff);

        tvStatStudents = findViewById(R.id.tvStatStudents);
        tvStatTodayFee = findViewById(R.id.tvStatTodayFee);
        tvStatAttendance = findViewById(R.id.tvStatAttendance);

        if (btnQuickPhotoDesk != null) {
            btnQuickPhotoDesk.setOnClickListener(v -> startActivity(new Intent(this, PhotoDeskActivity.class)));
        }
        if (btnQuickAttendance != null) {
            btnQuickAttendance.setOnClickListener(v -> startActivity(new Intent(this, AttendanceActivity.class)));
        }
        if (btnQuickAddStudent != null) {
            btnQuickAddStudent.setOnClickListener(v -> startActivityForResult(new Intent(this, AddStudentActivity.class), REQ_EDIT_STUDENT));
        }
        if (btnQuickStaff != null) {
            btnQuickStaff.setOnClickListener(v -> startActivity(new Intent(this, StaffDirectoryActivity.class)));
        }

        swipeRefresh.setOnRefreshListener(() -> {
            refreshCurrentTab();
            loadDashboardMetrics();
        });
    }

    private void setupTopHeader() {
        if (currentUser != null) {
            tvUserRoleGreeting.setText("Role: " + currentUser.role + " • " + currentUser.fullName);
        }
        if (tvServerUrlMain != null) {
            tvServerUrlMain.setText(apiClient.getBaseUrl());
        }

        // Top Server URL button triggers server change dialog directly
        if (btnServerConfigMain != null) {
            btnServerConfigMain.setOnClickListener(v -> showServerConfigDialog());
            btnServerConfigMain.setOnLongClickListener(v -> {
                com.asacademy.schoolapp.utils.AppUpdateManager.checkForUpdate(MainActivity.this, true);
                return true;
            });
        }

        View btnScanQrMain = findViewById(R.id.btnScanQrMain);
        if (btnScanQrMain != null) {
            btnScanQrMain.setOnClickListener(v -> launchQrScanner());
        }

        if (btnQuickRefresh != null) {
            btnQuickRefresh.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "🔄 Syncing latest data...", Toast.LENGTH_SHORT).show();
                refreshCurrentTab();
            });
            btnQuickRefresh.setOnLongClickListener(v -> {
                com.asacademy.schoolapp.utils.AppUpdateManager.checkForUpdate(MainActivity.this, true);
                return true;
            });
        }

        if (btnLogoutMain != null) {
            btnLogoutMain.setOnClickListener(v -> confirmLogout());
        }
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out of the school app?")
                .setPositiveButton("Logout", (d, w) -> {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupNavigation() {
        navStudents.setOnClickListener(v -> {
            highlightNav(1);
            showTab(containerStudents);
            loadStudents();
        });

        navPrevious.setOnClickListener(v -> {
            highlightNav(2);
            showTab(containerPrevious);
            loadPreviousStudents();
        });

        navFees.setOnClickListener(v -> {
            highlightNav(3);
            showTab(containerStudents);
            Toast.makeText(MainActivity.this, "💳 Tap on any student to collect fee & issue receipt", Toast.LENGTH_SHORT).show();
        });

        navExams.setOnClickListener(v -> {
            highlightNav(4);
            showTab(containerExams);
            loadExams();
        });
    }

    private void highlightNav(int tabIndex) {
        int colorActive = ContextCompat.getColor(this, R.color.m3_primary_light);
        int colorInactive = ContextCompat.getColor(this, R.color.m3_text_muted);

        ivNavStudents.setColorFilter(tabIndex == 1 ? colorActive : colorInactive);
        tvNavStudents.setTextColor(tabIndex == 1 ? colorActive : colorInactive);

        ivNavPrevious.setColorFilter(tabIndex == 2 ? colorActive : colorInactive);
        tvNavPrevious.setTextColor(tabIndex == 2 ? colorActive : colorInactive);

        ivNavFees.setColorFilter(tabIndex == 3 ? colorActive : colorInactive);
        tvNavFees.setTextColor(tabIndex == 3 ? colorActive : colorInactive);

        ivNavExams.setColorFilter(tabIndex == 4 ? colorActive : colorInactive);
        tvNavExams.setTextColor(tabIndex == 4 ? colorActive : colorInactive);
    }

    private void showTab(View tabToView) {
        containerStudents.setVisibility(View.GONE);
        containerPrevious.setVisibility(View.GONE);
        containerExams.setVisibility(View.GONE);
        tabToView.setVisibility(View.VISIBLE);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                }
                if (containerStudents.getVisibility() == View.VISIBLE) {
                    loadStudents();
                } else if (containerPrevious.getVisibility() == View.VISIBLE) {
                    loadPreviousStudents();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> etSearch.setText(""));
        }
    }

    private void setupArchivePills() {
        View.OnClickListener statusClick = v -> {
            pillAll.setBackgroundResource(R.drawable.bg_glass_pill_unselected);
            pillStudying.setBackgroundResource(R.drawable.bg_glass_pill_unselected);
            pillTc.setBackgroundResource(R.drawable.bg_glass_pill_unselected);
            pill12th.setBackgroundResource(R.drawable.bg_glass_pill_unselected);

            int id = v.getId();
            if (id == R.id.pillAll) {
                selectedArchiveStatus = "all";
                pillAll.setBackgroundResource(R.drawable.bg_glass_pill_selected);
            } else if (id == R.id.pillStudying) {
                selectedArchiveStatus = "study";
                pillStudying.setBackgroundResource(R.drawable.bg_glass_pill_selected);
            } else if (id == R.id.pillTc) {
                selectedArchiveStatus = "tc";
                pillTc.setBackgroundResource(R.drawable.bg_glass_pill_selected);
            } else if (id == R.id.pill12th) {
                selectedArchiveStatus = "pass";
                pill12th.setBackgroundResource(R.drawable.bg_glass_pill_selected);
            }
            loadPreviousStudents();
        };

        pillAll.setOnClickListener(statusClick);
        pillStudying.setOnClickListener(statusClick);
        pillTc.setOnClickListener(statusClick);
        pill12th.setOnClickListener(statusClick);

        View.OnClickListener wingClick = v -> {
            if (pillWingAll != null) pillWingAll.setBackgroundResource(R.drawable.bg_glass_pill_unselected);
            if (pillWingPrimary != null) pillWingPrimary.setBackgroundResource(R.drawable.bg_glass_pill_unselected);
            if (pillWingSecondary != null) pillWingSecondary.setBackgroundResource(R.drawable.bg_glass_pill_unselected);

            int id = v.getId();
            if (id == R.id.pillWingAll) {
                selectedArchiveWing = "all";
                if (pillWingAll != null) pillWingAll.setBackgroundResource(R.drawable.bg_glass_pill_selected);
            } else if (id == R.id.pillWingPrimary) {
                selectedArchiveWing = "P";
                if (pillWingPrimary != null) pillWingPrimary.setBackgroundResource(R.drawable.bg_glass_pill_selected);
            } else if (id == R.id.pillWingSecondary) {
                selectedArchiveWing = "S";
                if (pillWingSecondary != null) pillWingSecondary.setBackgroundResource(R.drawable.bg_glass_pill_selected);
            }
            loadPreviousStudents();
        };

        if (pillWingAll != null) pillWingAll.setOnClickListener(wingClick);
        if (pillWingPrimary != null) pillWingPrimary.setOnClickListener(wingClick);
        if (pillWingSecondary != null) pillWingSecondary.setOnClickListener(wingClick);
    }

    private void setupActiveWingPills() {
        View.OnClickListener wingClick = v -> {
            if (pillActiveWingAll != null) pillActiveWingAll.setBackgroundResource(R.drawable.bg_glass_pill_unselected);
            if (pillActiveWingPrimary != null) pillActiveWingPrimary.setBackgroundResource(R.drawable.bg_glass_pill_unselected);
            if (pillActiveWingSecondary != null) pillActiveWingSecondary.setBackgroundResource(R.drawable.bg_glass_pill_unselected);

            int id = v.getId();
            if (id == R.id.pillActiveWingAll) {
                selectedActiveWing = "all";
                if (pillActiveWingAll != null) pillActiveWingAll.setBackgroundResource(R.drawable.bg_glass_pill_selected);
            } else if (id == R.id.pillActiveWingPrimary) {
                selectedActiveWing = "P";
                if (pillActiveWingPrimary != null) pillActiveWingPrimary.setBackgroundResource(R.drawable.bg_glass_pill_selected);
            } else if (id == R.id.pillActiveWingSecondary) {
                selectedActiveWing = "S";
                if (pillActiveWingSecondary != null) pillActiveWingSecondary.setBackgroundResource(R.drawable.bg_glass_pill_selected);
            }
            selectedClassId = 0;
            if (classAdapter != null) classAdapter.setSelectedClassId(0);
            loadClasses();
            loadStudents();
        };

        if (pillActiveWingAll != null) pillActiveWingAll.setOnClickListener(wingClick);
        if (pillActiveWingPrimary != null) pillActiveWingPrimary.setOnClickListener(wingClick);
        if (pillActiveWingSecondary != null) pillActiveWingSecondary.setOnClickListener(wingClick);
    }

    private void refreshCurrentTab() {
        if (containerStudents.getVisibility() == View.VISIBLE) {
            loadClasses();
            loadStudents();
        } else if (containerPrevious.getVisibility() == View.VISIBLE) {
            loadPreviousStudents();
        } else if (containerExams.getVisibility() == View.VISIBLE) {
            loadExams();
        }
    }

    private void loadClasses() {
        String endpoint = "api/mobile/classes?wing=" + selectedActiveWing;
        apiClient.get(endpoint, ApiResponses.ClassListResponse.class, new ApiClient.ApiCallback<ApiResponses.ClassListResponse>() {
            @Override
            public void onSuccess(ApiResponses.ClassListResponse result) {
                if (result != null && result.success) {
                    classAdapter.setClasses(result.data);
                }
            }

            @Override
            public void onError(String errorMessage) {}
        });
    }

    private void loadStudents() {
        if (studentAdapter == null || studentAdapter.getItemCount() == 0) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            swipeRefresh.setRefreshing(true);
        }
        String search = etSearch.getText().toString().trim();
        String endpoint = "api/mobile/students?classId=" + selectedClassId + "&wing=" + selectedActiveWing + "&search=" + UriEncode(search);

        apiClient.get(endpoint, ApiResponses.StudentListResponse.class, new ApiClient.ApiCallback<ApiResponses.StudentListResponse>() {
            @Override
            public void onSuccess(ApiResponses.StudentListResponse result) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (result != null && result.success) {
                    studentAdapter.setStudents(result.data);
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPreviousStudents() {
        if (previousAdapter == null || previousAdapter.getItemCount() == 0) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            swipeRefresh.setRefreshing(true);
        }
        String search = etSearch.getText().toString().trim();
        String endpoint = "api/mobile/previous-students?status=" + selectedArchiveStatus + "&wing=" + selectedArchiveWing + "&search=" + UriEncode(search);

        apiClient.get(endpoint, ApiResponses.PreviousStudentListResponse.class, new ApiClient.ApiCallback<ApiResponses.PreviousStudentListResponse>() {
            @Override
            public void onSuccess(ApiResponses.PreviousStudentListResponse result) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (result != null && result.success) {
                    previousAdapter.setStudents(result.data);
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadExams() {
        progressBar.setVisibility(View.VISIBLE);
        apiClient.get("api/mobile/exams", ApiResponses.ExamListResponse.class, new ApiClient.ApiCallback<ApiResponses.ExamListResponse>() {
            @Override
            public void onSuccess(ApiResponses.ExamListResponse result) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (result != null && result.success && result.data != null && !result.data.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (Exam ex : result.data) {
                        sb.append("📋 ").append(ex.examName).append("\n");
                        sb.append("📅 Dates: ").append(ex.startDate).append(" to ").append(ex.endDate).append("\n");
                        sb.append("🎯 Max: ").append(ex.totalMarks).append(" | Pass: ").append(ex.passingMarks).append("\n\n");
                    }
                    tvExamsContent.setText(sb.toString().trim());
                } else {
                    tvExamsContent.setText("No active exams scheduled currently.");
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                tvExamsContent.setText("Error loading exams: " + errorMessage);
            }
        });
    }

    private String UriEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    @Override
    public void onClassSelected(int classId) {
        this.selectedClassId = classId;
        loadStudents();
    }

    @Override
    public void onStudentClick(Student student) {
        if (studentAdapter != null) {
            com.asacademy.schoolapp.utils.StudentNavigationManager.setActiveStudents(studentAdapter.getStudents(), student);
        }
        Intent intent = new Intent(this, StudentDetailActivity.class);
        intent.putExtra("student", student);
        startActivityForResult(intent, REQ_EDIT_STUDENT);
    }

    @Override
    public void onCameraClick(Student student) {
        if (studentAdapter != null) {
            com.asacademy.schoolapp.utils.StudentNavigationManager.setActiveStudents(studentAdapter.getStudents(), student);
        }
        Intent intent = new Intent(this, StudentDetailActivity.class);
        intent.putExtra("student", student);
        startActivityForResult(intent, REQ_EDIT_STUDENT);
    }

    @Override
    public void onPreviousStudentClick(PreviousStudent student) {
        if (previousAdapter != null) {
            com.asacademy.schoolapp.utils.StudentNavigationManager.setPreviousStudents(previousAdapter.getStudents(), student);
        }
        Intent intent = new Intent(this, PreviousStudentEditActivity.class);
        intent.putExtra("student", student);
        startActivityForResult(intent, REQ_EDIT_STUDENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_EDIT_STUDENT && resultCode == RESULT_OK) {
            boolean updatedInPlace = false;
            if (data != null) {
                if (data.hasExtra("updated_student")) {
                    Student s = (Student) data.getSerializableExtra("updated_student");
                    if (studentAdapter != null && s != null) {
                        studentAdapter.updateStudent(s);
                        updatedInPlace = true;
                    }
                }
                if (data.hasExtra("updated_previous_student")) {
                    PreviousStudent ps = (PreviousStudent) data.getSerializableExtra("updated_previous_student");
                    if (previousAdapter != null && ps != null) {
                        previousAdapter.updateStudent(ps);
                        updatedInPlace = true;
                    }
                }
            }
            if (!updatedInPlace) {
                refreshCurrentTab();
            }
        }
    }

    private final androidx.activity.result.ActivityResultLauncher<com.journeyapps.barcodescanner.ScanOptions> qrScannerLauncher = registerForActivityResult(
            new com.journeyapps.barcodescanner.ScanContract(),
            result -> {
                if (result.getContents() != null && !result.getContents().trim().isEmpty()) {
                    String scannedUrl = result.getContents().trim();
                    String cleanedUrl = ApiClient.cleanServerUrl(scannedUrl);
                    apiClient.setBaseUrl(cleanedUrl);
                    if (tvServerUrlMain != null) {
                        tvServerUrlMain.setText(apiClient.getBaseUrl());
                    }
                    Toast.makeText(this, "🟢 Connected to Server:\n" + cleanedUrl, Toast.LENGTH_LONG).show();
                    refreshCurrentTab();
                }
            }
    );

    private void launchQrScanner() {
        com.journeyapps.barcodescanner.ScanOptions options = new com.journeyapps.barcodescanner.ScanOptions();
        options.setPrompt("📷 Point camera at Server QR Code\n(Scan Wi-Fi or Cloudflare QR from Windows Screen)");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setCaptureActivity(CaptureActivityAnyOrientation.class);
        qrScannerLauncher.launch(options);
    }

    public void showServerConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🌐 Change Connected Server URL");
        builder.setMessage("Connect over Wi-Fi (e.g. http://192.168.1.8:5233) or anywhere on Internet (Cloudflare Live URL):");

        final EditText input = new EditText(this);
        input.setText(apiClient.getBaseUrl());
        input.setPadding(30, 20, 30, 20);
        builder.setView(input);

        builder.setPositiveButton("Save & Connect", (dialog, which) -> {
            String newUrl = input.getText().toString().trim();
            if (!newUrl.isEmpty()) {
                apiClient.setBaseUrl(newUrl);
                if (tvServerUrlMain != null) {
                    tvServerUrlMain.setText(apiClient.getBaseUrl());
                }
                Toast.makeText(MainActivity.this, "Connected to " + apiClient.getBaseUrl(), Toast.LENGTH_SHORT).show();
                refreshCurrentTab();
            }
        });
        builder.setNeutralButton("📷 Tools / 🚀 Update", (dialog, which) -> {
            CharSequence[] options = new CharSequence[]{"📷 Scan Server QR Code", "🚀 Check for App Updates (GitHub)"};
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Tools & Updates")
                    .setItems(options, (d, idx) -> {
                        if (idx == 0) {
                            launchQrScanner();
                        } else {
                            com.asacademy.schoolapp.utils.AppUpdateManager.checkForUpdate(MainActivity.this, true);
                        }
                    })
                    .show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadDashboardMetrics() {
        apiClient.get("api/mobile/dashboard", com.asacademy.schoolapp.models.DashboardMetrics.class, new ApiClient.ApiCallback<com.asacademy.schoolapp.models.DashboardMetrics>() {
            @Override
            public void onSuccess(com.asacademy.schoolapp.models.DashboardMetrics result) {
                if (result != null && result.data != null) {
                    if (tvStatStudents != null) {
                        tvStatStudents.setText("👥 Students: " + result.data.totalStudents);
                    }
                    if (tvStatTodayFee != null) {
                        tvStatTodayFee.setText(String.format(java.util.Locale.getDefault(), "💳 Today: ₹%.0f", result.data.todayCollection));
                    }
                    if (tvStatAttendance != null && result.data.todayAttendance != null) {
                        tvStatAttendance.setText(String.format(java.util.Locale.getDefault(), "📅 Att: %.0f%%", result.data.todayAttendance.percentage));
                    }
                }
            }

            @Override
            public void onError(String errorMessage) { }
        });
    }
}
