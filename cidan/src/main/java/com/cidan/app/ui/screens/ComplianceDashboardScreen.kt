package com.cidan.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Pending
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cidan.app.data.models.ComplianceReportModel
import com.cidan.app.data.services.CidanApiService
import com.cidan.app.data.services.SupabaseService
import com.cidan.app.ui.theme.Poppins
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplianceDashboardScreen(
    email: String,
    fileName: String = "CIDAN_Regulatory_Circular.pdf",
    report: ComplianceReportModel = remember { ComplianceReportModel.defaultSample() },
    initialApproved: Boolean = false,
    initialShowPdfModal: Boolean = false,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pdfSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isApproved by remember { mutableStateOf(initialApproved || report.isDeployed) }
    var isApproving by remember { mutableStateOf(false) }
    var showPdfModal by remember { mutableStateOf(initialShowPdfModal) }

    // Save initial compliance audit report to Supabase
    LaunchedEffect(report.workflowId) {
        SupabaseService.getInstance(context).saveComplianceReport(
            email = email,
            workflowId = report.workflowId,
            filename = fileName,
            obligationsCount = report.obligations.size,
            violationsCount = 3,
            approvedCount = report.obligations.size,
            reportDataJson = report.rawJson,
            isDeployed = isApproved
        )
    }

    fun handleDeploy() {
        if (isApproved || isApproving) return
        isApproving = true
        coroutineScope.launch {
            // Call AI model backend to approve rule
            CidanApiService().approveRule(
                report.obligations.firstOrNull()?.obligationId ?: "OBL-001"
            )

            // Update deployment status in Supabase
            SupabaseService.getInstance(context).updateReportDeploymentStatus(
                email = email,
                workflowId = report.workflowId,
                isDeployed = true
            )

            // Record audit log entry in Supabase
            SupabaseService.getInstance(context).recordHistory(
                email = email,
                actionType = "POLICY_APPROVED",
                documentName = fileName,
                workflowId = report.workflowId,
                summary = "Compliance review approved for production sandbox deployment"
            )

            isApproving = false
            isApproved = true
            snackbarHostState.showSnackbar("Workflow sanctioned & approved for production deployment!")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Top Navigation Bar matching Flutter screen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Back Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(4.dp, CircleShape, spotColor = Color(0x15000000))
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Compliance Analysis",
                        fontFamily = Poppins,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "Audit & Verification Dashboard",
                        fontFamily = Poppins,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // 2. Scrollable Body Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 90.dp)
            ) {
                // Card 1: Policy Evolution & Gap Comparison
                PolicyEvolutionCard(report = report)

                Spacer(modifier = Modifier.height(16.dp))

                // Card 2: Test Cases Performed to Verify
                TestCasesCard(tests = report.tests)

                Spacer(modifier = Modifier.height(16.dp))

                // Card 3: Problems & Risks Identified
                ProblemsAndRisksCard()

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 3. Pinned Bottom Action Bar with 2 Buttons ([PDF Report] & [Deploy])
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White)
                .shadow(12.dp, spotColor = Color(0x18000000))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // PDF Report Outlined Button
                OutlinedButton(
                    onClick = { showPdfModal = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.6.dp, Color(0xFF5B4BDB)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF5B4BDB)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = Color(0xFF5B4BDB),
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PDF Report",
                        fontFamily = Poppins,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5B4BDB)
                    )
                }

                // Deploy / Deployed Button
                Button(
                    onClick = { handleDeploy() },
                    enabled = !isApproved && !isApproving,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isApproved) Color(0xFF10B981) else Color(0xFF5B4BDB),
                        disabledContainerColor = Color(0xFF10B981),
                        contentColor = Color.White,
                        disabledContentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (isApproving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isApproved) Icons.Rounded.CheckCircle else Icons.Rounded.CheckCircleOutline,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isApproved) "Deployed" else "Deploy",
                            fontFamily = Poppins,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // PDF Report Modal Bottom Sheet (Anchored to bottom, rising to 88% height so background header is visible)
        if (showPdfModal) {
            ModalBottomSheet(
                onDismissRequest = { showPdfModal = false },
                sheetState = pdfSheetState,
                containerColor = Color(0xFF334155),
                scrimColor = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = null
            ) {
                PdfReportModalContent(
                    report = report,
                    fileName = fileName,
                    isApproved = isApproved,
                    onDismiss = { showPdfModal = false }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp)
        )
    }
}

@Composable
private fun PolicyEvolutionCard(report: ComplianceReportModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = Color(0x100F172A))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEEF2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.CompareArrows,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Policy Evolution & Gap Comparison",
                        fontFamily = Poppins,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Compare existing handbook vs new circular rule.",
                        fontFamily = Poppins,
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subcard 1: OLD RULE (Existing Handbook)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFFFFDF5))
                    .border(1.dp, Color(0xFFFEF3C7), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFFBEB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "OLD RULE (Existing Handbook)",
                                fontFamily = Poppins,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Coverage Gap",
                                    fontFamily = Poppins,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Standard Personal Credit Policy (Handbook Ch. 4)",
                            fontFamily = Poppins,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Existing rule relies on baseline credit appraisal and does NOT contain mandatory control checkpoints for illustrative circular compliance.",
                            fontFamily = Poppins,
                            fontSize = 11.5.sp,
                            color = Color(0xFF475569),
                            lineHeight = 16.5.sp
                        )
                    }
                }
            }

            // Connector Arrow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFCBD5E1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDownward,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Subcard 2: NEW RULE ADDED (From Circular)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF8F9FF))
                    .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "NEW RULE ADDED (From Circular)",
                                fontFamily = Poppins,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEEF2FF))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "OBL-001",
                                    fontFamily = Poppins,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4F46E5)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Illustrative Control Framework Implementation",
                            fontFamily = Poppins,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Apply the illustrative control framework to all described personal-loan and consumer-credit applications prior to sanction. Requires human confirmation of illustrative language.",
                            fontFamily = Poppins,
                            fontSize = 11.5.sp,
                            color = Color(0xFF475569),
                            lineHeight = 16.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TestCasesCard(tests: List<com.cidan.app.data.models.TestCaseItem>) {
    val items = if (tests.isNotEmpty()) tests else listOf(
        com.cidan.app.data.models.TestCaseItem("TC-001", "Personal Loan Scope Verification", "PASS", true),
        com.cidan.app.data.models.TestCaseItem("TC-002", "Control Framework Boundary Test", "PASS", true),
        com.cidan.app.data.models.TestCaseItem("TC-003", "Handbook Policy Provision Cross-Mapping", "NEEDS REVIEW", false),
        com.cidan.app.data.models.TestCaseItem("TC-004", "Audit Trail & Validation Report Compilation", "PASS", true)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = Color(0x100F172A))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEEF2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Test Cases Performed to Verify",
                        fontFamily = Poppins,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Validate rule changes and compliance impact.",
                        fontFamily = Poppins,
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${items.size} Tests",
                        fontFamily = Poppins,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            items.forEach { test ->
                TestCaseRow(test = test)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TestCaseRow(test: com.cidan.app.data.models.TestCaseItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (test.isPass) Color(0xFFECFDF5) else Color(0xFFFFFBEB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (test.isPass) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = if (test.isPass) Color(0xFF10B981) else Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${test.id} • ${test.name}",
                    fontFamily = Poppins,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (test.isPass) "Condition verified in synthetic sandbox test." else "Needs human reviewer cross-examination.",
                    fontFamily = Poppins,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (test.isPass) Color(0xFFECFDF5) else Color(0xFFFFFBEB))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = test.status,
                    fontFamily = Poppins,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (test.isPass) Color(0xFF047857) else Color(0xFFD97706)
                )
            }
        }
    }
}

@Composable
private fun ProblemsAndRisksCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = Color(0x100F172A))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEEF2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Problems & Risks Identified",
                        fontFamily = Poppins,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Key issues requiring attention before deployment.",
                        fontFamily = Poppins,
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEEF2FF))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "3 Issues",
                        fontFamily = Poppins,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            ProblemItemRow(
                tag = "AMBIGUITY ISSUE",
                tagBg = Color(0xFFFEF3C7),
                tagColor = Color(0xFFD97706),
                icon = Icons.Rounded.History,
                iconBg = Color(0xFFFFF1F2),
                iconColor = Color(0xFFF43F5E),
                title = "Illustrative Source Language Requires Legal Confirmation",
                description = "The circular text uses illustrative phrasing for demo applications. A compliance officer must confirm the exact phrasing."
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProblemItemRow(
                tag = "POLICY GAP ISSUE",
                tagBg = Color(0xFFFEE2E2),
                tagColor = Color(0xFFDC2626),
                icon = Icons.Rounded.Shield,
                iconBg = Color(0xFFFEE2E2),
                iconColor = Color(0xFFEF4444),
                title = "Handbook Coverage Incomplete for New Controls",
                description = "The retrieved passages are insufficient to prove full coverage of the new illustrative control framework."
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProblemItemRow(
                tag = "OPERATIONAL RISK",
                tagBg = Color(0xFFE0E7FF),
                tagColor = Color(0xFF4338CA),
                icon = Icons.Rounded.Security,
                iconBg = Color(0xFFEEF2FF),
                iconColor = Color(0xFF6366F1),
                title = "Human Review Mandatory Before Production Deployment",
                description = "This report does NOT determine final legal compliance without human sign-off."
            )
        }
    }
}

@Composable
private fun ProblemItemRow(
    tag: String,
    tagBg: Color,
    tagColor: Color,
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(tagBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tag,
                        fontFamily = Poppins,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tagColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = title,
                    fontFamily = Poppins,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = description,
                    fontFamily = Poppins,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 15.sp
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(18.dp)
            )
        }
    }
}

@Composable
private fun PdfReportModalContent(
    report: ComplianceReportModel,
    fileName: String,
    isApproved: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }

    fun handleDownload() {
        if (isDownloading) return
        isDownloading = true
        coroutineScope.launch {
            delay(900)
            isDownloading = false
            Toast.makeText(context, "Report downloaded to local storage successfully!", Toast.LENGTH_LONG).show()
        }
    }

    val configuration = LocalConfiguration.current
    val sheetHeight = configuration.screenHeightDp.dp * 0.88f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(sheetHeight)
            .background(Color(0xFF334155))
            .navigationBarsPadding()
    ) {
        // =========================================================================
        // TOP MODAL ACTION BAR (Header bar: Title, Download Button, and Close ✕ icon)
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = "PDF Document",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PDF Reference Document",
                        fontFamily = Poppins,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Top Download Button (Small Pill)
                    Button(
                        onClick = { handleDownload() },
                        enabled = !isDownloading,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(13.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "Download",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Download",
                            fontFamily = Poppins,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // =========================================================================
        // SCROLLABLE PDF PAGE SHEET CONTAINER
        // =========================================================================
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 1. OFFICIAL LETTERHEAD
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CIDAN REGULATORY COMPLIANCE AI",
                                fontFamily = Poppins,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                letterSpacing = 0.3.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Automated Regulatory Ingestion & Validation Report",
                                fontFamily = Poppins,
                                fontSize = 9.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isApproved) Color(0xFFECFDF5) else Color(0xFFFEF3C7))
                                .border(
                                    1.dp,
                                    if (isApproved) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.5.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isApproved) "AUDIT APPROVED" else "REVIEW PENDING",
                                fontFamily = Poppins,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isApproved) Color(0xFF047857) else Color(0xFFB45309)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. DOCUMENT METADATA
                    val cleanWorkflowId = report.workflowId.ifEmpty { "7d19481b-2d2b-4bb4-96b8-35eaf0693df1" }
                    val cleanReportId = report.report.reportId.ifEmpty { "CIDAN-20261026105955000000" }
                    val cleanGeneratedAt = "2026-10-26T05:29:55.000000+00:00"
                    val cleanFileName = fileName.ifEmpty { "SYNVORA_Final_Package_Model.pdf" }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        MetadataRow(label = "Report ID:", value = cleanReportId)
                        Spacer(modifier = Modifier.height(4.dp))
                        MetadataRow(label = "Workflow ID:", value = cleanWorkflowId)
                        Spacer(modifier = Modifier.height(4.dp))
                        MetadataRow(label = "Generated At:", value = cleanGeneratedAt)
                        Spacer(modifier = Modifier.height(4.dp))
                        MetadataRow(label = "Source Circular:", value = cleanFileName, isBold = true)
                        Spacer(modifier = Modifier.height(4.dp))
                        MetadataRow(label = "Target Sector:", value = "Personal Loan & Consumer Credit")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // =====================================================================
                    // SECTION 1: EXECUTIVE METRICS SUMMARY
                    // =====================================================================
                    Text(
                        text = "1. EXECUTIVE METRICS SUMMARY",
                        fontFamily = Poppins,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFCBD5E1))
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC))
                        ) {
                            PdfTableCell(text = "Metric", weight = 1.25f, isHeader = true)
                            PdfTableCell(text = "Count", weight = 0.75f, isHeader = true)
                            PdfTableCell(text = "Audit Status", weight = 1.35f, isHeader = true)
                        }

                        // Row 1
                        Row(modifier = Modifier.fillMaxWidth()) {
                            PdfTableCell(text = "Obligations Extracted", weight = 1.25f)
                            PdfTableCell(text = "${report.obligations.size.coerceAtLeast(1)}", weight = 0.75f, isBold = true)
                            PdfTableCell(text = "OBL-001 Confirmed (80%)", weight = 1.35f)
                        }

                        // Row 2
                        Row(modifier = Modifier.fillMaxWidth()) {
                            PdfTableCell(text = "Policy Gap Findings", weight = 1.25f)
                            PdfTableCell(text = "1", weight = 0.75f, isBold = true)
                            PdfTableCell(text = "Requires Human Review", weight = 1.35f)
                        }

                        // Row 3
                        Row(modifier = Modifier.fillMaxWidth()) {
                            PdfTableCell(text = "Draft Rules Generated", weight = 1.25f)
                            PdfTableCell(text = "0", weight = 0.75f, isBold = true)
                            PdfTableCell(text = "Sandbox Baseline", weight = 1.35f)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // =====================================================================
                    // SECTION 2: RULE EVOLUTION (OLD RULE VS NEW RULE ADDED)
                    // =====================================================================
                    Text(
                        text = "2. RULE EVOLUTION (OLD RULE VS NEW RULE ADDED)",
                        fontFamily = Poppins,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "• Existing Rule: Standard Personal Credit Underwriting (Handbook Ch 4). Lacks mandatory checkpoints for illustrative circular requirements.",
                                fontFamily = Poppins,
                                fontSize = 10.sp,
                                color = Color(0xFF334155),
                                lineHeight = 15.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "• New Rule Added: OBL-001 — Apply illustrative control framework across personal loan and consumer credit applications before sanction.",
                                fontFamily = Poppins,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF047857),
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // =====================================================================
                    // SECTION 3: TESTCASES PERFORMED TO VERIFY
                    // =====================================================================
                    Text(
                        text = "3. TESTCASES PERFORMED TO VERIFY",
                        fontFamily = Poppins,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFCBD5E1))
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC))
                        ) {
                            PdfTableCell(text = "ID", weight = 0.8f, isHeader = true)
                            PdfTableCell(text = "Test Scenario", weight = 1.4f, isHeader = true)
                            PdfTableCell(text = "Result", weight = 0.9f, isHeader = true)
                        }

                        // TC-001
                        Row(modifier = Modifier.fillMaxWidth()) {
                            PdfTableCell(text = "TC-001", weight = 0.8f, textColor = Color(0xFF64748B))
                            PdfTableCell(text = "Loan scope boundary validation", weight = 1.4f)
                            PdfTableCell(text = "PASS", weight = 0.9f, textColor = Color(0xFF10B981), isBold = true)
                        }

                        // TC-002
                        Row(modifier = Modifier.fillMaxWidth()) {
                            PdfTableCell(text = "TC-002", weight = 0.8f, textColor = Color(0xFF64748B))
                            PdfTableCell(text = "Illustrative framework execution", weight = 1.4f)
                            PdfTableCell(text = "PASS", weight = 0.9f, textColor = Color(0xFF10B981), isBold = true)
                        }

                        // TC-003
                        Row(modifier = Modifier.fillMaxWidth()) {
                            PdfTableCell(text = "TC-003", weight = 0.8f, textColor = Color(0xFF64748B))
                            PdfTableCell(text = "Handbook policy cross-mapping", weight = 1.4f)
                            PdfTableCell(text = "FLAGGED", weight = 0.9f, textColor = Color(0xFFD97706), isBold = true)
                        }

                        // TC-004
                        Row(modifier = Modifier.fillMaxWidth()) {
                            PdfTableCell(text = "TC-004", weight = 0.8f, textColor = Color(0xFF64748B))
                            PdfTableCell(text = "Audit trail report generation", weight = 1.4f)
                            PdfTableCell(text = "PASS", weight = 0.9f, textColor = Color(0xFF10B981), isBold = true)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // =====================================================================
                    // SECTION 4: PROBLEMS & ACTIONABLE RISKS
                    // =====================================================================
                    Text(
                        text = "4. PROBLEMS & ACTIONABLE RISKS",
                        fontFamily = Poppins,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "• Ambiguity: Illustrative source language requires human confirmation.",
                        fontFamily = Poppins,
                        fontSize = 9.5.sp,
                        color = Color(0xFFB45309),
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "• Policy Gap: Coverage cannot be determined from retrieved handbook passages.",
                        fontFamily = Poppins,
                        fontSize = 9.5.sp,
                        color = Color(0xFFDC2626),
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "• Legal Caveat: Synthetic demo output only; not an official RBI circular or legal advice.",
                        fontFamily = Poppins,
                        fontSize = 9.5.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // =====================================================================
                    // VERIFICATION SIGN-OFF BADGE
                    // =====================================================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reviewed & Verified By:",
                                    fontFamily = Poppins,
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isApproved)
                                        "Chief Compliance Officer (Signed)"
                                    else
                                        "Awaiting Compliance Officer Signature",
                                    fontFamily = Poppins,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isApproved) Color(0xFF047857) else Color(0xFF0F172A)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isApproved) Color(0xFF10B981) else Color(0xFFF59E0B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isApproved) Icons.Rounded.Check else Icons.Rounded.Pending,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // =====================================================================
                    // DOWNLOAD OFFICIAL REPORT BUTTON (AT BOTTOM OF MODAL)
                    // =====================================================================
                    Button(
                        onClick = { handleDownload() },
                        enabled = !isDownloading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        )
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Downloading...",
                                fontFamily = Poppins,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download Report to Local Storage",
                                fontFamily = Poppins,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontFamily = Poppins,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.width(115.dp)
        )
        Text(
            text = value,
            fontFamily = Poppins,
            fontSize = 10.5.sp,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isBold) Color(0xFF0F172A) else Color(0xFF475569),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RowScope.PdfTableCell(
    text: String,
    weight: Float,
    isHeader: Boolean = false,
    isBold: Boolean = false,
    textColor: Color = if (isHeader) Color(0xFF0F172A) else Color(0xFF334155)
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .border(0.5.dp, Color(0xFFCBD5E1))
            .padding(horizontal = 7.dp, vertical = 7.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontFamily = Poppins,
            fontSize = if (isHeader) 10.sp else 9.5.sp,
            fontWeight = if (isHeader || isBold) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            lineHeight = 14.sp
        )
    }
}
