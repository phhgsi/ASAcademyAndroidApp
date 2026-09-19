package com.asacademy.schoolapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.asacademy.schoolapp.models.ApiResponses;
import com.asacademy.schoolapp.models.PreviousStudent;
import com.asacademy.schoolapp.network.ApiClient;
import com.asacademy.schoolapp.utils.ImageEnhancer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PreviousStudentEditActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 301;
    private static final int CAMERA_CAPTURE_CODE = 302;
    private static final int GALLERY_PICK_CODE = 303;

    private PreviousStudent student;
    private ApiClient apiClient;

    private ImageView ivAvatar;
    private TextView tvAvatarInitial;
    private Button btnTakePhoto;
    private File currentPhotoFile;
    private Uri photoUri;

    private TextView tvScholarBadge, tvWingBadge, tvNameTitle, tvHindiTitle;
    private EditText etScholarNumber, etName, etNameHindi, etFatherName, etFatherNameHindi;
    private EditText etMotherName, etMotherNameHindi, etClass, etSssmid, etAadhar, etDob, etAddress, etTcDetails;
    private Spinner spWing, spStatus, spGender, spCategory;
    private Button btnSave, btnDelete;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_previous_student_edit);

        apiClient = ApiClient.getInstance(this);

        student = (PreviousStudent) getIntent().getSerializableExtra("student");
        if (student == null) {
            Toast.makeText(this, "Archive record missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        populateData();
        setupListeners();
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.ivAvatar);
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);

        tvScholarBadge = findViewById(R.id.tvScholarBadge);
        tvWingBadge = findViewById(R.id.tvWingBadge);
        tvNameTitle = findViewById(R.id.tvNameTitle);
        tvHindiTitle = findViewById(R.id.tvHindiTitle);

        etScholarNumber = findViewById(R.id.etScholarNumber);
        etName = findViewById(R.id.etName);
        etNameHindi = findViewById(R.id.etNameHindi);
        etFatherName = findViewById(R.id.etFatherName);
        etFatherNameHindi = findViewById(R.id.etFatherNameHindi);
        etMotherName = findViewById(R.id.etMotherName);
        etMotherNameHindi = findViewById(R.id.etMotherNameHindi);
        etClass = findViewById(R.id.etClass);
        etSssmid = findViewById(R.id.etSssmid);
        etAadhar = findViewById(R.id.etAadhar);
        etDob = findViewById(R.id.etDob);
        etAddress = findViewById(R.id.etAddress);
        etTcDetails = findViewById(R.id.etTcDetails);

        spWing = findViewById(R.id.spWing);
        spStatus = findViewById(R.id.spStatus);
        spGender = findViewById(R.id.spGender);
        spCategory = findViewById(R.id.spCategory);

        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        progressBar = findViewById(R.id.progressBar);

        // Wing Spinner (Primary / High School)
        ArrayAdapter<String> wingAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{"🎒 Primary Wing (P)", "🎓 High School Wing (S)"});
        spWing.setAdapter(wingAdapter);

        // Status Spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{"Currently Studying", "TC Taken", "12th Passout", "Left School"});
        spStatus.setAdapter(statusAdapter);

        // Gender Spinner
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{"Male", "Female", "Other"});
        spGender.setAdapter(genderAdapter);

        // Category Spinner
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{"GEN", "OBC", "SC", "ST"});
        spCategory.setAdapter(catAdapter);
    }

    private void populateData() {
        if (student.photoUrl != null && !student.photoUrl.trim().isEmpty()) {
            apiClient.loadImage(student.photoUrl, ivAvatar);
            ivAvatar.setVisibility(View.VISIBLE);
            tvAvatarInitial.setVisibility(View.GONE);
        } else {
            String initial = "P";
            if (student.name != null && !student.name.trim().isEmpty()) {
                initial = student.name.trim().substring(0, 1).toUpperCase();
            }
            tvAvatarInitial.setText(initial);
            tvAvatarInitial.setVisibility(View.VISIBLE);
            ivAvatar.setVisibility(View.GONE);
        }

        tvScholarBadge.setText(student.scholarNumber != null ? student.scholarNumber : "-");
        tvNameTitle.setText(student.name);
        if (student.nameHindi != null && !student.nameHindi.isEmpty()) {
            tvHindiTitle.setText(student.nameHindi);
            tvHindiTitle.setVisibility(View.VISIBLE);
        } else {
            tvHindiTitle.setVisibility(View.GONE);
        }

        etScholarNumber.setText(student.scholarNumber != null ? student.scholarNumber : "");
        etName.setText(student.name);
        etNameHindi.setText(student.nameHindi);
        etFatherName.setText(student.fatherName);
        etFatherNameHindi.setText(student.fatherNameHindi);
        etMotherName.setText(student.motherName);
        etMotherNameHindi.setText(student.motherNameHindi);
        etClass.setText(student.className);
        etSssmid.setText(student.sssmid);
        etAadhar.setText(student.aadhar);
        etDob.setText(student.dob);
        etAddress.setText(student.address);
        etTcDetails.setText(student.tcDetails);

        // Wing Selection & Badge
        String wing = student.wing != null ? student.wing : (student.scholarNumber != null && student.scholarNumber.toUpperCase().startsWith("S") ? "High School" : "Primary");
        if ("Secondary".equalsIgnoreCase(wing) || "High School".equalsIgnoreCase(wing) || (student.scholarNumber != null && student.scholarNumber.toUpperCase().startsWith("S"))) {
            spWing.setSelection(1);
            if (tvWingBadge != null) {
                tvWingBadge.setText("🎓 High School Wing");
                tvWingBadge.setTextColor(android.graphics.Color.parseColor("#FBBF24"));
            }
        } else {
            spWing.setSelection(0);
            if (tvWingBadge != null) {
                tvWingBadge.setText("🎒 Primary Wing");
                tvWingBadge.setTextColor(android.graphics.Color.parseColor("#22D3EE"));
            }
        }

        if (student.status != null) {
            if (student.status.contains("TC")) spStatus.setSelection(1);
            else if (student.status.contains("12th") || student.status.contains("Pass")) spStatus.setSelection(2);
            else if (student.status.contains("Left")) spStatus.setSelection(3);
            else spStatus.setSelection(0);
        }

        if ("Female".equalsIgnoreCase(student.gender)) spGender.setSelection(1);
        else if ("Other".equalsIgnoreCase(student.gender)) spGender.setSelection(2);
        else spGender.setSelection(0);

        if ("OBC".equalsIgnoreCase(student.category)) spCategory.setSelection(1);
        else if ("SC".equalsIgnoreCase(student.category)) spCategory.setSelection(2);
        else if ("ST".equalsIgnoreCase(student.category)) spCategory.setSelection(3);
        else spCategory.setSelection(0);
    }

    private void setupListeners() {
        btnTakePhoto.setOnClickListener(v -> showPhotoOptionsDialog());
        ivAvatar.setOnClickListener(v -> showPhotoOptionsDialog());
        tvAvatarInitial.setOnClickListener(v -> showPhotoOptionsDialog());

        etDob.setOnClickListener(v -> showDatePicker());

        // Update scholar prefix when wing spinner changes
        spWing.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String curScholar = etScholarNumber.getText().toString().trim();
                if (position == 0) { // Primary (P)
                    if (tvWingBadge != null) {
                        tvWingBadge.setText("🌱 Primary Wing");
                        tvWingBadge.setTextColor(android.graphics.Color.parseColor("#22D3EE"));
                    }
                    if (curScholar.startsWith("S") || curScholar.startsWith("s")) {
                        etScholarNumber.setText("P" + curScholar.substring(1));
                    } else if (!curScholar.isEmpty() && !curScholar.toUpperCase().startsWith("P")) {
                        etScholarNumber.setText("P" + curScholar);
                    }
                } else { // Secondary (S)
                    if (tvWingBadge != null) {
                        tvWingBadge.setText("🎓 Secondary Wing");
                        tvWingBadge.setTextColor(android.graphics.Color.parseColor("#FBBF24"));
                    }
                    if (curScholar.startsWith("P") || curScholar.startsWith("p")) {
                        etScholarNumber.setText("S" + curScholar.substring(1));
                    } else if (!curScholar.isEmpty() && !curScholar.toUpperCase().startsWith("S")) {
                        etScholarNumber.setText("S" + curScholar);
                    }
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnSave.setOnClickListener(v -> saveRecord());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void showPhotoOptionsDialog() {
        CharSequence[] options = new CharSequence[]{"📷 Take Photo (Camera)", "🖼️ Choose from Gallery / Files", "❌ Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Permanent Register Photo");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                checkCameraPermissionAndLaunch();
            } else if (which == 1) {
                launchGalleryPicker();
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            launchNativeCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchNativeCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to take student photos.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchNativeCamera() {
        try {
            currentPhotoFile = createImageFile();
            if (currentPhotoFile != null) {
                photoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", currentPhotoFile);
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(takePictureIntent, CAMERA_CAPTURE_CODE);
            }
        } catch (Exception ex) {
            Toast.makeText(this, "Error launching camera: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGalleryPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Select Student Photo"), GALLERY_PICK_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening photo picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_ARCHIVE_" + student.id + "_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_CAPTURE_CODE && resultCode == RESULT_OK) {
            if (currentPhotoFile != null && currentPhotoFile.exists()) {
                compressAndUploadPhoto(currentPhotoFile);
            }
        } else if (requestCode == GALLERY_PICK_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            handleGalleryImageUri(data.getData());
        }
    }

    private void handleGalleryImageUri(Uri uri) {
        try {
            currentPhotoFile = createImageFile();
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                FileOutputStream fos = new FileOutputStream(currentPhotoFile);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                inputStream.close();
                compressAndUploadPhoto(currentPhotoFile);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image from gallery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void compressAndUploadPhoto(File photoFile) {
        progressBar.setVisibility(View.VISIBLE);
        btnTakePhoto.setEnabled(false);

        ImageEnhancer.processAndOptimizePhotoAsync(photoFile, new ImageEnhancer.AiOptimizationCallback() {
            @Override
            public void onSuccess(ImageEnhancer.AiOptimizedResult result) {
                runOnUiThread(() -> {
                    ivAvatar.setImageBitmap(result.bitmap);
                    ivAvatar.setVisibility(View.VISIBLE);
                    tvAvatarInitial.setVisibility(View.GONE);

                    apiClient.uploadPhotoDirectBase64(student.id, result.base64Image, ApiResponses.PhotoUploadResponse.class, new ApiClient.ApiCallback<ApiResponses.PhotoUploadResponse>() {
                        @Override
                        public void onSuccess(ApiResponses.PhotoUploadResponse response) {
                            progressBar.setVisibility(View.GONE);
                            btnTakePhoto.setEnabled(true);
                            if (response.success) {
                                student.photoUrl = response.photoUrl;
                                Toast.makeText(PreviousStudentEditActivity.this, result.summaryText, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(PreviousStudentEditActivity.this, response.message, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            progressBar.setVisibility(View.GONE);
                            btnTakePhoto.setEnabled(true);
                            Toast.makeText(PreviousStudentEditActivity.this, "Upload Error: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnTakePhoto.setEnabled(true);
                    Toast.makeText(PreviousStudentEditActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            etDob.setText(dateStr);
        }, cal.get(Calendar.YEAR) - 10, 0, 1);
        dialog.show();
    }

    private void saveRecord() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Student Name is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        String scholarNum = etScholarNumber.getText().toString().trim();
        if (scholarNum.isEmpty()) {
            scholarNum = student.scholarNumber;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("id", student.id);
        body.put("scholarNumber", scholarNum);
        body.put("wing", spWing.getSelectedItemPosition() == 1 ? "Secondary" : "Primary");
        body.put("name", name);
        body.put("nameHindi", etNameHindi.getText().toString().trim());
        body.put("fatherName", etFatherName.getText().toString().trim());
        body.put("fatherNameHindi", etFatherNameHindi.getText().toString().trim());
        body.put("motherName", etMotherName.getText().toString().trim());
        body.put("motherNameHindi", etMotherNameHindi.getText().toString().trim());
        body.put("class", etClass.getText().toString().trim());
        body.put("status", spStatus.getSelectedItem().toString());
        body.put("sssmid", etSssmid.getText().toString().trim());
        body.put("aadhar", etAadhar.getText().toString().trim());
        body.put("dob", etDob.getText().toString().trim());
        body.put("gender", spGender.getSelectedItem().toString());
        body.put("category", spCategory.getSelectedItem().toString());
        body.put("address", etAddress.getText().toString().trim());
        body.put("tcDetails", etTcDetails.getText().toString().trim());

        apiClient.post("api/mobile/previous-students/save", body, ApiResponses.SimpleResponse.class, new ApiClient.ApiCallback<ApiResponses.SimpleResponse>() {
            @Override
            public void onSuccess(ApiResponses.SimpleResponse result) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                if (result.success) {
                    Toast.makeText(PreviousStudentEditActivity.this, "Permanent register updated!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                } else {
                    Toast.makeText(PreviousStudentEditActivity.this, result.message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(PreviousStudentEditActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Archive Record")
                .setMessage("Are you sure you want to delete " + student.name + " (" + student.scholarNumber + ") from permanent register?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Yes, Delete", (dialog, which) -> executeDelete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeDelete() {
        progressBar.setVisibility(View.VISIBLE);
        btnDelete.setEnabled(false);

        apiClient.post("api/mobile/previous-students/delete/" + student.id, new HashMap<>(), ApiResponses.SimpleResponse.class, new ApiClient.ApiCallback<ApiResponses.SimpleResponse>() {
            @Override
            public void onSuccess(ApiResponses.SimpleResponse result) {
                progressBar.setVisibility(View.GONE);
                btnDelete.setEnabled(true);
                if (result.success) {
                    Toast.makeText(PreviousStudentEditActivity.this, "Archive record deleted successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(PreviousStudentEditActivity.this, result.message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                btnDelete.setEnabled(true);
                Toast.makeText(PreviousStudentEditActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
