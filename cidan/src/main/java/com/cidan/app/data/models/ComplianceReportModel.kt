package com.cidan.app.data.models

import org.json.JSONArray
import org.json.JSONObject

/**
 * Compliance Report Model representing Virtusa Agent Workflow Analysis Output
 */
data class ComplianceReportModel(
    val workflowId: String,
    val stage: String,
    val error: String? = null,
    val obligations: List<ObligationItem> = emptyList(),
    val findings: List<FindingItem> = emptyList(),
    val tests: List<TestCaseItem> = emptyList(),
    val report: ReportSummary = ReportSummary(),
    val isDeployed: Boolean = false,
    val rawJson: String = ""
) {
    companion object {
        fun fromJson(json: JSONObject): ComplianceReportModel {
            val workflowId = json.optString("workflow_id", "7d19481b-2d2b-4bb4-96b8-35eaf0693df1")
            val stage = json.optString("stage", "awaiting_human_review")
            val error = if (json.has("error") && !json.isNull("error")) json.getString("error") else null

            val obligationsList = mutableListOf<ObligationItem>()
            val rawObligations = json.optJSONArray("obligations")
            if (rawObligations != null) {
                for (i in 0 until rawObligations.length()) {
                    val obj = rawObligations.optJSONObject(i)
                    if (obj != null) obligationsList.add(ObligationItem.fromJson(obj))
                }
            }

            val findingsList = mutableListOf<FindingItem>()
            val rawFindings = json.optJSONArray("findings")
            if (rawFindings != null) {
                for (i in 0 until rawFindings.length()) {
                    val obj = rawFindings.optJSONObject(i)
                    if (obj != null) findingsList.add(FindingItem.fromJson(obj))
                }
            }

            val testsList = mutableListOf<TestCaseItem>()
            val rawTests = json.optJSONArray("tests")
            if (rawTests != null) {
                for (i in 0 until rawTests.length()) {
                    val obj = rawTests.optJSONObject(i)
                    if (obj != null) testsList.add(TestCaseItem.fromJson(obj))
                }
            }

            val reportObj = json.optJSONObject("report")
            val reportSummary = if (reportObj != null) ReportSummary.fromJson(reportObj) else ReportSummary()
            val isDeployed = json.optBoolean("is_deployed", false)

            return ComplianceReportModel(
                workflowId = workflowId,
                stage = stage,
                error = error,
                obligations = obligationsList,
                findings = findingsList,
                tests = testsList,
                report = reportSummary,
                isDeployed = isDeployed,
                rawJson = json.toString()
            )
        }

        fun defaultSample(): ComplianceReportModel {
            return ComplianceReportModel(
                workflowId = "7d19481b-2d2b-4bb4-96b8-35eaf0693df1",
                stage = "awaiting_human_review",
                obligations = listOf(
                    ObligationItem(
                        obligationId = "OBL-001",
                        text = "Apply the illustrative control framework to the described loan applications.",
                        applicability = "Synthetic demo applications described in the circular.",
                        productType = "personal loan",
                        requiredAction = "Apply the illustrative control framework prior to credit sanction.",
                        confidence = 0.85,
                        status = "needs_review"
                    )
                ),
                findings = listOf(
                    FindingItem(
                        obligationId = "OBL-001",
                        status = "requires_human_review",
                        reasoning = "The retrieved handbook evidence is insufficient to confirm policy coverage.",
                        missingOrInconsistent = "Coverage cannot be determined from the retrieved passages.",
                        operationalImpact = "A reviewer should check the full handbook before drawing conclusions.",
                        recommendedAction = "Review the cited circular and relevant handbook sections.",
                        confidence = 0.4
                    )
                ),
                tests = listOf(
                    TestCaseItem("TC-001", "Personal Loan Scope Verification", "PASS", true),
                    TestCaseItem("TC-002", "Control Framework Boundary Test", "PASS", true),
                    TestCaseItem("TC-003", "Handbook Policy Provision Cross-Mapping", "NEEDS REVIEW", false),
                    TestCaseItem("TC-004", "Audit Trail & Validation Report Compilation", "PASS", true)
                ),
                report = ReportSummary(
                    reportId = "CIDAN-20261026105955000000",
                    executiveSummary = "Extracted 1 obligations and generated 1 policy findings. 3 of 4 tests verified successfully.",
                    operationalImpact = "1 policy finding requires human verification before final deployment.",
                    disclaimer = "Synthetic demo output only; not an official RBI circular or legal advice."
                ),
                isDeployed = false
            )
        }
    }
}

data class ObligationItem(
    val obligationId: String = "OBL-001",
    val text: String = "",
    val applicability: String = "",
    val productType: String = "personal loan",
    val requiredAction: String = "",
    val confidence: Double = 0.8,
    val status: String = "needs_review"
) {
    companion object {
        fun fromJson(json: JSONObject): ObligationItem {
            return ObligationItem(
                obligationId = json.optString("obligation_id", "OBL-001"),
                text = json.optString("text", ""),
                applicability = json.optString("applicability", ""),
                productType = json.optString("product_type", "personal loan"),
                requiredAction = json.optString("required_action", ""),
                confidence = json.optDouble("confidence", 0.8),
                status = json.optString("status", "needs_review")
            )
        }
    }
}

data class FindingItem(
    val obligationId: String = "OBL-001",
    val status: String = "requires_human_review",
    val reasoning: String = "",
    val missingOrInconsistent: String = "",
    val operationalImpact: String = "",
    val recommendedAction: String = "",
    val confidence: Double = 0.4
) {
    companion object {
        fun fromJson(json: JSONObject): FindingItem {
            return FindingItem(
                obligationId = json.optString("obligation_id", "OBL-001"),
                status = json.optString("status", "requires_human_review"),
                reasoning = json.optString("reasoning", ""),
                missingOrInconsistent = json.optString("missing_or_inconsistent", ""),
                operationalImpact = json.optString("operational_impact", ""),
                recommendedAction = json.optString("recommended_action", ""),
                confidence = json.optDouble("confidence", 0.4)
            )
        }
    }
}

data class TestCaseItem(
    val id: String = "TC-001",
    val name: String = "",
    val status: String = "PASS",
    val isPass: Boolean = true
) {
    companion object {
        fun fromJson(json: JSONObject): TestCaseItem {
            val status = json.optString("status", "PASS")
            val isPass = status.equals("PASS", ignoreCase = true) || status.equals("passed", ignoreCase = true)
            return TestCaseItem(
                id = json.optString("id", "TC-001"),
                name = json.optString("name", "Verification Test"),
                status = status.uppercase(),
                isPass = isPass
            )
        }
    }
}

data class ReportSummary(
    val reportId: String = "CIDAN-20261026105955000000",
    val executiveSummary: String = "Extracted regulatory obligations and compliance mapping results.",
    val operationalImpact: String = "Review required before sandbox sanction.",
    val disclaimer: String = "Synthetic demo only; not official legal advice."
) {
    companion object {
        fun fromJson(json: JSONObject): ReportSummary {
            val narrative = json.optJSONObject("narrative")
            val execSummary = narrative?.optString("executive_summary", "")
                ?: json.optString("executive_summary", "Extracted regulatory obligations.")
            val opImpact = narrative?.optString("operational_impact", "")
                ?: json.optString("operational_impact", "Review required before sandbox sanction.")

            return ReportSummary(
                reportId = json.optString("report_id", "CIDAN-20261026105955000000"),
                executiveSummary = execSummary,
                operationalImpact = opImpact,
                disclaimer = json.optString("disclaimer", "Synthetic demo only; not official legal advice.")
            )
        }
    }
}
