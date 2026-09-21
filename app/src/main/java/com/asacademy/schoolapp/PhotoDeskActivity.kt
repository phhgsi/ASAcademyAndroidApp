package com.asacademy.schoolapp

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.asacademy.schoolapp.models.ApiResponses
import com.asacademy.schoolapp.models.SchoolClass
import com.asacademy.schoolapp.models.Student
import com.asacademy.schoolapp.network.ApiClient
import com.asacademy.schoolapp.utils.ImageEnhancer
import com.asacademy.schoolapp.utils.PhotoUploadManager
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class PhotoDeskActivity : ComponentActivity() {

    private lateinit var apiClient: ApiClient
    private lateinit var uploadManager: PhotoUploadManager
    private var targetStudentId: Int = 0
    private var currentPhotoFile: File? = null
    private var photoUri: Uri? = null

    // State holders
    private val studentsList = mutableStateListOf<Student>()
    private val classList = mutableStateListOf<SchoolClass>()
    private var selectedClassId by mutableStateOf<Int?>(null)
    private var activeFilter by mutableStateOf("ALL") // "ALL", "MISSING", "HAS_PHOTO"
    private var searchQuery by mutableStateOf("")
    private var isLoading by mutableStateOf(false)
    private var queueRemaining by mutableStateOf(0)
    private var queueActive by mutableStateOf(0)
    private var queueCompleted by mutableStateOf(0)

    private val uploadListener = object : PhotoUploadManager.UploadListener {
        override fun onQueueProgress(remainingCount: Int, uploadingCount: Int, completedCount: Int) {
            runOnUiThread {
                queueRemaining = remainingCount
                queueActive = uploadingCount
                queueCompleted = completedCount
            }
        }

        override fun onItemStatusChanged(item: PhotoUploadManager.UploadItem) {
            if (item.status == PhotoUploadManager.Status.SUCCESS && item.serverPhotoUrl != null) {
                runOnUiThread {
                    val index = studentsList.indexOfFirst { it.id == item.studentId }
                    if (index != -1) {
                        val s = studentsList[index]
                        s.photoUrl = item.serverPhotoUrl
                        studentsList[index] = s
                    }
                }
            }
        }
    }

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && currentPhotoFile != null && currentPhotoFile!!.exists()) {
            processAndUploadCapturedPhoto(currentPhotoFile!!)
        }
    }

    // Gallery launcher
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data?.data != null) {
            handleGalleryUri(result.data!!.data!!)
        }
    }

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchNativeCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to snap photos.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiClient = ApiClient.getInstance(this)
        uploadManager = PhotoUploadManager.getInstance(this)
        uploadManager.registerListener(uploadListener)

        loadClasses()
        loadStudents()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF38BDF8),
                    secondary = Color(0xFF818CF8),
                    background = Color(0xFF0F172A),
                    surface = Color(0xFF1E293B),
                    surfaceVariant = Color(0xFF334155),
                    onPrimary = Color.White,
                    onBackground = Color(0xFFF1F5F9),
                    onSurface = Color(0xFFE2E8F0)
                )
            ) {
                PhotoDeskScreen()
            }
        }
    }

    private fun loadClasses() {
        apiClient.get("api/mobile/classes", ApiResponses.ClassListResponse::class.java, object : ApiClient.ApiCallback<ApiResponses.ClassListResponse> {
            override fun onSuccess(result: ApiResponses.ClassListResponse) {
                if (result.success && result.data != null) {
                    classList.clear()
                    classList.addAll(result.data)
                }
            }

            override fun onError(errorMessage: String) {
                // Ignore or log
            }
        })
    }

    private fun loadStudents() {
        isLoading = true
        var endpoint = "api/mobile/students?"
        if (selectedClassId != null && selectedClassId!! > 0) {
            endpoint += "classId=$selectedClassId&"
        }
        apiClient.get(endpoint, ApiResponses.StudentListResponse::class.java, object : ApiClient.ApiCallback<ApiResponses.StudentListResponse> {
            override fun onSuccess(result: ApiResponses.StudentListResponse) {
                runOnUiThread {
                    isLoading = false
                    if (result.success && result.data != null) {
                        studentsList.clear()
                        studentsList.addAll(result.data)
                    }
                }
            }

            override fun onError(errorMessage: String) {
                runOnUiThread {
                    isLoading = false
                    Toast.makeText(this@PhotoDeskActivity, "Failed to load students: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun promptPhotoSource(studentId: Int) {
        targetStudentId = studentId
        val options = arrayOf("📷 Take Photo with Camera", "🖼️ Choose from Gallery", "❌ Cancel")
        AlertDialog.Builder(this)
            .setTitle("Snap Student Photo")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            launchNativeCamera()
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                    1 -> launchGalleryPicker()
                    else -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun launchNativeCamera() {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            currentPhotoFile = File.createTempFile("STUDENT_${targetStudentId}_${timeStamp}_", ".jpg", storageDir)
            if (currentPhotoFile != null) {
                photoUri = FileProvider.getUriForFile(this, "$packageName.provider", currentPhotoFile!!)
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                cameraLauncher.launch(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchGalleryPicker() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            galleryLauncher.launch(Intent.createChooser(intent, "Select Student Photo"))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to open gallery: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleGalleryUri(uri: Uri) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            currentPhotoFile = File.createTempFile("GALLERY_${targetStudentId}_${timeStamp}_", ".jpg", storageDir)
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream != null && currentPhotoFile != null) {
                val fos = FileOutputStream(currentPhotoFile)
                val buffer = ByteArray(8192)
                var len: Int
                while (inputStream.read(buffer).also { len = it } > 0) {
                    fos.write(buffer, 0, len)
                }
                fos.close()
                inputStream.close()
                processAndUploadCapturedPhoto(currentPhotoFile!!)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to process gallery image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        uploadManager.unregisterListener(uploadListener)
    }

    private fun processAndUploadCapturedPhoto(photoFile: File) {
        val currentTargetId = targetStudentId
        val targetStudent = studentsList.firstOrNull { it.id == currentTargetId }
        val sName = targetStudent?.fullName ?: "Student #$currentTargetId"
        val sScholar = targetStudent?.scholarNumber ?: "-"

        ImageEnhancer.processAndOptimizePhotoAsync(photoFile, object : ImageEnhancer.AiOptimizationCallback {
            override fun onSuccess(result: ImageEnhancer.AiOptimizedResult) {
                runOnUiThread {
                    // Enqueue to background queue and cache in memory immediately!
                    val tempKey = uploadManager.enqueue(
                        currentTargetId,
                        false,
                        sName,
                        sScholar,
                        result.bitmap,
                        result.base64Image
                    )

                    // 0ms Optimistic UI Update: photo appears immediately!
                    val index = studentsList.indexOfFirst { it.id == currentTargetId }
                    if (index != -1) {
                        val student = studentsList[index]
                        student.photoUrl = tempKey
                        studentsList[index] = student
                    }

                    Toast.makeText(this@PhotoDeskActivity, "📸 ${result.summaryText}\nSyncing in background...", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(errorMessage: String) {
                runOnUiThread {
                    Toast.makeText(this@PhotoDeskActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PhotoDeskScreen() {
        val filteredStudents = remember(studentsList.toList(), activeFilter, searchQuery, selectedClassId) {
            studentsList.filter { student ->
                val matchesClass = if (selectedClassId == null || selectedClassId == 0) true else student.classId == selectedClassId
                val matchesFilter = when (activeFilter) {
                    "MISSING" -> student.photoUrl.isNullOrBlank()
                    "HAS_PHOTO" -> !student.photoUrl.isNullOrBlank()
                    else -> true
                }
                val matchesSearch = if (searchQuery.isBlank()) true else {
                    val query = searchQuery.trim().lowercase()
                    (student.fullName?.lowercase()?.contains(query) == true) ||
                            (student.scholarNumber?.lowercase()?.contains(query) == true) ||
                            (student.fatherName?.lowercase()?.contains(query) == true)
                }
                matchesClass && matchesFilter && matchesSearch
            }
        }

        val totalCount = filteredStudents.size
        val withPhotoCount = filteredStudents.count { !it.photoUrl.isNullOrBlank() }
        val missingPhotoCount = totalCount - withPhotoCount
        val progress = if (totalCount > 0) withPhotoCount.toFloat() / totalCount else 0f
        val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "📸 Rapid Photo Desk",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Bulk Studio Capture & AI Auto-Framing",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { loadStudents() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF38BDF8))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
                )
            },
            containerColor = Color(0xFF0F172A)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 14.dp)
            ) {
                // Header Stat Card (Liquid Glass & Capsule Style)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A).copy(alpha = 0.8f))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Studio Progress",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "$withPhotoCount / $totalCount Captured",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (missingPhotoCount == 0) Color(0xFF10B981) else Color(0xFFF59E0B)
                            ) {
                                Text(
                                    text = if (missingPhotoCount == 0) "100% Done" else "$missingPhotoCount Missing",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF334155),
                        )
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    placeholder = { Text("Search by student or scholar number...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF38BDF8)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                // Class Selection Chips Row
                if (classList.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedClassId == null || selectedClassId == 0,
                                onClick = {
                                    selectedClassId = 0
                                    loadStudents()
                                },
                                label = { Text("All Classes", fontSize = 12.sp) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        items(classList) { cls ->
                            val isSelected = selectedClassId == cls.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedClassId = if (isSelected) 0 else cls.id
                                    loadStudents()
                                },
                                label = { Text(cls.className ?: "Class", fontSize = 12.sp) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Filter Chips Row (Kyant0 Pill Shape)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = activeFilter == "ALL",
                            onClick = { activeFilter = "ALL" },
                            label = { Text("All ($totalCount)", fontSize = 12.sp) },
                            shape = RoundedCornerShape(50)
                        )
                    }
                    item {
                        FilterChip(
                            selected = activeFilter == "MISSING",
                            onClick = { activeFilter = "MISSING" },
                            label = { Text("⚠️ Missing ($missingPhotoCount)", fontSize = 12.sp) },
                            shape = RoundedCornerShape(50)
                        )
                    }
                    item {
                        FilterChip(
                            selected = activeFilter == "HAS_PHOTO",
                            onClick = { activeFilter = "HAS_PHOTO" },
                            label = { Text("✅ Captured ($withPhotoCount)", fontSize = 12.sp) },
                            shape = RoundedCornerShape(50)
                        )
                    }
                }

                // Loading or Uploading banner
                AnimatedVisibility(visible = queueRemaining > 0 || queueActive > 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0369A1))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "☁️ Background Uploading: $queueRemaining in queue (Active: $queueActive)",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Student List
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF38BDF8))
                    }
                } else if (filteredStudents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎉 No students found matching filter", color = Color(0xFF94A3B8), fontSize = 14.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(filteredStudents, key = { it.id }) { student ->
                            StudentPhotoCard(student = student, onSnapClick = { promptPhotoSource(student.id) })
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun StudentPhotoCard(student: Student, onSnapClick: () -> Unit) {
        val hasPhoto = !student.photoUrl.isNullOrBlank()
        var bitmapState by remember(student.photoUrl) { mutableStateOf<Bitmap?>(null) }

        LaunchedEffect(student.photoUrl) {
            val url = student.photoUrl
            if (!url.isNullOrBlank()) {
                apiClient.loadImage(url, object : ApiClient.ApiCallback<Bitmap> {
                    override fun onSuccess(result: Bitmap?) {
                        bitmapState = result
                    }

                    override fun onError(errorMessage: String?) {
                        bitmapState = null
                    }
                })
            } else {
                bitmapState = null
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Frame with Kyant0 Pill / Squircle
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (hasPhoto) Color(0xFF065F46) else Color(0xFF475569))
                        .border(
                            width = 1.5.dp,
                            color = if (hasPhoto) Color(0xFF10B981) else Color(0xFF64748B),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onSnapClick() },
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = bitmapState
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Student Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = (student.fullName?.take(1) ?: "S").uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Student Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0284C7)
                        ) {
                            Text(
                                text = student.scholarNumber ?: "-",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = student.className ?: "",
                            fontSize = 11.5.sp,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = student.fullName ?: "Unnamed Student",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!student.fatherName.isNullOrBlank()) {
                        Text(
                            text = "S/D of: ${student.fatherName}",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Snap Button (Capsule Style)
                Button(
                    onClick = onSnapClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasPhoto) Color(0xFF0F766E) else Color(0xFF2563EB)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = if (hasPhoto) Icons.Default.Check else Icons.Default.CameraAlt,
                        contentDescription = "Snap",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hasPhoto) "Update" else "Snap",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
