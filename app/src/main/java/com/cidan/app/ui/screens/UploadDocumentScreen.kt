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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Verified
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

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
    onCloseClick: () -> Unit
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

    // Sequence: Step 1 (Uploading) -> Step 2 (Upload Completed) -> Step 3 (Analyzing Document) -> Step 4 (Document Processed Successfully)
    fun startUploadSequence() {
        uploadJob?.cancel()
        uploadJob = coroutineScope.launch {
            // 1. PAGE 1: UPLOADING
            currentStep = UploadProcessStep.UPLOADING
            uploadProgress = 0.15f
            delay(500)
            uploadProgress = 0.35f
            delay(500)
            uploadProgress = 0.50f
            delay(600)
            uploadProgress = 0.75f
            delay(500)
            uploadProgress = 1.0f
            delay(400)

            // 2. PAGE 2: UPLOAD COMPLETED!
            currentStep = UploadProcessStep.UPLOAD_COMPLETED
            delay(1600)

            // 3. PAGE 3: ANALYZING DOCUMENT...
            currentStep = UploadProcessStep.ANALYZING
            analysisStepIndex = 0

            for (i in 1..4) {
                delay(850)
                analysisStepIndex = i
            }

            delay(600)

            // 4. LAST PAGE: DOCUMENT PROCESSED SUCCESSFULLY
            currentStep = UploadProcessStep.PROCESSED_SUCCESS
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

                        // Top-Right Close Button ('X')
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(top = 10.dp, end = 16.dp)
                                .size(38.dp)
                                .shadow(2.dp, CircleShape, spotColor = Color(0x18000000))
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.2.dp, Color(0xFFE2E8F0), CircleShape)
                                .clickable { onCloseClick() },
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

                    // Content Section with Exact Spacings & Alignment
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(14.dp))

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
                                Icon(
                                    imageVector = Icons.Outlined.Description,
                                    contentDescription = "Document Icon",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(34.dp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

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

                        // File Preview Card & Upload & Process Button (Directly below, with identical alignment!)
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
                                .clickable { showResultsSheet = true },
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
                // COMMON TOP VIDEO PLAYER VIEW FOR STAGES 2, 3, AND 4 (WITH SOUND ENABLED!)
                // =========================================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(310.dp)
                            .background(Color(0xFFFAFBFE))
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    val uri = Uri.parse("android.resource://${ctx.packageName}/${R.raw.loading_video}")
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

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(top = 16.dp, end = 20.dp)
                                .size(42.dp)
                                .shadow(2.dp, CircleShape, spotColor = Color(0x18000000))
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.2.dp, Color(0xFFE2E8F0), CircleShape)
                                .clickable { cancelUpload() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    when (currentStep) {
                        UploadProcessStep.UPLOADING -> {
                            // STAGE 1: "Uploading file..."
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Uploading file...",
                                    fontFamily = Poppins,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "$selectedFileName  $selectedFileSize",
                                    fontFamily = Poppins,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFEEF0FA))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(uploadProgress)
                                                .height(12.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF4F46E5))
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Text(
                                        text = "${(uploadProgress * 100).toInt()}%",
                                        fontFamily = Poppins,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4F46E5)
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                OutlinedButton(
                                    onClick = { cancelUpload() },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.2.dp, Color(0xFFE2E8F0)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF0F172A)
                                    ),
                                    modifier = Modifier.height(42.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = null,
                                            tint = Color(0xFF0F172A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Cancel",
                                            fontFamily = Poppins,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Your file is securely uploaded.",
                                        fontFamily = Poppins,
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        UploadProcessStep.UPLOAD_COMPLETED -> {
                            // STAGE 2: "Upload Completed!"
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "Upload Completed!",
                                    fontFamily = Poppins,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "$selectedFileName  $selectedFileSize",
                                    fontFamily = Poppins,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(22.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF4F46E5))
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Text(
                                        text = "100%",
                                        fontFamily = Poppins,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4F46E5)
                                    )
                                }

                                Spacer(modifier = Modifier.height(26.dp))

                                val completedChecks = listOf(
                                    "File uploaded successfully",
                                    "Scanning document",
                                    "Preparing for analysis"
                                )

                                completedChecks.forEach { item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = item,
                                            fontFamily = Poppins,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }
                            }
                        }

                        UploadProcessStep.ANALYZING -> {
                            // STAGE 3: "Analyzing Document..."
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "Analyzing Document...",
                                    fontFamily = Poppins,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Extracting key information from the circular.",
                                    fontFamily = Poppins,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(26.dp))

                                analysisSteps.forEachIndexed { index, step ->
                                    val isDone = index < analysisStepIndex
                                    val isCurrent = index == analysisStepIndex

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 9.dp)
                                    ) {
                                        if (isDone) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else if (isCurrent) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.2.dp,
                                                color = Color(0xFF4F46E5)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .border(1.6.dp, Color(0xFFCFCBF3), CircleShape)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Text(
                                            text = step,
                                            fontFamily = Poppins,
                                            fontSize = 14.sp,
                                            fontWeight = if (isCurrent || isDone) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isDone) Color(0xFF0F172A) else (if (isCurrent) Color(0xFF4F46E5) else Color(0xFF64748B))
                                        )
                                    }
                                }
                            }
                        }

                        else -> {}
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}
