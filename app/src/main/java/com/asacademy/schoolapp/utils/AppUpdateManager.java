package com.asacademy.schoolapp.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppUpdateManager {

    private static final String GITHUB_REPO_API = "https://api.github.com/repos/phhgsi/ASAcademyAndroidApp/releases/latest";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static class ReleaseInfo {
        public String tagName;
        public String releaseName;
        public String changelog;
        public String apkDownloadUrl;
        public String apkFileName;
        public long apkSize;
    }

    public static void checkForUpdate(Activity activity, boolean showFeedbackIfUpToDate) {
        if (activity == null || activity.isFinishing()) return;

        executor.execute(() -> {
            try {
                ReleaseInfo release = fetchLatestRelease();
                if (release == null || release.apkDownloadUrl == null) {
                    if (showFeedbackIfUpToDate) {
                        mainHandler.post(() -> {
                            if (!activity.isFinishing()) {
                                Toast.makeText(activity, "Unable to check for updates right now.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    return;
                }

                String currentVersion = getCurrentVersionName(activity);
                boolean updateAvailable = isNewerVersion(release.tagName, currentVersion);

                mainHandler.post(() -> {
                    if (activity.isFinishing()) return;

                    if (updateAvailable) {
                        showUpdateDialog(activity, release, currentVersion);
                    } else if (showFeedbackIfUpToDate) {
                        Toast.makeText(activity, "✅ You are already using the latest version (v" + currentVersion + ")", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                if (showFeedbackIfUpToDate) {
                    mainHandler.post(() -> {
                        if (!activity.isFinishing()) {
                            Toast.makeText(activity, "Update check failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private static ReleaseInfo fetchLatestRelease() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(GITHUB_REPO_API);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "ASAcademy-Android-App");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return null;
            }

            String jsonStr = readStream(conn.getInputStream());
            JSONObject json = new JSONObject(jsonStr);

            ReleaseInfo info = new ReleaseInfo();
            info.tagName = json.optString("tag_name", "");
            info.releaseName = json.optString("name", info.tagName);
            info.changelog = json.optString("body", "");

            JSONArray assets = json.optJSONArray("assets");
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    String name = asset.optString("name", "");
                    if (name.toLowerCase(Locale.ROOT).endsWith(".apk")) {
                        info.apkDownloadUrl = asset.optString("browser_download_url");
                        info.apkFileName = name;
                        info.apkSize = asset.optLong("size", 0);
                        break;
                    }
                }
            }

            return info;
        } catch (Exception ignored) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public static boolean isNewerVersion(String remoteVer, String currentVer) {
        if (remoteVer == null || currentVer == null) return false;
        String cleanRemote = remoteVer.trim().replaceAll("^[vV]", "").split("[-_]")[0];
        String cleanCurrent = currentVer.trim().replaceAll("^[vV]", "").split("[-_]")[0];

        String[] rParts = cleanRemote.split("\\.");
        String[] cParts = cleanCurrent.split("\\.");
        int len = Math.max(rParts.length, cParts.length);

        for (int i = 0; i < len; i++) {
            int rNum = i < rParts.length ? parseSafeInt(rParts[i]) : 0;
            int cNum = i < cParts.length ? parseSafeInt(cParts[i]) : 0;
            if (rNum > cNum) return true;
            if (rNum < cNum) return false;
        }
        return false;
    }

    private static int parseSafeInt(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getCurrentVersionName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName != null ? pInfo.versionName : "1.0";
        } catch (Exception e) {
            return "1.0";
        }
    }

    private static void showUpdateDialog(Activity activity, ReleaseInfo release, String currentVer) {
        String sizeFormatted = formatSize(release.apkSize);
        String message = "A new version of A.S. Academy is ready to install!\n\n"
                + "• Installed: v" + currentVer + "\n"
                + "• Latest: " + release.tagName + "\n"
                + (sizeFormatted.isEmpty() ? "" : "• Download Size: " + sizeFormatted + "\n\n")
                + (release.changelog != null && !release.changelog.trim().isEmpty() 
                   ? "Changelog:\n" + release.changelog.trim() 
                   : "Includes performance improvements, faster photo uploads, and bug fixes.");

        new AlertDialog.Builder(activity)
                .setTitle("🚀 New App Update Available")
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton("⚡ Update Now", (dialog, which) -> {
                    startDownloadAndInstall(activity, release);
                })
                .setNegativeButton("Later", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private static void startDownloadAndInstall(Activity activity, ReleaseInfo release) {
        // Android 8.0+ Unknown sources install permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                new AlertDialog.Builder(activity)
                        .setTitle("Permission Required")
                        .setMessage("To install the updated version, please allow 'Install unknown apps' permission in system settings.")
                        .setPositiveButton("Open Settings", (d, w) -> {
                            try {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                                activity.startActivity(intent);
                            } catch (Exception e) {
                                Toast.makeText(activity, "Failed to open settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return;
            }
        }

        // Show Download Progress Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Downloading A.S. Academy Update");
        builder.setCancelable(false);

        ProgressBar progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setIndeterminate(false);

        TextView tvStatus = new TextView(activity);
        tvStatus.setText("Connecting to server...");
        tvStatus.setPadding(0, 20, 0, 0);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(activity);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 20);
        layout.addView(progressBar);
        layout.addView(tvStatus);
        builder.setView(layout);

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // Cancelled
        });

        AlertDialog progressDialog = builder.create();
        progressDialog.show();

        executor.execute(() -> {
            File targetFile = null;
            try {
                File downloadDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (downloadDir == null) downloadDir = activity.getCacheDir();
                if (!downloadDir.exists()) downloadDir.mkdirs();

                targetFile = new File(downloadDir, "ASAcademyApp-latest.apk");
                if (targetFile.exists()) targetFile.delete();

                // Open Connection with Redirect support
                HttpURLConnection conn = openConnectionWithRedirects(release.apkDownloadUrl);
                int totalLength = conn.getContentLength();
                if (totalLength <= 0 && release.apkSize > 0) {
                    totalLength = (int) release.apkSize;
                }

                try (InputStream input = conn.getInputStream();
                     FileOutputStream output = new FileOutputStream(targetFile)) {

                    byte[] buffer = new byte[8192];
                    long totalDownloaded = 0;
                    int count;

                    while ((count = input.read(buffer)) != -1) {
                        if (!progressDialog.isShowing()) {
                            // User cancelled
                            if (targetFile.exists()) targetFile.delete();
                            return;
                        }
                        totalDownloaded += count;
                        output.write(buffer, 0, count);

                        if (totalLength > 0) {
                            int percent = (int) ((totalDownloaded * 100) / totalLength);
                            long finalDownloaded = totalDownloaded;
                            int finalTotalLength = totalLength;
                            mainHandler.post(() -> {
                                progressBar.setProgress(percent);
                                tvStatus.setText(String.format(Locale.getDefault(), "%d%% (%s / %s)",
                                        percent, formatSize(finalDownloaded), formatSize(finalTotalLength)));
                            });
                        }
                    }
                    output.flush();
                }

                // Download Finished -> Trigger Android Installer
                File finalFile = targetFile;
                mainHandler.post(() -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    installApk(activity, finalFile);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(activity, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Fallback to browser
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(release.apkDownloadUrl));
                        activity.startActivity(browserIntent);
                    } catch (Exception ignored) {}
                });
            }
        });
    }

    private static HttpURLConnection openConnectionWithRedirects(String initialUrl) throws Exception {
        String currentUrl = initialUrl;
        for (int redirects = 0; redirects < 5; redirects++) {
            URL url = new URL(currentUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)");
            conn.setInstanceFollowRedirects(true);

            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_SEE_OTHER ||
                status == 307 || status == 308) {
                
                String location = conn.getHeaderField("Location");
                if (location != null && !location.isEmpty()) {
                    currentUrl = location;
                    conn.disconnect();
                    continue;
                }
            }
            return conn;
        }
        throw new Exception("Too many redirects while downloading update");
    }

    private static void installApk(Activity activity, File apkFile) {
        try {
            if (apkFile == null || !apkFile.exists()) {
                Toast.makeText(activity, "Update file missing.", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri apkUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".provider", apkFile);

            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            installIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            activity.startActivity(installIntent);
        } catch (Exception e) {
            Toast.makeText(activity, "Could not open installer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static String formatSize(long bytes) {
        if (bytes <= 0) return "";
        float mb = bytes / (1024f * 1024f);
        return String.format(Locale.getDefault(), "%.1f MB", mb);
    }

    private static String readStream(InputStream is) throws Exception {
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
