package com.asacademy.schoolapp

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.asacademy.schoolapp.models.ApiResponses
import com.asacademy.schoolapp.models.SchoolClass
import com.asacademy.schoolapp.models.Student
import com.asacademy.schoolapp.network.ApiClient
import com.asacademy.schoolapp.utils.ImageEnhancer
import com.asacademy.schoolapp.utils.PhotoUploadManager
import java.io.File

/**
 * Kyant0 Liquid Glass Rapid Photo Desk.
 * - Single-class locked studio capture (prevents OOMs and large dumps)
 * - In-app CameraX studio viewfinder (no process death, zero activity reload)
 * - Kyant0 Liquid Glass UI with frosted acrylic, neon cyber borders, and squircle pills
 */
class PhotoDeskActivity : ComponentActivity() {

    private lateinit var apiClient: ApiClient
    private lateinit var uploadManager: PhotoUploadManager
    private var targetStudentId: Int = 0

    private val PREFS_NAME = "PhotoDeskPrefs"
    private val KEY_LOCKED_CLASS_ID = "photo_desk_locked_class_id"

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

    // In-App CameraX Studio Launcher: Runs 100% inside app process. Zero OS reloads!
    private val inAppCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val imagePath = result.data?.getStringExtra(InAppCameraActivity.EXTRA_IMAGE_PATH)
            if (!imagePath.isNullOrBlank()) {
                val photoFile = File(imagePath)
                if (photoFile.exists()) {
                    processAndUploadCapturedPhoto(photoFile)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiClient = ApiClient.getInstance(this)
        uploadManager = PhotoUploadManager.getInstance(this)
        uploadManager.registerListener(uploadListener)

        // Restore or initialize locked single class
        val intentClassId = intent.getIntExtra("class_id", -1)
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (intentClassId > 0) {
            selectedClassId = intentClassId
            prefs.edit().putInt(KEY_LOCKED_CLASS_ID, intentClassId).apply()
        } else {
            val saved = prefs.getInt(KEY_LOCKED_CLASS_ID, 0)
            if (saved > 0) {
                selectedClassId = saved
            }
        }

        loadClasses()
        if (selectedClassId != null && selectedClassId!! > 0) {
            loadStudents()
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF38BDF8),
                    secondary = Color(0xFF818CF8),
                    background = Color(0xFF0B0F19),
                    surface = Color(0xFF161F30),
                    surfaceVariant = Color(0xFF1E293B),
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

                    // Strict single-class locking: if nothing selected or invalid, lock to first class
                    val currentId = selectedClassId
                    if ((currentId == null || currentId <= 0 || classList.none { it.id == currentId }) && classList.isNotEmpty()) {
                        val firstId = classList.first().id
                        selectedClassId = firstId
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit().putInt(KEY_LOCKED_CLASS_ID, firstId).apply()
                        loadStudents()
                    }
                }
            }

            override fun onError(errorMessage: String) {
                // Ignore or log
            }
        })
    }

    private fun loadStudents() {
        val classId = selectedClassId ?: return
        if (classId <= 0) return

        isLoading = true
        val endpoint = "api/mobile/students?classId=$classId"
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
        val targetStudent = studentsList.firstOrNull { it.id == targetStudentId }

        val intent = Intent(this, InAppCameraActivity::class.java).apply {
            putExtra(InAppCameraActivity.EXTRA_STUDENT_ID, targetStudentId)
            putExtra(InAppCameraActivity.EXTRA_STUDENT_NAME, targetStudent?.fullName ?: "")
            putExtra(InAppCameraActivity.EXTRA_SCHOLAR_NUM, targetStudent?.scholarNumber ?: "")
            putExtra(InAppCameraActivity.EXTRA_CLASS_NAME, targetStudent?.className ?: "")
        }
        inAppCameraLauncher.launch(intent)
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

                    Toast.makeText(this@PhotoDeskActivity, "⚡ ${result.summaryText}\nSyncing in background...", Toast.LENGTH_SHORT).show()
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
        val currentClass = classList.firstOrNull { it.id == selectedClassId }
        val classNameDisplay = currentClass?.className ?: "Selected Class"

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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "📸 Photo Studio",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0x3338BDF8),
                                    border = BorderStroke(1.dp, Color(0x6638BDF8))
                                ) {
                                    Text(
                                        text = classNameDisplay,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }
                            Text(
                                text = "Liquid Glass Studio • Single Class Locked",
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B0F19))
                )
            },
            containerColor = Color(0xFF0B0F19)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 14.dp)
            ) {
                // Kyant0 Liquid Glass Stat Card with glowing neon border
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161F30)),
                    border = BorderStroke(1.dp, Color(0x3338BDF8)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF161F30), Color(0xFF0E1626))
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
                                    text = "$classNameDisplay Progress",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "$withPhotoCount / $totalCount Captured",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (missingPhotoCount == 0) Color(0x3310B981) else Color(0x33F59E0B),
                                border = BorderStroke(1.dp, if (missingPhotoCount == 0) Color(0xFF10B981) else Color(0xFFF59E0B))
                            ) {
                                Text(
                                    text = if (missingPhotoCount == 0) "✨ 100% Ready" else "⚠️ $missingPhotoCount Left",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (missingPhotoCount == 0) Color(0xFF34D399) else Color(0xFFFBBF24)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF1E293B),
                        )
                    }
                }

                // Kyant0 Single-Class Locked Selector Strip (NO "All Classes" dumping)
                if (classList.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            text = "SELECT CLASS (LOCKED):",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(classList) { cls ->
                                val isSelected = selectedClassId == cls.id
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isSelected) Color(0xFF0284C7) else Color(0xFF161F30),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF38BDF8) else Color(0x33334155)
                                    ),
                                    modifier = Modifier.clickable {
                                        if (selectedClassId != cls.id) {
                                            selectedClassId = cls.id
                                            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                                .edit().putInt(KEY_LOCKED_CLASS_ID, cls.id).apply()
                                            loadStudents()
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = cls.className ?: "Class",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Kyant0 Liquid Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    placeholder = { Text("Search by name, scholar no, father...", fontSize = 13.sp, color = Color(0xFF64748B)) },
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
                        unfocusedBorderColor = Color(0x3338BDF8),
                        focusedContainerColor = Color(0xFF161F30),
                        unfocusedContainerColor = Color(0xFF161F30),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                // Filter Chips Row (Kyant0 Pill Shape)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("ALL", "All ($totalCount)", Color(0xFF38BDF8)),
                        Triple("MISSING", "⚠️ Left ($missingPhotoCount)", Color(0xFFF59E0B)),
                        Triple("HAS_PHOTO", "✨ Done ($withPhotoCount)", Color(0xFF10B981))
                    ).forEach { (key, label, accentColor) ->
                        val isSel = activeFilter == key
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSel) accentColor.copy(alpha = 0.2f) else Color(0xFF161F30),
                            border = BorderStroke(
                                1.dp,
                                if (isSel) accentColor else Color(0x2238BDF8)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeFilter = key }
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier
                                    .padding(vertical = 7.dp)
                                    .fillMaxWidth(),
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else Color(0xFF94A3B8),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Cloud Background Uploading Banner (Liquid Glass Style)
                AnimatedVisibility(visible = queueRemaining > 0 || queueActive > 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x330284C7)),
                        border = BorderStroke(1.dp, Color(0x6638BDF8))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "☁️ Background Sync: $queueRemaining remaining (Active: $queueActive)",
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
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
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161F30)),
            border = BorderStroke(1.dp, if (hasPhoto) Color(0x3310B981) else Color(0x2238BDF8)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Squircle Avatar Frame with Kyant0 glow
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (hasPhoto) Color(0xFF064E3B) else Color(0xFF1E293B))
                        .border(
                            width = 1.5.dp,
                            color = if (hasPhoto) Color(0xFF10B981) else Color(0x4438BDF8),
                            shape = RoundedCornerShape(16.dp)
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
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Student Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x330284C7),
                            border = BorderStroke(1.dp, Color(0x5538BDF8))
                        ) {
                            Text(
                                text = "SCH: " + (student.scholarNumber ?: "-"),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF38BDF8)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = student.className ?: "",
                            fontSize = 11.5.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

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
                            text = "Father: ${student.fatherName}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Liquid Glass Snap / Update Button (Capsule Style)
                Button(
                    onClick = onSnapClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasPhoto) Color(0xFF0F766E) else Color(0xFF0284C7)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(40.dp)
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
