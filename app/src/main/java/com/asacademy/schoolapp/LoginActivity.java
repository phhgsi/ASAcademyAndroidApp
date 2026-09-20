package com.asacademy.schoolapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.asacademy.schoolapp.models.ApiResponses;
import com.asacademy.schoolapp.models.User;
import com.asacademy.schoolapp.network.ApiClient;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private TextView tvServerUrl;
    private ApiClient apiClient;

    private final ActivityResultLauncher<ScanOptions> qrScannerLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null && !result.getContents().trim().isEmpty()) {
                    String scannedUrl = result.getContents().trim();
                    String cleanedUrl = ApiClient.cleanServerUrl(scannedUrl);
                    apiClient.setBaseUrl(cleanedUrl);
                    updateServerUrlDisplay();
                    Toast.makeText(this, "🟢 Server Configured:\n" + cleanedUrl, Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        apiClient = ApiClient.getInstance(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvServerUrl = findViewById(R.id.tvServerUrl);
        View btnServerSettings = findViewById(R.id.btnServerSettings);
        View btnScanQr = findViewById(R.id.btnScanQr);
        View btnScanQrBig = findViewById(R.id.btnScanQrBig);

        updateServerUrlDisplay();

        if (btnServerSettings != null) {
            btnServerSettings.setOnClickListener(v -> showServerConfigDialog());
        }
        tvServerUrl.setOnClickListener(v -> showServerConfigDialog());

        if (btnScanQr != null) {
            btnScanQr.setOnClickListener(v -> launchQrScanner());
        }
        if (btnScanQrBig != null) {
            btnScanQrBig.setOnClickListener(v -> launchQrScanner());
        }

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void updateServerUrlDisplay() {
        tvServerUrl.setText("Server: " + apiClient.getBaseUrl() + " ⚙️");
    }

    private void launchQrScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("📷 Point camera at Server QR Code\n(Scan Wi-Fi or Cloudflare QR from Windows Screen)");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setCaptureActivity(CaptureActivityAnyOrientation.class);
        qrScannerLauncher.launch(options);
    }

    private void showServerConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Configure Server URL");
        builder.setMessage("Enter the PC's Wi-Fi IP (e.g. http://192.168.1.8:5233) or Cloudflare Live Tunnel URL (https://...):");

        final EditText input = new EditText(this);
        input.setText(apiClient.getBaseUrl());
        input.setSingleLine(true);
        input.setPadding(30, 20, 30, 20);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newUrl = input.getText().toString().trim();
            if (!newUrl.isEmpty()) {
                apiClient.setBaseUrl(newUrl);
                updateServerUrlDisplay();
                Toast.makeText(LoginActivity.this, "Server URL updated!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNeutralButton("📷 Scan QR", (dialog, which) -> {
            launchQrScanner();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);

        apiClient.post("api/mobile/login", body, ApiResponses.LoginResponse.class, new ApiClient.ApiCallback<ApiResponses.LoginResponse>() {
            @Override
            public void onSuccess(ApiResponses.LoginResponse result) {
                btnLogin.setEnabled(true);
                progressBar.setVisibility(View.GONE);

                if (result.success && result.user != null) {
                    Toast.makeText(LoginActivity.this, "Welcome " + result.user.fullName, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("user", result.user);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, result.message != null ? result.message : "Login failed.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                btnLogin.setEnabled(true);
                progressBar.setVisibility(View.GONE);

                new AlertDialog.Builder(LoginActivity.this)
                        .setTitle("Login / Connection Issue")
                        .setMessage(errorMessage + "\n\nTip: Please verify your Server URL or scan the QR code from the Windows desktop screen.")
                        .setPositiveButton("Configure Server", (dialog, which) -> showServerConfigDialog())
                        .setNeutralButton("📷 Scan QR", (dialog, which) -> launchQrScanner())
                        .setNegativeButton("Close", null)
                        .show();
            }
        });
    }
}
