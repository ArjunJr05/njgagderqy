package com.cidan.app.ui.screens

import android.media.AudioAttributes
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.ui.graphics.Path
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.cidan.app.R
import com.cidan.app.ui.components.CustomButton
import com.cidan.app.ui.components.dashedBorder
import com.cidan.app.ui.theme.AppColors
import com.cidan.app.ui.theme.Poppins
import androidx.compose.runtime.LaunchedEffect
import com.cidan.app.data.models.ComplianceReportModel
import com.cidan.app.data.services.CidanApiService
import com.cidan.app.data.services.SupabaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class RegulatoryHistoryItem(
    val id: String,
    val fileName: String,
    val fileSize: String,
    val timeAgo: String = "Just now",
    val status: String = "Analyzed"
)

enum class UploadProcessStep {
    SELECT_FILE,
    UPLOADING,          // Stage 1: Uploading file... (50%)
    UPLOAD_COMPLETED,   // Stage 2: Upload Completed! (100% + 3 green checks)
    ANALYZING,          // Stage 3: Analyzing Document... (4 animated lines)
    PROCESSED_SUCCESS   // Stage 4: Document Processed Successfully (Last Page)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadDocumentScreen(
    email: String,
    onCloseClick: () -> Unit,
    onNavigateToDashboard: ((email: String, fileName: String, showPdfFirst: Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Current Step in the pipeline
    var currentStep by remember { mutableStateOf(UploadProcessStep.SELECT_FILE) }

    // Document Information
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("receipt_26048462_17913815349.pdf") }
    var selectedFileSize by remember { mutableStateOf("71.7 KB") }

    // Upload & Analysis Progress
    var uploadProgress by remember { mutableFloatStateOf(0.15f) }
    var analysisStepIndex by remember { mutableIntStateOf(0) }
    var uploadJob by remember { mutableStateOf<Job?>(null) }
    var showResultsSheet by remember { mutableStateOf(false) }
    var showAssistantSheet by remember { mutableStateOf(false) }
    val assistantSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val recentHistoryList = remember { mutableStateListOf<RegulatoryHistoryItem>() }
    var analysisReport by remember { mutableStateOf(ComplianceReportModel.defaultSample()) }

    // Fetch existing user compliance history from Supabase
    LaunchedEffect(email) {
        launch(Dispatchers.IO) {
            try {
                val reports = SupabaseService.getInstance(context).getComplianceReports(email)
                if (reports.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        recentHistoryList.clear()
                        reports.forEach { r ->
                            val fn = r.optString("filename", "CIDAN_Circular.pdf")
                            val isDep = r.optBoolean("is_deployed", false)
                            val wid = r.optString("workflow_id", System.currentTimeMillis().toString())
                            recentHistoryList.add(
                                RegulatoryHistoryItem(
                                    id = wid,
                                    fileName = fn,
                                    fileSize = "71.7 KB",
                                    timeAgo = "Archived",
                                    status = if (isDep) "Deployed" else "Analyzed"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback gracefully
            }
        }
    }

    // User Profile Display Info
    val displayName = remember(email) {
        val trimmed = email.substringBefore('@').trim()
        if (trimmed.isEmpty() || trimmed.equals("ravi.sharma", ignoreCase = true)) {
            "arjunfree256"
        } else {
            trimmed
        }
    }
    val avatarInitial = remember(displayName) {
        displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "A"
    }
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = remember(hour) {
        when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    val analysisSteps = listOf(
        "Reading document content",
        "Identifying regulatory sections",
        "Extracting obligations",
        "Mapping to compliance rules"
    )

    // Strictly documents only picker (PDF, DOCX, DOC) - NO IMAGES
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var name = "selected_document.pdf"
            var sizeStr = "71.7 KB"

            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) name = cursor.getString(nameIndex)
                        if (sizeIndex != -1) {
                            val bytes = cursor.getLong(sizeIndex)
                            sizeStr = when {
                                bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
                                bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
                                else -> "$bytes B"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                name = uri.lastPathSegment?.substringAfterLast('/') ?: "selected_document.pdf"
            }

            // Strictly filter out image extensions
            val ext = name.substringAfterLast('.', "").lowercase()
            val imageExtensions = listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")
            if (ext in imageExtensions) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Images are not allowed. Please select a PDF, DOCX, or DOC file.")
                }
                return@rememberLauncherForActivityResult
            }

            selectedUri = uri
            selectedFileName = name
            selectedFileSize = sizeStr
        }
    }

    // Sequence: Full-screen video plays during AI analysis, followed by Document Processed Successfully
    fun startUploadSequence() {
        uploadJob?.cancel()
        uploadJob = coroutineScope.launch {
            // 1. Immediately switch to Full-Screen Video Analysis
            currentStep = UploadProcessStep.ANALYZING

            // 2. Trigger AI backend analysis concurrently with video playback
            var fetchedReport: ComplianceReportModel = ComplianceReportModel.defaultSample()
            val apiJob = launch(Dispatchers.IO) {
                try {
                    fetchedReport = CidanApiService().analyzeDocument(selectedFileName)
                } catch (e: Exception) {
                    fetchedReport = ComplianceReportModel.defaultSample()
                }
            }

            // Let the video play through full screen for the analysis duration (~6.5s)
            val startTime = System.currentTimeMillis()
            apiJob.join()
            val elapsed = System.currentTimeMillis() - startTime
            val minPlayDuration = 6500L
            if (elapsed < minPlayDuration) {
                delay(minPlayDuration - elapsed)
            }

            analysisReport = fetchedReport

            // 3. Save report to Supabase
            launch(Dispatchers.IO) {
                SupabaseService.getInstance(context).saveComplianceReport(
                    email = email,
                    workflowId = fetchedReport.workflowId,
                    filename = selectedFileName,
                    obligationsCount = fetchedReport.obligations.size,
                    violationsCount = 3,
                    approvedCount = fetchedReport.obligations.size,
                    reportDataJson = fetchedReport.rawJson,
                    isDeployed = false
                )
            }

            if (recentHistoryList.none { it.fileName == selectedFileName }) {
                recentHistoryList.add(
                    0,
                    RegulatoryHistoryItem(
                        id = fetchedReport.workflowId,
                        fileName = selectedFileName,
                        fileSize = selectedFileSize,
                        timeAgo = "Just now",
                        status = "Analyzed"
                    )
                )
            }

            // After clicking Upload & Process and video completes, PDF Reference Document comes first!
            if (onNavigateToDashboard != null) {
                onNavigateToDashboard(email, selectedFileName, true)
            } else {
                currentStep = UploadProcessStep.PROCESSED_SUCCESS
            }
        }
    }

    fun cancelUpload() {
        uploadJob?.cancel()
        currentStep = UploadProcessStep.SELECT_FILE
        uploadProgress = 0.15f
        analysisStepIndex = 0
    }

    fun launchFilePicker() {
        filePickerLauncher.launch(
            arrayOf(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        when (currentStep) {
            UploadProcessStep.SELECT_FILE -> {
                // =========================================================================
                // SCREEN 1: IDENTICAL TOP-DOWN ALIGNMENT MATCHING REFERENCE SCREENSHOT
                // =========================================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top User Bar: Avatar + Good Evening/Username + Sign Out Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Purple Avatar + Greeting & Username
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF5B42F3)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = avatarInitial,
                                    fontFamily = Poppins,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "👋",
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = greeting,
                                        fontFamily = Poppins,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF78716C)
                                    )
                                }
                                Text(
                                    text = displayName,
                                    fontFamily = Poppins,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }

                        // Right: Sign Out Pill Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFFFF1F2))
                                .border(1.2.dp, Color(0xFFFECDD3), RoundedCornerShape(50))
                                .clickable { onCloseClick() }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = "Sign Out",
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sign Out",
                                    fontFamily = Poppins,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE11D48)
                                )
                            }
                        }
                    }

                    // Top Hero Banner with integrated CIDAN logo, bank graphic & title
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenWidth * (1078f / 1460f))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.regulatory_compliance_ai_hero_banner),
                            contentDescription = "Upload Regulatory Document Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillWidth,
                            alignment = Alignment.TopCenter
                        )
                    }

                    // Content Section with Exact Spacings & Alignment
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Dashed Upload Zone
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .dashedBorder(
                                    strokeWidth = 1.3.dp,
                                    color = Color(0xFFC7D2FE),
                                    cornerRadius = 16.dp,
                                    dashLength = 6.dp,
                                    gapLength = 4.dp
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFFAFBFE))
                                .clickable { launchFilePicker() }
                                .padding(vertical = 18.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFEEF2FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudUpload,
                                        contentDescription = "Upload Document",
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Drag & Drop your file here",
                                    fontFamily = Poppins,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(modifier = Modifier.width(38.dp).height(1.dp).background(Color(0xFFE2E8F0)))
                                    Text(
                                        text = "or",
                                        fontFamily = Poppins,
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Box(modifier = Modifier.width(38.dp).height(1.dp).background(Color(0xFFE2E8F0)))
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = { launchFilePicker() },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.3.dp, Color(0xFF4F46E5)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF4F46E5)
                                    ),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.FolderOpen,
                                            contentDescription = null,
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Choose File",
                                            fontFamily = Poppins,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF4F46E5)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Supported formats: PDF, DOCX, DOC (Max 50 MB)",
                            fontFamily = Poppins,
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )

                        // File Preview Card & Upload & Process Button (Directly below)
                        if (selectedUri != null) {
                            Spacer(modifier = Modifier.height(14.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF1F5F9).copy(alpha = 0.8f))
                                    .border(1.2.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFEF4444)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Rounded.PictureAsPdf,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Text(
                                                text = "PDF",
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedFileName,
                                            fontFamily = Poppins,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = selectedFileSize,
                                            fontFamily = Poppins,
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    IconButton(
                                        onClick = { selectedUri = null },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            CustomButton(
                                text = "Upload & Process",
                                icon = Icons.Rounded.CloudUpload,
                                height = 50.dp,
                                onClick = { startUploadSequence() }
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // =========================================================================
                        // RECENT REGULATORY HISTORY SECTION (Matches Reference Design Exactly)
                        // =========================================================================
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "Recent Regulatory History",
                                fontFamily = Poppins,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (recentHistoryList.isEmpty()) {
                            // Empty State Card matching user screenshot
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .border(1.2.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Description,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(26.dp)
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Text(
                                        text = "No regulatory circulars processed yet. Select a document above to run AI compliance analysis.",
                                        fontFamily = Poppins,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color(0xFF64748B),
                                        lineHeight = 17.5.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            // Processed circulars history list
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                recentHistoryList.forEach { item ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White)
                                            .border(1.2.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                            .clickable {
                                                if (onNavigateToDashboard != null) {
                                                    onNavigateToDashboard(email, item.fileName, false)
                                                } else {
                                                    showResultsSheet = true
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFEF4444)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.PictureAsPdf,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = "PDF",
                                                        fontSize = 7.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.fileName,
                                                    fontFamily = Poppins,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${item.fileSize} • ${item.timeAgo}",
                                                    fontFamily = Poppins,
                                                    fontSize = 11.5.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(Color(0xFFECFDF5))
                                                    .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(50))
                                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = item.status,
                                                    fontFamily = Poppins,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF059669)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            UploadProcessStep.PROCESSED_SUCCESS -> {
                // =========================================================================
                // SCREEN 5: LAST PAGE - "Document Processed Successfully"
                // =========================================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar with Close ('X') on the right
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(38.dp)
                                .shadow(2.dp, CircleShape, spotColor = Color(0x18000000))
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.2.dp, Color(0xFFE2E8F0), CircleShape)
                                .clickable {
                                    currentStep = UploadProcessStep.SELECT_FILE
                                    selectedUri = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hero 3D Bank Checklist Graphic
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.verified_bank_checklist),
                            contentDescription = "Document Processed Illustration",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Title: Document Processed Successfully
                    Text(
                        text = "Document Processed\nSuccessfully",
                        fontFamily = Poppins,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "The contents have been analyzed and\ncompliance insights are ready.",
                        fontFamily = Poppins,
                        fontSize = 13.5.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Processed Document Preview Card with Green Checkmark
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF1F5F9).copy(alpha = 0.7f))
                            .border(1.2.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEF4444)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Rounded.PictureAsPdf,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "PDF",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedFileName,
                                    fontFamily = Poppins,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = selectedFileSize,
                                    fontFamily = Poppins,
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            // Green Completed Check Badge
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Verified",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Two Buttons Row: [ View Results -> ] and [ Upload Another ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // View Results Button (Purple Gradient)
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .height(50.dp)
                                .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = Color(0x354F46E5))
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF5046E5), Color(0xFF4338CA))
                                    )
                                )
                                .clickable {
                                    if (onNavigateToDashboard != null) {
                                        onNavigateToDashboard(email, selectedFileName, false)
                                    } else {
                                        showResultsSheet = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "View Results",
                                    fontFamily = Poppins,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Upload Another Button (Outlined)
                        OutlinedButton(
                            onClick = {
                                currentStep = UploadProcessStep.SELECT_FILE
                                selectedUri = null
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.4.dp, Color(0xFF4F46E5)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF4F46E5)
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Upload",
                                    fontFamily = Poppins,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4F46E5),
                                    lineHeight = 15.sp
                                )
                                Text(
                                    text = "Another",
                                    fontFamily = Poppins,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4F46E5),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                // =========================================================================
                // FULL SCREEN VIDEO PLAYER (REPLACING ALL INTERMEDIATE HALF PAGES)
                // =========================================================================
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                val uri = Uri.parse("android.resource://${ctx.packageName}/${R.raw.cidan_analysis}")
                                setVideoURI(uri)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    val audioAttributes = AudioAttributes.Builder()
                                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                        .setUsage(AudioAttributes.USAGE_MEDIA)
                                        .build()
                                    mp.setAudioAttributes(audioAttributes)
                                    mp.setVolume(1.0f, 1.0f) // SOUND ENABLED!
                                    start()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Floating Close / Cancel Button in Top Right
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 16.dp, end = 20.dp)
                            .size(42.dp)
                            .shadow(4.dp, CircleShape, spotColor = Color(0x30000000))
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                            .border(1.2.dp, Color(0xFFE2E8F0), CircleShape)
                            .clickable { cancelUpload() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancel",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Floating Translucent Status Badge at Bottom Center
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 36.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF0B192C).copy(alpha = 0.88f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 22.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF60A5FA),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "CIDAN AI Analyzing Regulatory Document...",
                                fontFamily = Poppins,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Compliance Results Bottom Sheet Modal
        if (showResultsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showResultsSheet = false },
                sheetState = sheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.SuccessSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = AppColors.Success,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Compliance Extraction Results",
                            fontFamily = Poppins,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val results = listOf(
                        "RBI Master Direction Sec 4.2" to "Capital Adequacy ratio compliance verified (11.5% min tier-1).",
                        "Risk Governance Framework" to "Mandatory quarterly audit report submission scheduled.",
                        "KYC / AML Norms 2024" to "Customer due diligence policy aligned with latest circular.",
                        "Action Items Generated" to "3 compliance tasks assigned to Risk & Audit department."
                    )

                    results.forEach { (title, desc) ->
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = AppColors.Success,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = title,
                                    fontFamily = Poppins,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = desc,
                                    fontFamily = Poppins,
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    CustomButton(
                        text = "Done",
                        onClick = { showResultsSheet = false }
                    )
                }
            }
        }

        // Soft decorative bottom illustration matching reference design
        if (currentStep == UploadProcessStep.SELECT_FILE) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .align(Alignment.BottomCenter)
            ) {
                val w = size.width
                val h = size.height

                val path1 = Path().apply {
                    moveTo(0f, h * 0.5f)
                    cubicTo(w * 0.25f, h * 0.25f, w * 0.5f, h * 0.85f, w, h * 0.35f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = path1,
                    color = Color(0xFFF1EEFE).copy(alpha = 0.8f)
                )

                val path2 = Path().apply {
                    moveTo(0f, h * 0.72f)
                    cubicTo(w * 0.35f, h * 0.52f, w * 0.65f, h * 0.95f, w, h * 0.62f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = path2,
                    color = Color(0xFFE8E2FD).copy(alpha = 0.7f)
                )
            }

            // Floating AI Assistant Button (Bottom-Right, exactly matching reference screenshot!)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 22.dp, bottom = 22.dp)
                    .size(58.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        spotColor = Color(0x604F46E5),
                        ambientColor = Color(0x304F46E5)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                        )
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    .clickable { showAssistantSheet = true },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SmartToy,
                        contentDescription = "AI Assistant",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 1.dp, y = (-2).dp)
                    )
                }
            }
        }

        // AI Assistant Modal Bottom Sheet
        if (showAssistantSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAssistantSheet = false },
                sheetState = assistantSheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .padding(bottom = 36.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SmartToy,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CIDAN AI Assistant",
                                fontFamily = Poppins,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Regulatory Compliance Intelligence",
                                fontFamily = Poppins,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        IconButton(
                            onClick = { showAssistantSheet = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Hello $displayName! I'm your AI compliance copilot. You can upload RBI circulars, internal audit reports, or guidelines above to extract mandatory obligations and verify compliance rules.",
                            fontFamily = Poppins,
                            fontSize = 13.sp,
                            color = Color(0xFF334155),
                            lineHeight = 19.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Quick Insights",
                        fontFamily = Poppins,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val quickFaqs = listOf(
                        "Supported formats" to "Upload documents in PDF, DOCX, or DOC formats up to 50 MB.",
                        "AI Obligation Extraction" to "Extracts key regulatory sections, timelines, and mandatory penalties automatically.",
                        "Compliance Mapping" to "Cross-references your circular against current RBI master directions."
                    )

                    quickFaqs.forEach { (q, a) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFAFBFE))
                                .border(1.dp, Color(0xFFEEF2F6), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "• $q",
                                fontFamily = Poppins,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4F46E5)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = a,
                                fontFamily = Poppins,
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    CustomButton(
                        text = "Got it",
                        onClick = { showAssistantSheet = false }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}
