package com.asacademy.schoolapp

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asacademy.schoolapp.models.Student
import com.asacademy.schoolapp.network.ApiClient
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class StudentIdCardActivity : ComponentActivity() {

    private var student: Student? = null
    private var qrBitmap by mutableStateOf<Bitmap?>(null)
    private var avatarBitmap by mutableStateOf<Bitmap?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        student = intent.getSerializableExtra("student") as? Student
        if (student == null) {
            finish()
            return
        }

        generateQr()
        loadAvatar()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF38BDF8),
                    secondary = Color(0xFF818CF8),
                    background = Color(0xFF0B1120),
                    surface = Color(0xFF1E293B)
                )
            ) {
                StudentIdCardScreen()
            }
        }
    }

    private fun generateQr() {
        val s = student ?: return
        val qrContent = "AS_ACADEMY|SCHOLAR:${s.scholarNumber}|NAME:${s.fullName}|CLASS:${s.className}|DOB:${s.dob}|PHONE:${s.mobile}"
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 260, 260)
            val w = bitMatrix.width
            val h = bitMatrix.height
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
            for (x in 0 until w) {
                for (y in 0 until h) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            qrBitmap = bmp
        } catch (ignored: Exception) {}
    }

    private fun loadAvatar() {
        val url = student?.photoUrl
        if (!url.isNullOrBlank()) {
            val apiClient = ApiClient.getInstance(this)
            val fullUrl = if (url.startsWith("http")) url else apiClient.baseUrl + (if (url.startsWith("/")) url else "/$url")
            Thread {
                try {
                    val conn = java.net.URL(fullUrl).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 5000
                    val stream = conn.inputStream
                    val bmp = android.graphics.BitmapFactory.decodeStream(stream)
                    stream.close()
                    runOnUiThread { avatarBitmap = bmp }
                } catch (ignored: Exception) {}
            }.start()
        }
    }

    private fun shareIdCard() {
        val s = student ?: return
        val shareText = """
            🏫 A.S. ACADEMY HIGHER SECONDARY SCHOOL
            🪪 STUDENT IDENTITY CARD
            ------------------------------------
            • Scholar No: ${s.scholarNumber ?: "-"}
            • Name: ${s.fullName ?: "-"}
            • Class: ${s.className ?: "-"}
            • Father's Name: ${s.fatherName ?: "-"}
            • DOB: ${s.dob ?: "-"}
            • Samagra ID (SSSMID): ${s.sssmid ?: "-"}
            • Parent Phone: ${s.mobile ?: "-"}
            ------------------------------------
            Verified by A.S. Academy Digital Portal
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Student ID Card - ${s.fullName}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share Student ID Card"))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun StudentIdCardScreen() {
        val s = student ?: return

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Student Identity Card", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { shareIdCard() }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF38BDF8))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B1120))
                )
            },
            containerColor = Color(0xFF0B1120)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Identity Card Body (Kyant0 Continuous Curve Style)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // School Header Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF1E3A8A), Color(0xFF1E40AF))
                                    )
                                )
                                .padding(vertical = 18.dp, horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "A.S. ACADEMY",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "HIGHER SECONDARY SCHOOL",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF93C5FD),
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFFF59E0B)
                                ) {
                                    Text(
                                        text = "STUDENT IDENTITY CARD • 2026-27",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF78350F)
                                    )
                                }
                            }
                        }

                        // Gold accent line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFF59E0B), Color(0xFFFDE68A), Color(0xFFF59E0B))
                                    )
                                )
                        )

                        // Student Photo & Core Info Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar Box (Kyant0 Squircle / Rounded Rect)
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .shadow(8.dp, RoundedCornerShape(20.dp))
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(2.5.dp, Color(0xFF1E40AF), RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarBitmap != null) {
                                    Image(
                                        bitmap = avatarBitmap!!.asImageBitmap(),
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = (s.fullName?.take(1) ?: "S").uppercase(),
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = s.fullName ?: "Student Name",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )

                            if (!s.nameHindi.isNullOrBlank()) {
                                Text(
                                    text = s.nameHindi,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Badges Row
                            Row(horizontalArrangement = Arrangement.Center) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF1E40AF)
                                ) {
                                    Text(
                                        text = "Scholar: ${s.scholarNumber ?: "-"}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF0D9488)
                                ) {
                                    Text(
                                        text = s.className ?: "Class",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Details Table
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .padding(12.dp)
                            ) {
                                IdCardRow(label = "Father's Name", value = s.fatherName ?: "-")
                                IdCardRow(label = "Mother's Name", value = s.motherName ?: "-")
                                IdCardRow(label = "Date of Birth", value = s.dob ?: "-")
                                IdCardRow(label = "Samagra ID (SSSMID)", value = s.sssmid ?: "-")
                                IdCardRow(label = "Aadhaar Card", value = s.aadhaar ?: "-")
                                IdCardRow(label = "Emergency Mobile", value = s.mobile ?: "-")
                                IdCardRow(label = "Address", value = s.address ?: "-")
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // QR & Principal Stamp Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // QR Code
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (qrBitmap != null) {
                                        Image(
                                            bitmap = qrBitmap!!.asImageBitmap(),
                                            contentDescription = "QR Code",
                                            modifier = Modifier
                                                .size(72.dp)
                                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                                .padding(3.dp)
                                        )
                                    } else {
                                        Icon(Icons.Default.QrCode, contentDescription = "QR", modifier = Modifier.size(72.dp), tint = Color.Gray)
                                    }
                                    Text(text = "Scan to Verify", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                }

                                // Principal Sign
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "✍️ A.S. Academy",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E40AF)
                                        )
                                    }
                                    Box(modifier = Modifier.width(100.dp).height(1.dp).background(Color(0xFF94A3B8)))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "Principal Signature", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Share Action Button
                Button(
                    onClick = { shareIdCard() },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Student ID Card Details", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }

    @Composable
    private fun IdCardRow(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 11.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
            Text(
                text = value,
                fontSize = 12.sp,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
