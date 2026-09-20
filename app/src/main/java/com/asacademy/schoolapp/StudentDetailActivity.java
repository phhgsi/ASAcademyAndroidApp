package com.asacademy.schoolapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.asacademy.schoolapp.models.Student;
import com.asacademy.schoolapp.network.ApiClient;

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

public class StudentDetailActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 201;
    private static final int CAMERA_CAPTURE_CODE = 202;
    private static final int GALLERY_PICK_CODE = 203;

    private Student student;
    private ApiClient apiClient;

    private ImageView ivAvatar;
    private TextView tvAvatarInitial, tvScholarBadge, tvClassBadge, tvWingBadge, tvStudentTitle, tvHindiTitle;
    private Button btnTakePhoto, btnSave, btnDelete, btnFeeDesk, btnIdCard, btnWhatsApp, btnCallParent;
    private ProgressBar progressBar;

    private EditText etScholar, etFirstName, etLastName, etNameHindi, etFatherMobile;
    private EditText etFatherName, etFatherNameHindi, etMotherName, etMotherNameHindi;
    private EditText etSssmid, etAadhaar, etDob, etAddress;
    private Spinner spWing, spGender, spCategory;

    private File currentPhotoFile;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_detail);

        apiClient = ApiClient.getInstance(this);

        student = (Student) getIntent().getSerializableExtra("student");
        if (student == null) {
            Toast.makeText(this, "Student record missing", Toast.LENGTH_SHORT).show();
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
        tvScholarBadge = findViewById(R.id.tvScholarBadge);
        tvClassBadge = findViewById(R.id.tvClassBadge);
        tvWingBadge = findViewById(R.id.tvWingBadge);
        tvStudentTitle = findViewById(R.id.tvStudentTitle);
        tvHindiTitle = findViewById(R.id.tvHindiTitle);

        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        btnFeeDesk = findViewById(R.id.btnFeeDesk);
        btnIdCard = findViewById(R.id.btnIdCard);
        btnWhatsApp = findViewById(R.id.btnWhatsApp);
        btnCallParent = findViewById(R.id.btnCallParent);
        progressBar = findViewById(R.id.progressBar);

        etScholar = findViewById(R.id.etScholar);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etNameHindi = findViewById(R.id.etNameHindi);
        etFatherMobile = findViewById(R.id.etFatherMobile);

        etFatherName = findViewById(R.id.etFatherName);
        etFatherNameHindi = findViewById(R.id.etFatherNameHindi);
        etMotherName = findViewById(R.id.etMotherName);
        etMotherNameHindi = findViewById(R.id.etMotherNameHindi);

        etSssmid = findViewById(R.id.etSssmid);
        etAadhaar = findViewById(R.id.etAadhaar);
        etDob = findViewById(R.id.etDob);
        etAddress = findViewById(R.id.etAddress);

        spWing = findViewById(R.id.spWing);
        spGender = findViewById(R.id.spGender);
        spCategory = findViewById(R.id.spCategory);

        // Populate Wing Spinner
        ArrayAdapter<String> wingAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{"🎒 Primary Wing (P)", "🎓 High School Wing (S)"});
        spWing.setAdapter(wingAdapter);

        // Populate Gender Spinner
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{"Male", "Female", "Other"});
        spGender.setAdapter(genderAdapter);

        // Populate Category Spinner
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{"GEN", "OBC", "SC", "ST"});
        spCategory.setAdapter(catAdapter);
    }

    private void populateData() {
        tvScholarBadge.setText(student.scholarNumber);
        tvClassBadge.setText(student.className != null && !student.className.isEmpty() ? "Class: " + student.className : "Class -");
        tvStudentTitle.setText(student.fullName);

        if (etScholar != null) {
            String rawScholar = student.rawScholarNumber != null && !student.rawScholarNumber.isEmpty() ? student.rawScholarNumber : student.scholarNumber;
            etScholar.setText(rawScholar);
        }

        boolean isSecondary = (student.scholarNumber != null && student.scholarNumber.toUpperCase().startsWith("S")) ||
                              (student.wing != null && (student.wing.equalsIgnoreCase("Secondary") || student.wing.equalsIgnoreCase("High School")));
        if (isSecondary) {
            if (spWing != null) spWing.setSelection(1);
            if (tvWingBadge != null) {
                tvWingBadge.setText("🎓 High School");
                tvWingBadge.setTextColor(android.graphics.Color.parseColor("#FBBF24"));
            }
        } else {
            if (spWing != null) spWing.setSelection(0);
            if (tvWingBadge != null) {
                tvWingBadge.setText("🎒 Primary");
                tvWingBadge.setTextColor(android.graphics.Color.parseColor("#22D3EE"));
            }
        }

        if (student.nameHindi != null && !student.nameHindi.isEmpty()) {
            tvHindiTitle.setText(student.nameHindi);
            tvHindiTitle.setVisibility(View.VISIBLE);
        } else {
            tvHindiTitle.setVisibility(View.GONE);
        }

        // Avatar
        String initial = student.firstName != null && !student.firstName.isEmpty() ? student.firstName.substring(0, 1).toUpperCase() : "S";
        tvAvatarInitial.setText(initial);
        ivAvatar.setVisibility(View.GONE);
        tvAvatarInitial.setVisibility(View.VISIBLE);

        if (student.photoUrl != null && !student.photoUrl.trim().isEmpty()) {
            apiClient.loadImage(student.photoUrl, new ApiClient.ApiCallback<Bitmap>() {
                @Override
                public void onSuccess(Bitmap result) {
                    ivAvatar.setImageBitmap(result);
                    ivAvatar.setVisibility(View.VISIBLE);
                    tvAvatarInitial.setVisibility(View.GONE);
                }

                @Override
                public void onError(String errorMessage) {}
            });
        }

        // Form Fields
        etFirstName.setText(student.firstName);
        etLastName.setText(student.lastName);
        etNameHindi.setText(student.nameHindi);
        etFatherMobile.setText(student.mobile);

        etFatherName.setText(student.fatherName);
        etFatherNameHindi.setText(student.fatherNameHindi);
        etMotherName.setText(student.motherName);
        etMotherNameHindi.setText(student.motherNameHindi);

        etSssmid.setText(student.sssmid);
        etAadhaar.setText(student.aadhaar);
        etDob.setText(student.dob);
        etAddress.setText(student.address);

        if ("Female".equalsIgnoreCase(student.gender)) spGender.setSelection(1);
        else if ("Other".equalsIgnoreCase(student.gender)) spGender.setSelection(2);
        else spGender.setSelection(0);

        if ("OBC".equalsIgnoreCase(student.casteCategory)) spCategory.setSelection(1);
        else if ("SC".equalsIgnoreCase(student.casteCategory)) spCategory.setSelection(2);
        else if ("ST".equalsIgnoreCase(student.casteCategory)) spCategory.setSelection(3);
        else spCategory.setSelection(0);
    }

    private void setupListeners() {
        btnTakePhoto.setOnClickListener(v -> showPhotoOptionsDialog());
        ivAvatar.setOnClickListener(v -> showPhotoOptionsDialog());
        tvAvatarInitial.setOnClickListener(v -> showPhotoOptionsDialog());

        if (btnIdCard != null) {
            btnIdCard.setOnClickListener(v -> openStudentIdCard());
        }
        if (btnWhatsApp != null) {
            btnWhatsApp.setOnClickListener(v -> openWhatsAppParent());
        }
        if (btnCallParent != null) {
            btnCallParent.setOnClickListener(v -> callParent());
        }

        if (spWing != null) {
            spWing.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (etScholar != null) {
                        String currentScholar = etScholar.getText().toString().trim();
                        String targetPrefix = position == 0 ? "P" : "S";
                        if (!currentScholar.isEmpty()) {
                            String numPart = currentScholar.replaceAll("^[A-Za-z]+", "");
                            etScholar.setText(targetPrefix + numPart);
                        }
                    }
                    if (tvWingBadge != null) {
                        if (position == 0) {
                            tvWingBadge.setText("🌱 Primary");
                            tvWingBadge.setTextColor(android.graphics.Color.parseColor("#22D3EE"));
                        } else {
                            tvWingBadge.setText("🎓 Secondary");
                            tvWingBadge.setTextColor(android.graphics.Color.parseColor("#FBBF24"));
                        }
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        etDob.setOnClickListener(v -> showDatePicker());

        btnSave.setOnClickListener(v -> saveStudentDetails());

        btnDelete.setOnClickListener(v -> confirmDeleteStudent());

        btnFeeDesk.setOnClickListener(v -> {
            Intent intent = new Intent(StudentDetailActivity.this, CollectFeeActivity.class);
            intent.putExtra("student", student);
            startActivity(intent);
        });
    }

    private void showPhotoOptionsDialog() {
        CharSequence[] options = new CharSequence[]{"📷 Take Photo (Camera)", "🖼️ Choose from Gallery / Files", "❌ Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Student Photo");
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

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            etDob.setText(dateStr);
        }, cal.get(Calendar.YEAR) - 10, 0, 1);
        dialog.show();
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

    private void openWhatsAppParent() {
        String phone = etFatherMobile != null ? etFatherMobile.getText().toString().trim() : "";
        if (phone.isEmpty() && student != null) phone = student.mobile;
        if (phone == null || phone.isEmpty()) {
            Toast.makeText(this, "No mobile number available for this student.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            if (!cleanPhone.startsWith("91") && cleanPhone.length() == 10) {
                cleanPhone = "91" + cleanPhone;
            }
            String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + Uri.encode("Namaste from A.S. Academy regarding student " + student.fullName + " (Scholar: " + student.scholarNumber + ")");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp is not installed on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    private void callParent() {
        String phone = etFatherMobile != null ? etFatherMobile.getText().toString().trim() : "";
        if (phone.isEmpty() && student != null) phone = student.mobile;
        if (phone == null || phone.isEmpty()) {
            Toast.makeText(this, "No mobile number available for this student.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to launch dialer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openStudentIdCard() {
        Intent intent = new Intent(this, StudentIdCardActivity.class);
        intent.putExtra("student", student);
        startActivity(intent);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_STUDENT_" + student.id + "_" + timeStamp + "_";
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

    public static Bitmap rotateBitmapIfRequired(String imagePath, Bitmap bitmap) {
        if (bitmap == null || imagePath == null) return bitmap;
        try {
            android.media.ExifInterface exif = new android.media.ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL);
            int rotationAngle = 0;
            if (orientation == android.media.ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
            else if (orientation == android.media.ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
            else if (orientation == android.media.ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;

            if (rotationAngle != 0) {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.postRotate(rotationAngle);
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
        } catch (Exception ignored) {}
        return bitmap;
    }

    private void compressAndUploadPhoto(File photoFile) {
        progressBar.setVisibility(View.VISIBLE);
        btnTakePhoto.setEnabled(false);

        // Run On-Device Google ML Kit AI Face Detection, Passport Auto-Framing & Smart Compression
        com.asacademy.schoolapp.utils.ImageEnhancer.processAndOptimizePhotoAsync(photoFile, new com.asacademy.schoolapp.utils.ImageEnhancer.AiOptimizationCallback() {
            @Override
            public void onSuccess(com.asacademy.schoolapp.utils.ImageEnhancer.AiOptimizedResult result) {
                runOnUiThread(() -> {
                    // Update UI Preview with AI-Framed Portrait
                    ivAvatar.setImageBitmap(result.bitmap);
                    ivAvatar.setVisibility(View.VISIBLE);
                    tvAvatarInitial.setVisibility(View.GONE);

                    // Upload pre-compressed Base64 directly
                    apiClient.uploadPhotoDirectBase64(student.id, result.base64Image, ApiResponses.PhotoUploadResponse.class, new ApiClient.ApiCallback<ApiResponses.PhotoUploadResponse>() {
                        @Override
                        public void onSuccess(ApiResponses.PhotoUploadResponse response) {
                            progressBar.setVisibility(View.GONE);
                            btnTakePhoto.setEnabled(true);
                            if (response.success) {
                                student.photoUrl = response.photoUrl;
                                apiClient.evictFromImageCache(response.photoUrl);
                                apiClient.cacheBitmap(response.photoUrl, result.bitmap);
                                Toast.makeText(StudentDetailActivity.this, result.summaryText, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(StudentDetailActivity.this, response.message, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            progressBar.setVisibility(View.GONE);
                            btnTakePhoto.setEnabled(true);
                            Toast.makeText(StudentDetailActivity.this, "Upload Error: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnTakePhoto.setEnabled(true);
                    Toast.makeText(StudentDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void saveStudentDetails() {
        String firstName = etFirstName.getText().toString().trim();
        if (firstName.isEmpty()) {
            Toast.makeText(this, "First Name is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        String scholarNum = etScholar != null ? etScholar.getText().toString().trim() : student.scholarNumber;
        String selectedWing = spWing != null && spWing.getSelectedItemPosition() == 1 ? "Secondary" : "Primary";

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("id", student.id);
        body.put("scholarNumber", scholarNum);
        body.put("wing", selectedWing);
        body.put("firstName", firstName);
        body.put("lastName", etLastName.getText().toString().trim());
        body.put("nameHindi", etNameHindi.getText().toString().trim());
        body.put("fatherName", etFatherName.getText().toString().trim());
        body.put("fatherNameHindi", etFatherNameHindi.getText().toString().trim());
        body.put("motherName", etMotherName.getText().toString().trim());
        body.put("motherNameHindi", etMotherNameHindi.getText().toString().trim());
        body.put("mobile", etFatherMobile.getText().toString().trim());
        body.put("sssmid", etSssmid.getText().toString().trim());
        body.put("aadhaar", etAadhaar.getText().toString().trim());
        body.put("dob", etDob.getText().toString().trim());
        body.put("gender", spGender.getSelectedItem().toString());
        body.put("casteCategory", spCategory.getSelectedItem().toString());
        body.put("address", etAddress.getText().toString().trim());

        apiClient.post("api/mobile/students/save", body, ApiResponses.SimpleResponse.class, new ApiClient.ApiCallback<ApiResponses.SimpleResponse>() {
            @Override
            public void onSuccess(ApiResponses.SimpleResponse result) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                if (result.success) {
                    Toast.makeText(StudentDetailActivity.this, "✅ Details updated successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                } else {
                    Toast.makeText(StudentDetailActivity.this, result.message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(StudentDetailActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDeleteStudent() {
        new AlertDialog.Builder(this)
                .setTitle("Archive / Delete Student")
                .setMessage("Are you sure you want to archive/delete " + student.fullName + " (" + student.scholarNumber + ")? This will mark the student as TC Taken / Inactive.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Yes, Delete", (dialog, which) -> executeDeleteStudent())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeDeleteStudent() {
        progressBar.setVisibility(View.VISIBLE);
        btnDelete.setEnabled(false);

        apiClient.post("api/mobile/students/delete/" + student.id, new HashMap<>(), ApiResponses.SimpleResponse.class, new ApiClient.ApiCallback<ApiResponses.SimpleResponse>() {
            @Override
            public void onSuccess(ApiResponses.SimpleResponse result) {
                progressBar.setVisibility(View.GONE);
                btnDelete.setEnabled(true);
                if (result.success) {
                    Toast.makeText(StudentDetailActivity.this, "Student archived/deleted successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(StudentDetailActivity.this, result.message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                btnDelete.setEnabled(true);
                Toast.makeText(StudentDetailActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
