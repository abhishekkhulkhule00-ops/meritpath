package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.security.shareTextDocument
import com.example.data.model.AdminRole
import com.example.data.model.AdminUser
import com.example.data.model.AppConfig
import com.example.data.model.AuditLog
import com.example.data.model.PaymentMode
import com.example.ui.components.StatMetricCard
import com.example.ui.theme.AmbitiousRose
import com.example.ui.theme.MeritBluePrimary
import com.example.ui.theme.MeritTealAccent
import com.example.ui.theme.ModerateAmber
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SafeGreen
import com.example.ui.viewmodel.AdminDashboardMetrics

@Composable
fun AdminDashboardTab(metrics: AdminDashboardMetrics, onSeedCatalog: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatMetricCard("Students", metrics.totalStudents.toString(), Icons.Default.People, modifier = Modifier.weight(1f))
            StatMetricCard("Revenue", "₹${metrics.totalRevenueInr}", Icons.Default.AttachMoney, SafeGreen, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatMetricCard("Submitted Lists", metrics.submittedPreferences.toString(), Icons.Default.Checklist, MeritBluePrimary, modifier = Modifier.weight(1f))
            StatMetricCard("Pending Counselling", metrics.pendingCounselling.toString(), Icons.Default.Schedule, ModerateAmber, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatMetricCard("Colleges", metrics.totalColleges.toString(), Icons.Default.AccountBalance, MeritTealAccent, modifier = Modifier.weight(1f))
            StatMetricCard("Courses / Pkgs", "${metrics.totalCourses}/${metrics.totalPackages}", Icons.Default.School, modifier = Modifier.weight(1f))
        }

        // Analytics Canvas Chart
        Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Transactions & Pipeline Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                val bars = listOf(
                    Triple("Paid", metrics.successfulPayments.toFloat(), SafeGreen),
                    Triple("Pending", metrics.pendingPayments.toFloat(), ModerateAmber),
                    Triple("Failed", metrics.failedPayments.toFloat(), AmbitiousRose),
                    Triple("Prefs", metrics.submittedPreferences.toFloat(), MeritBluePrimary),
                    Triple("Blocked", metrics.blockedStudents.toFloat(), Color.Gray)
                )
                val maxVal = (bars.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(1f)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val slotWidth = size.width / bars.size
                    val barW = slotWidth * 0.45f
                    bars.forEachIndexed { idx, (_, value, color) ->
                        val ratio = (value / maxVal).coerceIn(0.05f, 1f)
                        val barH = size.height * ratio
                        val left = idx * slotWidth + (slotWidth - barW) / 2f
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(left, size.height - barH),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    bars.forEach { (label, valF, _) ->
                        Text("$label (${valF.toInt()})", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Button(
            onClick = onSeedCatalog,
            modifier = Modifier.fillMaxWidth().testTag("admin_seed_catalog_btn")
        ) {
            Text("Publish / Refresh Starter Catalog in Firestore")
        }
    }
}

@Composable
fun AdminNotificationsTab(
    onSendNotification: (String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("CAP_ROUND") }
    var audience by remember { mutableStateOf("ALL") }
    var targetUid by remember { mutableStateOf("ALL") }

    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Dispatch Admission Notification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message") }, minLines = 3, modifier = Modifier.fillMaxWidth())

            Text("Type", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("CAP_ROUND", "CUTOFF", "DEADLINE", "GENERAL").forEach { t ->
                    FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                }
            }

            Text("Target Audience", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ALL", "PAID", "UNPAID", "STUDENT").forEach { a ->
                    FilterChip(
                        selected = audience == a,
                        onClick = {
                            audience = a
                            if (a != "STUDENT") targetUid = "ALL"
                        },
                        label = { Text(a) }
                    )
                }
            }
            if (audience == "STUDENT") {
                OutlinedTextField(
                    value = targetUid,
                    onValueChange = { targetUid = it },
                    label = { Text("Student Firebase UID") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(
                onClick = {
                    onSendNotification(title, message, type, audience, if (audience == "STUDENT") targetUid else "ALL")
                    title = ""
                    message = ""
                },
                enabled = title.isNotBlank() && message.isNotBlank(),
                modifier = Modifier.fillMaxWidth().testTag("admin_send_notif_btn")
            ) {
                Text("Send Notification")
            }
        }
    }
}

@Composable
fun AdminReportsTab(buildCsv: (String) -> String) {
    val context = LocalContext.current
    val reports = listOf(
        "STUDENTS" to "Student Directory & Profiles Report (CSV)",
        "PAYMENTS" to "Payments, Discounts & Revenue Report (CSV)",
        "PREFERENCES" to "Submitted Student Preference Lists Report (CSV)",
        "COLLEGES" to "Colleges & Branches Master Report (CSV)"
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        reports.forEach { (key, title) ->
            Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { shareTextDocument(context, "MeritPath $key Report", buildCsv(key)) },
                        modifier = Modifier.testTag("export_report_$key")
                    ) {
                        Text("Export CSV")
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSettingsAndRbacTab(
    appConfig: AppConfig,
    staffUsers: List<AdminUser>,
    auditLogs: List<AuditLog>,
    onSaveConfig: (AppConfig) -> Unit,
    onSaveStaff: (String, String, String, AdminRole, Boolean) -> Unit
) {
    var whatsapp by remember(appConfig) { mutableStateOf(appConfig.whatsappNumber) }
    var helpline by remember(appConfig) { mutableStateOf(appConfig.helplinePhone) }
    var year by remember(appConfig) { mutableStateOf(appConfig.academicYear) }
    var banner by remember(appConfig) { mutableStateOf(appConfig.announcementBanner) }
    var backendUrl by remember(appConfig) { mutableStateOf(appConfig.backendUrl) }
    var payMode by remember(appConfig) { mutableStateOf(appConfig.paymentMode) }

    var staffUid by remember { mutableStateOf("") }
    var staffEmail by remember { mutableStateOf("") }
    var staffName by remember { mutableStateOf("") }
    var staffRole by remember { mutableStateOf(AdminRole.COUNSELLOR) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Platform & Payment Gateway Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("WhatsApp Support Number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = helpline, onValueChange = { helpline = it }, label = { Text("Helpline Phone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Academic Year") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = banner, onValueChange = { banner = it }, label = { Text("Home Announcement Banner") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = backendUrl, onValueChange = { backendUrl = it }, label = { Text("Razorpay Verification Backend URL (Optional)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMode.entries.forEach { m ->
                        FilterChip(selected = payMode == m.name, onClick = { payMode = m.name }, label = { Text(m.name) })
                    }
                }
                Button(
                    onClick = {
                        onSaveConfig(
                            appConfig.copy(
                                whatsappNumber = whatsapp,
                                helplinePhone = helpline,
                                academicYear = year,
                                announcementBanner = banner,
                                backendUrl = backendUrl,
                                paymentMode = payMode
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Configuration")
                }
            }
        }

        Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("RBAC Staff & Counsellor Roles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = staffUid, onValueChange = { staffUid = it }, label = { Text("Firebase User UID") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = staffEmail, onValueChange = { staffEmail = it }, label = { Text("Staff Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = staffName, onValueChange = { staffName = it }, label = { Text("Display Name") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AdminRole.entries.forEach { r ->
                        FilterChip(selected = staffRole == r, onClick = { staffRole = r }, label = { Text(r.name) })
                    }
                }
                Button(
                    onClick = {
                        onSaveStaff(staffUid, staffEmail, staffName, staffRole, true)
                        staffUid = ""; staffEmail = ""; staffName = ""
                    },
                    enabled = staffUid.isNotBlank() && staffEmail.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Role")
                }
                if (staffUsers.isNotEmpty()) {
                    HorizontalDivider()
                    staffUsers.forEach { u ->
                        Text("${u.displayName} (${u.email}) • ${u.role}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Immutable Security & Payment Audit Logs (${auditLogs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                auditLogs.take(15).forEach { log ->
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            Text("${log.action} • ${log.entityType}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily), fontWeight = FontWeight.Bold)
                            Text(log.summary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
