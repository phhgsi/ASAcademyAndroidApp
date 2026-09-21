package com.asacademy.schoolapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import com.asacademy.schoolapp.models.ApiResponses;
import com.asacademy.schoolapp.network.ApiClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * High-performance background photo upload queue manager.
 * Features:
 * 1. 0ms Optimistic UI Caching (bitmap stored in memory immediately).
 * 2. Non-blocking asynchronous worker thread pool.
 * 3. Event listeners for real-time progress badges.
 * 4. Automatic retry on transient network drops.
 */
public class PhotoUploadManager {

    public enum Status {
        QUEUED,
        UPLOADING,
        SUCCESS,
        FAILED
    }

    public static class UploadItem {
        public final String queueId;
        public final int studentId;
        public final boolean isPreviousStudent;
        public final String studentName;
        public final String scholarNumber;
        public final String base64Image;
        public final Bitmap bitmap;
        public final String localCacheKey;

        public Status status = Status.QUEUED;
        public String error = null;
        public String serverPhotoUrl = null;
        public int retryCount = 0;
        public final long timestamp;

        public UploadItem(int studentId, boolean isPreviousStudent, String studentName,
                          String scholarNumber, String base64Image, Bitmap bitmap, String localCacheKey) {
            this.queueId = UUID.randomUUID().toString();
            this.studentId = studentId;
            this.isPreviousStudent = isPreviousStudent;
            this.studentName = studentName != null ? studentName : "Student #" + studentId;
            this.scholarNumber = scholarNumber != null ? scholarNumber : "-";
            this.base64Image = base64Image;
            this.bitmap = bitmap;
            this.localCacheKey = localCacheKey;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public interface UploadListener {
        void onQueueProgress(int remainingCount, int uploadingCount, int completedCount);
        void onItemStatusChanged(UploadItem item);
    }

    private static volatile PhotoUploadManager instance;
    private final Context appContext;
    private final ApiClient apiClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();
    private final ConcurrentLinkedQueue<UploadItem> queue = new ConcurrentLinkedQueue<>();
    private final List<UploadItem> allItems = Collections.synchronizedList(new ArrayList<>());
    private final List<UploadListener> listeners = new ArrayList<>();
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    private PhotoUploadManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.apiClient = ApiClient.getInstance(appContext);
    }

    public static PhotoUploadManager getInstance(Context context) {
        if (instance == null) {
            synchronized (PhotoUploadManager.class) {
                if (instance == null) {
                    instance = new PhotoUploadManager(context);
                }
            }
        }
        return instance;
    }

    public synchronized void registerListener(UploadListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            notifyProgress();
        }
    }

    public synchronized void unregisterListener(UploadListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Enqueue a photo for asynchronous background upload.
     * Instantly caches the bitmap into ApiClient memory so it displays with 0ms delay!
     *
     * @return The temporary local cache key used to immediately display the photo in UI.
     */
    public String enqueue(int studentId, boolean isPreviousStudent, String studentName,
                          String scholarNumber, Bitmap bitmap, String base64Image) {
        String localKey = "local_temp_" + (isPreviousStudent ? "prev_" : "active_") + studentId + "_" + System.currentTimeMillis();

        // 1. Instantly cache in memory
        if (bitmap != null) {
            apiClient.cacheBitmap(localKey, bitmap);
        }

        // 2. Create queue item
        UploadItem item = new UploadItem(studentId, isPreviousStudent, studentName, scholarNumber, base64Image, bitmap, localKey);
        allItems.add(item);
        queue.add(item);

        notifyProgress();
        notifyItemChanged(item);

        // 3. Trigger worker
        processNext();

        return localKey;
    }

    private void processNext() {
        if (isProcessing.get()) {
            return;
        }

        uploadExecutor.execute(() -> {
            if (!isProcessing.compareAndSet(false, true)) {
                return;
            }

            try {
                UploadItem item;
                while ((item = queue.poll()) != null) {
                    item.status = Status.UPLOADING;
                    notifyItemChanged(item);
                    notifyProgress();

                    uploadSingleItem(item);
                }
            } finally {
                isProcessing.set(false);
                notifyProgress();
            }
        });
    }

    private void uploadSingleItem(UploadItem item) {
        final Object lock = new Object();
        final AtomicBoolean finished = new AtomicBoolean(false);

        ApiClient.ApiCallback<ApiResponses.PhotoUploadResponse> callback = new ApiClient.ApiCallback<ApiResponses.PhotoUploadResponse>() {
            @Override
            public void onSuccess(ApiResponses.PhotoUploadResponse response) {
                if (response != null && response.success && response.photoUrl != null) {
                    item.status = Status.SUCCESS;
                    item.serverPhotoUrl = response.photoUrl;

                    // Cache under the permanent URL too
                    if (item.bitmap != null) {
                        apiClient.evictFromImageCache(response.photoUrl);
                        apiClient.cacheBitmap(response.photoUrl, item.bitmap);
                    }
                } else {
                    handleFailure(item, response != null ? response.message : "Server error");
                }
                notifyItemChanged(item);
                notifyProgress();

                synchronized (lock) {
                    finished.set(true);
                    lock.notifyAll();
                }
            }

            @Override
            public void onError(String errorMessage) {
                handleFailure(item, errorMessage);
                notifyItemChanged(item);
                notifyProgress();

                synchronized (lock) {
                    finished.set(true);
                    lock.notifyAll();
                }
            }
        };

        if (item.isPreviousStudent) {
            apiClient.uploadPreviousStudentPhotoDirectBase64(item.studentId, item.base64Image, ApiResponses.PhotoUploadResponse.class, callback);
        } else {
            apiClient.uploadPhotoDirectBase64(item.studentId, item.base64Image, ApiResponses.PhotoUploadResponse.class, callback);
        }

        // Wait for network response synchronously within background worker thread
        synchronized (lock) {
            while (!finished.get()) {
                try {
                    lock.wait(35000); // 35s max timeout per item
                    break;
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private void handleFailure(UploadItem item, String errorMsg) {
        if (item.retryCount < 2) {
            item.retryCount++;
            item.status = Status.QUEUED;
            item.error = "Retrying (" + item.retryCount + "/2): " + errorMsg;
            queue.add(item);
        } else {
            item.status = Status.FAILED;
            item.error = errorMsg;
        }
    }

    public int getPendingCount() {
        int count = 0;
        synchronized (allItems) {
            for (UploadItem it : allItems) {
                if (it.status == Status.QUEUED || it.status == Status.UPLOADING) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getUploadingCount() {
        int count = 0;
        synchronized (allItems) {
            for (UploadItem it : allItems) {
                if (it.status == Status.UPLOADING) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getCompletedCount() {
        int count = 0;
        synchronized (allItems) {
            for (UploadItem it : allItems) {
                if (it.status == Status.SUCCESS) {
                    count++;
                }
            }
        }
        return count;
    }

    private void notifyProgress() {
        int remaining = getPendingCount();
        int active = getUploadingCount();
        int done = getCompletedCount();

        mainHandler.post(() -> {
            synchronized (PhotoUploadManager.this) {
                for (UploadListener l : listeners) {
                    try {
                        l.onQueueProgress(remaining, active, done);
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    private void notifyItemChanged(UploadItem item) {
        mainHandler.post(() -> {
            synchronized (PhotoUploadManager.this) {
                for (UploadListener l : listeners) {
                    try {
                        l.onItemStatusChanged(item);
                    } catch (Exception ignored) {}
                }
            }
        });
    }
}
