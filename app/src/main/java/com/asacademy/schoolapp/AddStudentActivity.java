package com.asacademy.schoolapp;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.asacademy.schoolapp.models.ApiResponses;
import com.asacademy.schoolapp.models.SchoolClass;
import com.asacademy.schoolapp.network.ApiClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddStudentActivity extends AppCompatActivity {

    private ApiClient apiClient;

    // Wing & Suggestion
    private TextView btnWingP, btnWingS, tvScholarSuggestion;
    private String currentWing = "P";

    // Form inputs
    private EditText etAddScholar, etAddRollNumber;
    private Spinner spAddClass, spAddGender, spAddCategory, spAddBloodGroup;
    private TextView btnAdmissionDate, btnDobPicker;
    private EditText etAddFirstName, etAddMiddleName, etAddLastName, etAddNameHindi;
    private EditText etAddFatherName, etAddFatherHindi, etAddMotherName, etAddMotherHindi, etAddMobile;
    private EditText etAddSssmid, etAddAadhaar, etAddVillage, etAddAddress;
    private EditText etAddPrevSchool, etAddBankAccount, etAddBankName, etAddIfsc;

    private Button btnSubmitAdmission, btnTopSave;
    private ProgressBar progressBar;
    private ImageView btnBackAddStudent;

    private List<SchoolClass> classList = new ArrayList<>();
    private final Calendar dobCalendar = Calendar.getInstance();
    private final Calendar admCalendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private boolean isDobSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);

        apiClient = ApiClient.getInstance(this);

        initViews();
        setupSpinners();
        loadClasses();

        // Fetch initial next scholar number for Primary Wing
        fetchNextScholarNumber("P");
    }

    private void initViews() {
        btnBackAddStudent = findViewById(R.id.btnBackAddStudent);
        btnTopSave = findViewById(R.id.btnTopSave);
        btnSubmitAdmission = findViewById(R.id.btnSubmitAdmission);
        progressBar = findViewById(R.id.pbAddStudent);

        btnWingP = findViewById(R.id.btnWingP);
        btnWingS = findViewById(R.id.btnWingS);
        tvScholarSuggestion = findViewById(R.id.tvScholarSuggestion);

        etAddScholar = findViewById(R.id.etAddScholar);
        etAddRollNumber = findViewById(R.id.etAddRollNumber);
        spAddClass = findViewById(R.id.spAddClass);
        btnAdmissionDate = findViewById(R.id.btnAdmissionDate);
        btnDobPicker = findViewById(R.id.btnDobPicker);

        etAddFirstName = findViewById(R.id.etAddFirstName);
        etAddMiddleName = findViewById(R.id.etAddMiddleName);
        etAddLastName = findViewById(R.id.etAddLastName);
        etAddNameHindi = findViewById(R.id.etAddNameHindi);

        spAddGender = findViewById(R.id.spAddGender);
        spAddCategory = findViewById(R.id.spAddCategory);
        spAddBloodGroup = findViewById(R.id.spAddBloodGroup);

        etAddFatherName = findViewById(R.id.etAddFatherName);
        etAddFatherHindi = findViewById(R.id.etAddFatherHindi);
        etAddMotherName = findViewById(R.id.etAddMotherName);
        etAddMotherHindi = findViewById(R.id.etAddMotherHindi);
        etAddMobile = findViewById(R.id.etAddMobile);

        etAddSssmid = findViewById(R.id.etAddSssmid);
        etAddAadhaar = findViewById(R.id.etAddAadhaar);
        etAddVillage = findViewById(R.id.etAddVillage);
        etAddAddress = findViewById(R.id.etAddAddress);

        etAddPrevSchool = findViewById(R.id.etAddPrevSchool);
        etAddBankAccount = findViewById(R.id.etAddBankAccount);
        etAddBankName = findViewById(R.id.etAddBankName);
        etAddIfsc = findViewById(R.id.etAddIfsc);

        btnBackAddStudent.setOnClickListener(v -> finish());
        btnTopSave.setOnClickListener(v -> submitAdmission());
        btnSubmitAdmission.setOnClickListener(v -> submitAdmission());

        // Wing Selection Handlers
        btnWingP.setOnClickListener(v -> {
            setWing("P");
            fetchNextScholarNumber("P");
        });

        btnWingS.setOnClickListener(v -> {
            setWing("S");
            fetchNextScholarNumber("S");
        });

        // Live typing in Scholar Box: Typing 'p' or 's' auto assigns next scholar
        etAddScholar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null) {
                    String str = s.toString().trim();
                    if (str.equalsIgnoreCase("p") && str.length() == 1) {
                        setWing("P");
                        fetchNextScholarNumber("P");
                    } else if (str.equalsIgnoreCase("s") && str.length() == 1) {
                        setWing("S");
                        fetchNextScholarNumber("S");
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Admission Date Picker
        btnAdmissionDate.setText(displayDateFormat.format(admCalendar.getTime()) + " 📅");
        btnAdmissionDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                admCalendar.set(Calendar.YEAR, year);
                admCalendar.set(Calendar.MONTH, month);
                admCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                btnAdmissionDate.setText(displayDateFormat.format(admCalendar.getTime()) + " 📅");
            }, admCalendar.get(Calendar.YEAR), admCalendar.get(Calendar.MONTH), admCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Date of Birth Picker with Age preview
        btnDobPicker.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                dobCalendar.set(Calendar.YEAR, year);
                dobCalendar.set(Calendar.MONTH, month);
                dobCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                isDobSelected = true;

                int age = Calendar.getInstance().get(Calendar.YEAR) - year;
                btnDobPicker.setText(displayDateFormat.format(dobCalendar.getTime()) + " (" + age + " yrs) 📅");
            }, 2014, 0, 1).show();
        });
    }

    private void setWing(String wing) {
        currentWing = wing;
        if ("S".equalsIgnoreCase(wing)) {
            btnWingS.setBackgroundResource(R.drawable.bg_chip_selected);
            btnWingS.setTextColor(Color.WHITE);
            btnWingP.setBackgroundResource(R.drawable.bg_glass_pill_unselected);
            btnWingP.setTextColor(Color.parseColor("#94A3B8"));
        } else {
            btnWingP.setBackgroundResource(R.drawable.bg_chip_selected);
            btnWingP.setTextColor(Color.WHITE);
            btnWingS.setBackgroundResource(R.drawable.bg_glass_pill_unselected);
            btnWingS.setTextColor(Color.parseColor("#94A3B8"));
        }
    }

    private void setupSpinners() {
        // Gender
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{"Male (पुरुष)", "Female (महिला)", "Other"});
        spAddGender.setAdapter(genderAdapter);

        // Category
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{"GEN (General)", "OBC (पिछड़ा वर्ग)", "SC (अनुसूचित जाति)", "ST (अनुसूचित जनजाति)"});
        spAddCategory.setAdapter(catAdapter);

        // Blood Group
        ArrayAdapter<String> bloodAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{"Select Blood", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"});
        spAddBloodGroup.setAdapter(bloodAdapter);
    }

    private void loadClasses() {
        apiClient.get("api/mobile/classes", ApiResponses.ClassListResponse.class, new ApiClient.ApiCallback<ApiResponses.ClassListResponse>() {
            @Override
            public void onSuccess(ApiResponses.ClassListResponse result) {
                if (result != null && result.data != null && !result.data.isEmpty()) {
                    classList = result.data;
                    List<String> names = new ArrayList<>();
                    for (SchoolClass c : classList) {
                        names.add(c.displayName);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(AddStudentActivity.this,
                            android.R.layout.simple_spinner_dropdown_item, names);
                    spAddClass.setAdapter(adapter);

                    spAddClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position >= 0 && position < classList.size()) {
                                SchoolClass selected = classList.get(position);
                                String name = selected.displayName.toLowerCase();
                                if (name.contains("9") || name.contains("10") || name.contains("11") || name.contains("12")) {
                                    setWing("S");
                                    fetchNextScholarNumber("S");
                                } else {
                                    setWing("P");
                                    fetchNextScholarNumber("P");
                                }
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AddStudentActivity.this, "Error loading classes: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchNextScholarNumber(String wing) {
        tvScholarSuggestion.setText("Calculating next " + (wing.equals("S") ? "Secondary" : "Primary") + " scholar number...");
        apiClient.get("api/mobile/students/next-scholar?wing=" + wing, NextScholarResponse.class, new ApiClient.ApiCallback<NextScholarResponse>() {
            @Override
            public void onSuccess(NextScholarResponse result) {
                if (result != null && result.success && result.scholarNumber != null) {
                    etAddScholar.setText(result.scholarNumber);
                    etAddScholar.setSelection(result.scholarNumber.length());
                    tvScholarSuggestion.setText("✅ Auto-assigned: " + result.scholarNumber + " (Total registered: " + result.maxNumber + ")");
                }
            }

            @Override
            public void onError(String errorMessage) {
                tvScholarSuggestion.setText("Next available: " + (wing.equals("S") ? "S101" : "P1001"));
            }
        });
    }

    private void submitAdmission() {
        String scholar = etAddScholar.getText().toString().trim();
        String firstName = etAddFirstName.getText().toString().trim();

        if (firstName.isEmpty()) {
            Toast.makeText(this, "⚠️ Student First Name is required.", Toast.LENGTH_SHORT).show();
            etAddFirstName.requestFocus();
            return;
        }

        if (scholar.isEmpty()) {
            scholar = currentWing.equals("S") ? "S101" : "P1001";
        }

        int classId = 1;
        if (!classList.isEmpty() && spAddClass.getSelectedItemPosition() < classList.size()) {
            classId = classList.get(spAddClass.getSelectedItemPosition()).id;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmitAdmission.setEnabled(false);
        btnTopSave.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("id", 0); // New student
        body.put("scholarNumber", scholar);
        body.put("wing", currentWing);
        body.put("classId", classId);
        body.put("admissionDate", dateFormat.format(admCalendar.getTime()));

        String rollStr = etAddRollNumber.getText().toString().trim();
        if (!rollStr.isEmpty()) {
            try { body.put("rollNumber", Integer.parseInt(rollStr)); } catch (Exception ignored) {}
        }

        body.put("firstName", firstName);
        body.put("middleName", etAddMiddleName.getText().toString().trim());
        body.put("lastName", etAddLastName.getText().toString().trim());
        body.put("nameHindi", etAddNameHindi.getText().toString().trim());

        if (isDobSelected) {
            body.put("dob", dateFormat.format(dobCalendar.getTime()));
        }

        String gender = spAddGender.getSelectedItem().toString();
        if (gender.contains("Male")) gender = "Male";
        else if (gender.contains("Female")) gender = "Female";
        else gender = "Other";
        body.put("gender", gender);

        String category = spAddCategory.getSelectedItem().toString();
        if (category.contains("OBC")) category = "OBC";
        else if (category.contains("SC")) category = "SC";
        else if (category.contains("ST")) category = "ST";
        else category = "GEN";
        body.put("casteCategory", category);

        String blood = spAddBloodGroup.getSelectedItem().toString();
        if (!blood.contains("Select")) body.put("bloodGroup", blood);

        body.put("fatherName", etAddFatherName.getText().toString().trim());
        body.put("fatherNameHindi", etAddFatherHindi.getText().toString().trim());
        body.put("motherName", etAddMotherName.getText().toString().trim());
        body.put("motherNameHindi", etAddMotherHindi.getText().toString().trim());
        body.put("mobile", etAddMobile.getText().toString().trim());
        body.put("fatherMobile", etAddMobile.getText().toString().trim());

        body.put("sssmid", etAddSssmid.getText().toString().trim());
        body.put("aadhaar", etAddAadhaar.getText().toString().trim());
        body.put("village", etAddVillage.getText().toString().trim());
        body.put("address", etAddAddress.getText().toString().trim());

        body.put("previousSchool", etAddPrevSchool.getText().toString().trim());
        body.put("bankAccount", etAddBankAccount.getText().toString().trim());
        body.put("bankName", etAddBankName.getText().toString().trim());
        body.put("ifscCode", etAddIfsc.getText().toString().trim());

        apiClient.post("api/mobile/students/save", body, ApiResponses.SimpleResponse.class, new ApiClient.ApiCallback<ApiResponses.SimpleResponse>() {
            @Override
            public void onSuccess(ApiResponses.SimpleResponse result) {
                progressBar.setVisibility(View.GONE);
                btnSubmitAdmission.setEnabled(true);
                btnTopSave.setEnabled(true);
                if (result != null && result.success) {
                    Toast.makeText(AddStudentActivity.this, "🎉 " + result.message, Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(AddStudentActivity.this, "Failed: " + (result != null ? result.message : "Unknown error"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                btnSubmitAdmission.setEnabled(true);
                btnTopSave.setEnabled(true);
                Toast.makeText(AddStudentActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static class NextScholarResponse {
        public boolean success;
        public String wing;
        public String prefix;
        public int maxNumber;
        public int nextNumber;
        public String scholarNumber;
    }
}
