package com.asacademy.schoolapp.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import com.asacademy.schoolapp.models.ApiResponses;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {

    private static final String PREFS_NAME = "ASAcademyPrefs";
    private static final String KEY_SERVER_URL = "server_url";
    public static final String DEFAULT_SERVER_URL = "http://192.168.1.8:5233";

    private static ApiClient instance;
    private final Context context;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();
    private final LruCache<String, Bitmap> imageCache;

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    private ApiClient(Context context) {
        this.context = context.getApplicationContext();
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        imageCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context);
        }
        return instance;
    }

    public String getBaseUrl() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String url = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
        return cleanServerUrl(url);
    }

    public void setBaseUrl(String url) {
        if (url != null) {
            String cleaned = cleanServerUrl(url);
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_SERVER_URL, cleaned).apply();
        }
    }

    public static String cleanServerUrl(String input) {
        if (input == null || input.trim().isEmpty()) {
            return DEFAULT_SERVER_URL;
        }
        String url = input.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.contains("trycloudflare.com")) {
                url = "https://" + url;
            } else {
                url = "http://" + url;
            }
        }
        try {
            java.net.URI uri = new java.net.URI(url);
            String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
            String host = uri.getHost();
            int port = uri.getPort();
            if (host != null && !host.isEmpty()) {
                if (port == -1) {
                    if (host.equals("localhost") || host.equals("127.0.0.1") || host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                        port = 5233;
                    }
                }
                return scheme + "://" + host + (port != -1 ? ":" + port : "");
            }
        } catch (Exception ignored) {}

        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    public Gson getGson() {
        return gson;
    }

    public <T> void get(String endpoint, Class<T> responseClass, ApiCallback<T> callback) {
        executor.execute(() -> {
            try {
                String fullUrl = getBaseUrl() + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);
                HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Accept", "application/json");

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                String responseStr = readStream(is);

                if (code >= 200 && code < 300) {
                    T result = gson.fromJson(responseStr, responseClass);
                    mainHandler.post(() -> callback.onSuccess(result));
                } else {
                    String msg = extractErrorMessage(code, responseStr);
                    mainHandler.post(() -> callback.onError(msg));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Network Error: " + e.getMessage()));
            }
        });
    }

    public <T> void post(String endpoint, Object requestBody, Class<T> responseClass, ApiCallback<T> callback) {
        executor.execute(() -> {
            try {
                String fullUrl = getBaseUrl() + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);
                HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");

                String jsonInput = gson.toJson(requestBody);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                String responseStr = readStream(is);

                if (code >= 200 && code < 300) {
                    T result = gson.fromJson(responseStr, responseClass);
                    mainHandler.post(() -> callback.onSuccess(result));
                } else {
                    String msg = extractErrorMessage(code, responseStr);
                    mainHandler.post(() -> callback.onError(msg));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Network Error: " + e.getMessage()));
            }
        });
    }

    private String extractErrorMessage(int code, String responseStr) {
        if (responseStr != null && !responseStr.trim().isEmpty()) {
            try {
                ApiResponses.SimpleResponse errObj = gson.fromJson(responseStr, ApiResponses.SimpleResponse.class);
                if (errObj != null && errObj.message != null && !errObj.message.trim().isEmpty()) {
                    return errObj.message.trim();
                }
            } catch (Exception ignored) {}
        }
        if (code == 401) return "Invalid username or password (HTTP 401).";
        if (code == 403) return "Forbidden (HTTP 403): You do not have permission.";
        if (code == 404) return "Endpoint not found (HTTP 404). Please verify Server URL.";
        if (code >= 500) return "Server Error (HTTP " + code + "). Please check server logs.";
        return "Server returned status " + code + (responseStr != null ? ": " + responseStr : "");
    }

    public static Bitmap getResizedAndOptimizedBitmap(Bitmap source, int maxDim) {
        if (source == null) return null;
        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= maxDim && height <= maxDim) {
            return source;
        }
        float ratio = Math.min((float) maxDim / width, (float) maxDim / height);
        int targetW = Math.max(1, Math.round(width * ratio));
        int targetH = Math.max(1, Math.round(height * ratio));

        Bitmap targetBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(targetBitmap);
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG | android.graphics.Paint.FILTER_BITMAP_FLAG | android.graphics.Paint.DITHER_FLAG);
        android.graphics.Rect srcRect = new android.graphics.Rect(0, 0, width, height);
        android.graphics.Rect dstRect = new android.graphics.Rect(0, 0, targetW, targetH);
        canvas.drawBitmap(source, srcRect, dstRect, paint);
        return targetBitmap;
    }

    public <T> void uploadPhotoDirectBase64(int studentId, String base64Image, Class<T> responseClass, ApiCallback<T> callback) {
        uploadPhotoDirectBase64Internal(false, studentId, base64Image, responseClass, callback);
    }

    public <T> void uploadPreviousStudentPhotoDirectBase64(int studentId, String base64Image, Class<T> responseClass, ApiCallback<T> callback) {
        uploadPhotoDirectBase64Internal(true, studentId, base64Image, responseClass, callback);
    }

    private <T> void uploadPhotoDirectBase64Internal(boolean isPrevious, int studentId, String base64Image, Class<T> responseClass, ApiCallback<T> callback) {
        executor.execute(() -> {
            try {
                String endpoint = isPrevious 
                        ? "api/mobile/previous-students/upload-photo-base64" 
                        : "api/mobile/students/upload-photo-base64";

                String fullUrl = getBaseUrl() + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);
                HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000); // 30s connect timeout for photo uploads
                conn.setReadTimeout(60000);    // 60s read timeout for photo uploads
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");

                java.util.Map<String, Object> body = new java.util.HashMap<>();
                body.put("studentId", studentId);
                body.put("base64Image", base64Image);
                body.put("isPreviousStudent", isPrevious);
                body.put("studentType", isPrevious ? "previous" : "active");

                String jsonInput = gson.toJson(body);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                String responseStr = readStream(is);

                if (code >= 200 && code < 300) {
                    T result = gson.fromJson(responseStr, responseClass);
                    mainHandler.post(() -> callback.onSuccess(result));
                } else {
                    String msg = extractErrorMessage(code, responseStr);
                    mainHandler.post(() -> callback.onError(msg));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Failed to upload photo: " + e.getMessage()));
            }
        });
    }

    public <T> void uploadPhotoBase64(int studentId, Bitmap bitmap, Class<T> responseClass, ApiCallback<T> callback) {
        executor.execute(() -> {
            try {
                // 1. High-Quality Resize
                int maxDim = 800;
                Bitmap resized = getResizedAndOptimizedBitmap(bitmap, maxDim);

                // 2. ✨ AI Smart Enhancement
                Bitmap enhanced = com.asacademy.schoolapp.utils.ImageEnhancer.enhancePhoto(resized);
                Bitmap targetBitmap = enhanced != null ? enhanced : resized;

                // 3. Ultra-Crisp JPEG Compression (Target ~60KB)
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int quality = 85;
                targetBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                while (baos.size() > 75 * 1024 && quality > 65) {
                    baos.reset();
                    quality -= 5;
                    targetBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                }

                byte[] imageBytes = baos.toByteArray();
                String base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP);

                uploadPhotoDirectBase64(studentId, base64Image, responseClass, callback);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Failed to encode photo: " + e.getMessage()));
            }
        });
    }

    public <T> void uploadPhoto(String endpoint, int studentId, File imageFile, Class<T> responseClass, ApiCallback<T> callback) {
        executor.execute(() -> {
            String boundary = "===ASAcademy" + System.currentTimeMillis() + "===";
            String LINE_FEED = "\r\n";
            try {
                String fullUrl = getBaseUrl() + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);
                HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setRequestProperty("Accept", "application/json");

                try (DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream())) {
                    // 1. studentId parameter
                    outputStream.writeBytes("--" + boundary + LINE_FEED);
                    outputStream.writeBytes("Content-Disposition: form-data; name=\"studentId\"" + LINE_FEED);
                    outputStream.writeBytes(LINE_FEED);
                    outputStream.writeBytes(String.valueOf(studentId) + LINE_FEED);

                    // 2. photo file
                    outputStream.writeBytes("--" + boundary + LINE_FEED);
                    outputStream.writeBytes("Content-Disposition: form-data; name=\"photo\"; filename=\"" + imageFile.getName() + "\"" + LINE_FEED);
                    outputStream.writeBytes("Content-Type: image/jpeg" + LINE_FEED);
                    outputStream.writeBytes(LINE_FEED);

                    try (FileInputStream inputStream = new FileInputStream(imageFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                    outputStream.writeBytes(LINE_FEED);
                    outputStream.writeBytes("--" + boundary + "--" + LINE_FEED);
                    outputStream.flush();
                }

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                String responseStr = readStream(is);

                if (code >= 200 && code < 300) {
                    T result = gson.fromJson(responseStr, responseClass);
                    mainHandler.post(() -> callback.onSuccess(result));
                } else {
                    mainHandler.post(() -> callback.onError("Upload failed with code " + code + ": " + responseStr));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Photo Upload Error: " + e.getMessage()));
            }
        });
    }

    public void loadImage(String relativeUrl, ImageView imageView) {
        if (imageView == null) return;
        loadImage(relativeUrl, new ApiCallback<Bitmap>() {
            @Override
            public void onSuccess(Bitmap result) {
                imageView.setImageBitmap(result);
                imageView.setVisibility(android.view.View.VISIBLE);
            }

            @Override
            public void onError(String errorMessage) {}
        });
    }

    public void loadImage(String relativeUrl, ApiCallback<Bitmap> callback) {
        if (relativeUrl == null || relativeUrl.trim().isEmpty()) {
            callback.onError("Empty URL");
            return;
        }

        String fullUrl = relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://") 
                ? relativeUrl 
                : getBaseUrl() + (relativeUrl.startsWith("/") ? relativeUrl : "/" + relativeUrl);

        Bitmap cached = imageCache.get(fullUrl);
        if (cached != null) {
            callback.onSuccess(cached);
            return;
        }

        executor.execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(12000);
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    try (InputStream is = conn.getInputStream()) {
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        opts.inDither = false;
                        opts.inScaled = false;
                        Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);
                        if (bitmap != null) {
                            imageCache.put(fullUrl, bitmap);
                            mainHandler.post(() -> callback.onSuccess(bitmap));
                        } else {
                            mainHandler.post(() -> callback.onError("Could not decode image"));
                        }
                    }
                } else {
                    mainHandler.post(() -> callback.onError("HTTP " + responseCode));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void evictFromImageCache(String relativeOrFullUrl) {
        if (relativeOrFullUrl == null || relativeOrFullUrl.trim().isEmpty()) return;
        String fullUrl = relativeOrFullUrl.startsWith("http://") || relativeOrFullUrl.startsWith("https://") 
                ? relativeOrFullUrl 
                : getBaseUrl() + (relativeOrFullUrl.startsWith("/") ? relativeOrFullUrl : "/" + relativeOrFullUrl);
        imageCache.remove(fullUrl);
        int qIdx = fullUrl.indexOf('?');
        if (qIdx != -1) {
            imageCache.remove(fullUrl.substring(0, qIdx));
        }
    }

    public void cacheBitmap(String relativeOrFullUrl, Bitmap bitmap) {
        if (relativeOrFullUrl == null || bitmap == null) return;
        String fullUrl = relativeOrFullUrl.startsWith("http://") || relativeOrFullUrl.startsWith("https://") 
                ? relativeOrFullUrl 
                : getBaseUrl() + (relativeOrFullUrl.startsWith("/") ? relativeOrFullUrl : "/" + relativeOrFullUrl);
        imageCache.put(fullUrl, bitmap);
        int qIdx = fullUrl.indexOf('?');
        if (qIdx != -1) {
            imageCache.put(fullUrl.substring(0, qIdx), bitmap);
        }
    }

    public void clearImageCache() {
        imageCache.evictAll();
    }

    private String readStream(InputStream is) throws Exception {
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
