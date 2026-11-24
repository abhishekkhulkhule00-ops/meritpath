package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.localization.AppStrings
import com.example.core.security.openExternalUrlSafe
import com.example.data.model.AppConfig
import com.example.data.model.CounsellingAppointment
import com.example.data.model.Course
import com.example.data.model.PreferenceStatus
import com.example.data.model.StudentPackage
import com.example.data.model.StudentPreferenceList
import com.example.data.model.StudentProfile
import com.example.data.model.SupportTicket
import com.example.domain.CounselingEngine
import com.example.ui.components.StatMetricCard
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SafeGreenBg

private val defaultCapDocuments = listOf(
    "10th Marksheet",
    "12th Marksheet",
    "Entrance Scorecard (CET/NEET/JEE)",
    "Domicile / Nationality Certificate",
    "Caste / EWS Certificate",
    "Non-Creamy Layer (NCL)",
    "Income Certificate",
    "Aadhaar & Passport Photos"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    profile: StudentProfile,
    courses: List<Course>,
    activePackages: List<StudentPackage>,
    preferenceList: StudentPreferenceList?,
    shortlistCount: Int,
    appointments: List<CounsellingAppointment>,
    tickets: List<SupportTicket>,
    appConfig: AppConfig,
    isAdminAuthorized: Boolean,
    strings: AppStrings,
    onNavigateToColleges: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onOpenPackages: () -> Unit,
    onOpenAdminPortal: () -> Unit,
    onSelectCourse: (Course) -> Unit,
    onToggleDocument: (String) -> Unit,
    onRequestAppointment: (String, String, String) -> Unit,
    onSubmitTicket: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val completion = profile.computeCompletionPercent()
    val highestTier = CounselingEngine.highestActiveTier(activePackages)
    val latestPkg = activePackages.firstOrNull { it.isUnexpired() }

    var showAppointmentSheet by remember { mutableStateOf(false) }
    var showSupportSheet by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Banner
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.height(175.dp).fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_campus),
                        contentDescription = "University campus hero banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x660F172A),
                                        Color(0xDD0F172A)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = "AY ${appConfig.academicYear}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (isAdminAuthorized) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiary,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .clickable(onClick = onOpenAdminPortal)
                                        .testTag("open_admin_portal_chip")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.AdminPanelSettings,
                                            contentDescription = "Admin Portal",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Admin",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Column {
                            Text(
                                text = "${strings.welcomePrefix} ${profile.fullName.ifBlank { "Student" }}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = appConfig.announcementBanner,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // Profile Completion & Course Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.profileCompletion,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ID: ${profile.studentCode} • ${profile.category} • ${profile.percentile}%ile",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "$completion%",
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { completion / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    if (completion < 100) {
                        Spacer(Modifier.height(10.dp))
                        TextButton(
                            onClick = onNavigateToProfile,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.testTag("complete_profile_cta")
                        ) {
                            Text(strings.completeProfileCta)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Preferred Course Chips
        item {
            Column {
                Text(
                    text = strings.selectedCourse,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(courses, key = { it.id }) { course ->
                        val selected = profile.preferredCourseId == course.id
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectCourse(course) },
                            label = { Text(course.code.ifBlank { course.name }) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.testTag("course_chip_${course.id}")
                        )
                    }
                }
            }
        }

        // Package Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPackages)
                    .testTag("home_packages_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (latestPkg != null) SafeGreenBg.copy(alpha = 0.65f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (latestPkg != null) Icons.Default.Verified else Icons.Default.WorkspacePremium,
                        contentDescription = strings.activePackage,
                        tint = if (latestPkg != null) SafeGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = latestPkg?.packageName ?: strings.noActivePackage,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (latestPkg != null) {
                                val daysLeft = ((latestPkg.expiresAtEpochMs - System.currentTimeMillis()) / (24L * 3600L * 1000L)).coerceAtLeast(0L)
                                "Verified • ${highestTier?.label ?: ""} • $daysLeft days left"
                            } else {
                                "Unlock Preference Builder, 3-College Compare & 1-on-1 Counselling"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = strings.upgradePackage)
                }
            }
        }

        // KPI Grid
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatMetricCard(
                    title = "Shortlisted",
                    value = shortlistCount.toString(),
                    icon = Icons.Default.AccountBalance,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onNavigateToPreferences)
                )
                StatMetricCard(
                    title = strings.preferenceListStatus,
                    value = preferenceList?.statusEnum()?.name ?: PreferenceStatus.DRAFT.name,
                    icon = Icons.Default.School,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onNavigateToPreferences)
                )
            }
        }

        // Quick Action Row
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onNavigateToColleges,
                    modifier = Modifier.weight(1f).testTag("home_explore_colleges_btn")
                ) {
                    Text("Explore Colleges")
                }
                Button(
                    onClick = onOpenPackages,
                    modifier = Modifier.weight(1f).testTag("home_packages_btn")
                ) {
                    Text(strings.explorePackages)
                }
            }
        }

        // Counselling Appointment Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.bookCounselling, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { showAppointmentSheet = true },
                            modifier = Modifier.testTag("book_counselling_btn")
                        ) {
                            Text("Book Slot")
                        }
                    }
                    val latestAppt = appointments.firstOrNull()
                    if (latestAppt == null) {
                        Text(
                            text = "No counselling appointments requested yet. Book a 1-on-1 option form review session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Latest: ${latestAppt.topic} • ${latestAppt.status} (${latestAppt.scheduledDate.ifBlank { "Pending slot" }})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        if (latestAppt.counsellorName.isNotBlank()) {
                            Text(
                                text = "Counsellor: ${latestAppt.counsellorName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // CAP Document Checklist
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Checklist, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.documentChecklist, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    defaultCapDocuments.forEach { doc ->
                        val checked = profile.documentChecklist.contains(doc)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleDocument(doc) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = doc,
                                tint = if (checked) SafeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(text = doc, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Support, WhatsApp & FAQ
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.supportCenter, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Helpline: ${appConfig.helplinePhone} • Open Tickets: ${tickets.count { it.status != "RESOLVED" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val phone = appConfig.whatsappNumber.replace(Regex("[^0-9]"), "")
                                openExternalUrlSafe(context, "https://wa.me/$phone?text=Hello%20MeritPath%20Counselling")
                            },
                            modifier = Modifier.weight(1f).testTag("whatsapp_support_btn")
                        ) {
                            Text("WhatsApp", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { showFaqDialog = true },
                            modifier = Modifier.weight(1f).testTag("faq_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("FAQ")
                        }
                        Button(
                            onClick = { showSupportSheet = true },
                            modifier = Modifier.weight(1f).testTag("new_ticket_btn")
                        ) {
                            Text("Ticket")
                        }
                    }
                }
            }
        }
    }

    if (showAppointmentSheet) {
        var topic by remember { mutableStateOf("CAP Option Form Strategy") }
        var date by remember { mutableStateOf("2026-11-24") }
        var slot by remember { mutableStateOf("11:00 AM - 11:30 AM") }
        ModalBottomSheet(onDismissRequest = { showAppointmentSheet = false }) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Request Counselling Session", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("Topic") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Preferred Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = slot, onValueChange = { slot = it }, label = { Text("Preferred Time Slot") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        onRequestAppointment(topic, date, slot)
                        showAppointmentSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().testTag("submit_appointment_btn")
                ) {
                    Text("Submit Request")
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showSupportSheet) {
        var subject by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showSupportSheet = false }) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Raise Support Ticket", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Describe your query") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        onSubmitTicket(subject, "Counselling", message)
                        showSupportSheet = false
                    },
                    enabled = subject.isNotBlank() && message.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("submit_ticket_btn")
                ) {
                    Text("Submit Ticket")
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            title = { Text("Admission Counselling FAQ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Who can see my Preference List?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text("Only you and authorized MeritPath Admins/Counsellors. Other students cannot view or query your list.", style = MaterialTheme.typography.bodySmall)
                    Text("2. When do paid features unlock?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text("Only after verified payment completion. Unverified, pending, or failed orders never unlock paid modules.", style = MaterialTheme.typography.bodySmall)
                    Text("3. Are cutoffs guaranteed?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text("Cutoff tables display historical CAP records. Safe/Moderate/Ambitious badges are guidance indicators only.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showFaqDialog = false }) { Text("Got it") }
            }
        )
    }
}
