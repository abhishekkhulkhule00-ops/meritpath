package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.security.SecureScreenEffect
import com.example.data.model.AppConfig
import com.example.data.model.College
import com.example.data.model.CollegeBranch
import com.example.data.model.CollegeType
import com.example.data.model.CounsellingPackage
import com.example.data.model.CounsellingStatus
import com.example.data.model.Course
import com.example.data.model.DiscountType
import com.example.data.model.PackageTier
import com.example.data.model.PaymentStatus
import com.example.data.model.StudentPreferenceList
import com.example.data.model.StudentProfile
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SafeGreenBg
import com.example.ui.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

enum class AdminSection(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    STUDENTS("Students", Icons.Default.People),
    COURSES("Courses", Icons.Default.School),
    COLLEGES("Colleges", Icons.Default.AccountBalance),
    PACKAGES("Packages", Icons.Default.Inventory2),
    COMBO_DISCOUNTS("Combo & Discounts", Icons.Default.LocalOffer),
    PAYMENTS("Payments", Icons.Default.Payments),
    PREFERENCES("Preferences", Icons.Default.Checklist),
    COUNSELLING("Counselling", Icons.Default.CalendarMonth),
    NOTIFICATIONS("Notifications", Icons.Default.Notifications),
    REPORTS("Reports", Icons.Default.Assessment),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalScreen(
    adminViewModel: AdminViewModel,
    appConfig: AppConfig,
    screenShieldEnabled: Boolean,
    onExitAdmin: () -> Unit
) {
    BackHandler(onBack = onExitAdmin)
    SecureScreenEffect(enabled = screenShieldEnabled)

    val currentAdmin by adminViewModel.currentAdmin.collectAsStateWithLifecycle()
    val metrics by adminViewModel.metrics.collectAsStateWithLifecycle()
    val students by adminViewModel.students.collectAsStateWithLifecycle()
    val courses by adminViewModel.courses.collectAsStateWithLifecycle()
    val colleges by adminViewModel.colleges.collectAsStateWithLifecycle()
    val packages by adminViewModel.packages.collectAsStateWithLifecycle()
    val promoCodes by adminViewModel.promoCodes.collectAsStateWithLifecycle()
    val payments by adminViewModel.payments.collectAsStateWithLifecycle()
    val preferences by adminViewModel.preferences.collectAsStateWithLifecycle()
    val appointments by adminViewModel.appointments.collectAsStateWithLifecycle()
    val auditLogs by adminViewModel.auditLogs.collectAsStateWithLifecycle()
    val staffUsers by adminViewModel.staffUsers.collectAsStateWithLifecycle()
    val banner by adminViewModel.statusBanner.collectAsStateWithLifecycle()

    var section by remember { mutableStateOf(AdminSection.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(banner) {
        if (banner != null) {
            snackbarHostState.showSnackbar(banner!!)
            adminViewModel.clearBanner()
        }
    }

    val drawerMenuContent: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("MeritPath Admin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = "${currentAdmin?.displayName ?: "Staff"} • ${currentAdmin?.role ?: "VERIFIED"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            AdminSection.entries.forEach { item ->
                NavigationDrawerItem(
                    selected = section == item,
                    onClick = {
                        section = item
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(item.icon, contentDescription = item.title) },
                    label = { Text(item.title) },
                    modifier = Modifier.testTag("admin_nav_${item.name}")
                )
            }
        }
    }

    val mainBody: @Composable (Boolean) -> Unit = { isWide ->
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Admin • ${section.title}") },
                    navigationIcon = {
                        if (!isWide) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Open Admin Menu")
                            }
                        }
                    },
                    actions = {
                        TextButton(onClick = onExitAdmin, modifier = Modifier.testTag("exit_admin_btn")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Student View")
                        }
                    }
                )
            }
        ) { innerPadding ->
            AdminSectionContent(
                section = section,
                modifier = Modifier.padding(innerPadding),
                adminViewModel = adminViewModel,
                metrics = metrics,
                students = students,
                courses = courses,
                colleges = colleges,
                packages = packages,
                promoCodes = promoCodes,
                payments = payments,
                preferences = preferences,
                appointments = appointments,
                auditLogs = auditLogs,
                staffUsers = staffUsers,
                appConfig = appConfig
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 840.dp
        if (isWide) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(modifier = Modifier.width(260.dp)) {
                        LazyColumn { item { drawerMenuContent() } }
                    }
                },
                content = { mainBody(true) }
            )
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                        LazyColumn { item { drawerMenuContent() } }
                    }
                },
                content = { mainBody(false) }
            )
        }
    }
}

@Composable
private fun AdminSectionContent(
    section: AdminSection,
    modifier: Modifier,
    adminViewModel: AdminViewModel,
    metrics: com.example.ui.viewmodel.AdminDashboardMetrics,
    students: List<StudentProfile>,
    courses: List<Course>,
    colleges: List<College>,
    packages: List<CounsellingPackage>,
    promoCodes: List<com.example.data.model.PromoCode>,
    payments: List<com.example.data.model.PaymentRecord>,
    preferences: List<StudentPreferenceList>,
    appointments: List<com.example.data.model.CounsellingAppointment>,
    auditLogs: List<com.example.data.model.AuditLog>,
    staffUsers: List<com.example.data.model.AdminUser>,
    appConfig: AppConfig
) {
    var search by remember(section) { mutableStateOf("") }
    var editingStudent by remember { mutableStateOf<StudentProfile?>(null) }
    var reviewingPref by remember { mutableStateOf<StudentPreferenceList?>(null) }
    var showAddCourse by remember { mutableStateOf(false) }
    var showAddCollege by remember { mutableStateOf(false) }
    var showBulkCsv by remember { mutableStateOf(false) }
    var showAddPackage by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (section) {
            AdminSection.DASHBOARD -> {
                item { AdminDashboardTab(metrics = metrics, onSeedCatalog = { adminViewModel.seedStarterCatalog() }) }
            }

            AdminSection.STUDENTS -> {
                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text("Search student name, email, code...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                val filtered = students.filter {
                    search.isBlank() || it.fullName.contains(search, true) || it.email.contains(search, true) || it.studentCode.contains(search, true)
                }
                items(filtered, key = { it.userId }) { st ->
                    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${st.fullName} (${st.studentCode})", fontWeight = FontWeight.Bold)
                                Text("${st.email} • ${st.preferredCourseName} • ${st.percentile}%ile", style = MaterialTheme.typography.bodySmall)
                                Text("Status: ${st.counsellingStatus} • Blocked: ${st.blocked}", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(onClick = { editingStudent = st }) { Text("Manage") }
                        }
                    }
                }
            }

            AdminSection.COURSES -> {
                item {
                    Button(onClick = { showAddCourse = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Add New Course")
                    }
                }
                items(courses, key = { it.id }) { c ->
                    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${c.name} (${c.code})", fontWeight = FontWeight.Bold)
                                Text("${c.duration} • ${c.entranceExam}", style = MaterialTheme.typography.bodySmall)
                            }
                            FilterChip(
                                selected = c.active,
                                onClick = { adminViewModel.saveCourse(c.copy(active = !c.active)) },
                                label = { Text(if (c.active) "Active" else "Archived") }
                            )
                        }
                    }
                }
            }

            AdminSection.COLLEGES -> {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showAddCollege = true }, modifier = Modifier.weight(1f)) { Text("Add College") }
                        OutlinedButton(onClick = { showBulkCsv = true }, modifier = Modifier.weight(1f)) { Text("CSV Bulk Import") }
                    }
                }
                items(colleges, key = { it.id }) { col ->
                    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${col.name} (${col.code})", fontWeight = FontWeight.Bold)
                                Text("${col.city} • ${col.collegeType} • ${col.branches.size} branches", style = MaterialTheme.typography.bodySmall)
                            }
                            FilterChip(
                                selected = col.active,
                                onClick = { adminViewModel.saveCollege(col.copy(active = !col.active)) },
                                label = { Text(if (col.active) "Active" else "Archived") }
                            )
                        }
                    }
                }
            }

            AdminSection.PACKAGES -> {
                item {
                    Button(onClick = { showAddPackage = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Create Counselling Package")
                    }
                }
                items(packages, key = { it.id }) { pkg ->
                    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${pkg.name} [${pkg.tier}]", fontWeight = FontWeight.Bold)
                                Text("Orig ₹${pkg.originalPrice} -> Final ₹${pkg.finalPrice} • ${pkg.validityDays}d", style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily))
                            }
                            FilterChip(
                                selected = pkg.active,
                                onClick = { adminViewModel.savePackage(pkg.copy(active = !pkg.active)) },
                                label = { Text(if (pkg.active) "Active" else "Inactive") }
                            )
                        }
                    }
                }
            }

            AdminSection.COMBO_DISCOUNTS -> {
                item {
                    var promoCode by remember { mutableStateOf("") }
                    var promoVal by remember { mutableStateOf("250") }
                    var promoType by remember { mutableStateOf(DiscountType.FIXED) }
                    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Create Promo Code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = promoCode, onValueChange = { promoCode = it }, label = { Text("Promo Code (e.g. CAP2026)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = promoVal, onValueChange = { promoVal = it }, label = { Text("Discount Value (INR or %)") }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = promoType == DiscountType.FIXED, onClick = { promoType = DiscountType.FIXED }, label = { Text("Fixed ₹") })
                                FilterChip(selected = promoType == DiscountType.PERCENT, onClick = { promoType = DiscountType.PERCENT }, label = { Text("Percent %") })
                            }
                            Button(
                                onClick = {
                                    adminViewModel.savePromoCode(promoCode, promoType, promoVal.toLongOrNull() ?: 100L, 499L, 500L)
                                    promoCode = ""
                                },
                                enabled = promoCode.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Save Promo Code") }
                        }
                    }
                }
                items(promoCodes, key = { it.id }) { p ->
                    Card {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${p.code} • ${p.discountType} ${p.discountValue}", fontWeight = FontWeight.Bold)
                            Text("Used ${p.usedCount}/${p.maxUsage}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            AdminSection.PAYMENTS -> {
                items(payments, key = { it.id }) { pay ->
                    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${pay.studentName} • ₹${pay.finalAmount}", fontWeight = FontWeight.Bold)
                                Text("${pay.packageName} • ${pay.status} • ${pay.receiptNumber.ifBlank { pay.gatewayOrderId }}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (pay.statusEnum() == PaymentStatus.SUCCESS) {
                                OutlinedButton(onClick = { adminViewModel.refundPayment(pay.id, "Admin initiated refund") }) {
                                    Text("Refund")
                                }
                            }
                        }
                    }
                }
            }

            AdminSection.PREFERENCES -> {
                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text("Filter by student, course, or category...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                val filteredPrefs = preferences.filter {
                    search.isBlank() || it.studentName.contains(search, true) || it.courseName.contains(search, true) || it.category.contains(search, true)
                }
                items(filteredPrefs, key = { it.userId }) { pref ->
                    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${pref.studentName} (${pref.studentCode})", fontWeight = FontWeight.Bold)
                                Surface(color = SafeGreenBg, shape = RoundedCornerShape(6.dp)) {
                                    Text(pref.status, color = SafeGreen, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Text(
                                "${pref.courseName} • ${pref.percentile}%ile • Rank ${pref.rank} • ${pref.category} • Pkg: ${pref.packageTier}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text("Choices (${pref.items.size}): " + pref.items.take(3).joinToString { "#${it.order} ${it.collegeName}" }, style = MaterialTheme.typography.labelSmall)
                            OutlinedButton(onClick = { reviewingPref = pref }) { Text("View Full List & Assign Counsellor") }
                        }
                    }
                }
            }

            AdminSection.COUNSELLING -> {
                items(appointments, key = { it.id }) { appt ->
                    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${appt.studentName} • ${appt.topic}", fontWeight = FontWeight.Bold)
                            Text("Date: ${appt.scheduledDate} ${appt.scheduledSlot} • Status: ${appt.status}", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(CounsellingStatus.SCHEDULED, CounsellingStatus.IN_COUNSELLING, CounsellingStatus.COMPLETED).forEach { st ->
                                    FilterChip(
                                        selected = appt.statusEnum() == st,
                                        onClick = { adminViewModel.updateAppointment(appt.id, st, "Senior Counsellor", appt.scheduledDate, "Updated via Admin") },
                                        label = { Text(st.name) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AdminSection.NOTIFICATIONS -> {
                item {
                    AdminNotificationsTab { title, msg, type, aud, uid ->
                        adminViewModel.sendNotification(title, msg, type, aud, uid)
                    }
                }
            }

            AdminSection.REPORTS -> {
                item { AdminReportsTab(buildCsv = { type -> adminViewModel.buildCsvReport(type) }) }
            }

            AdminSection.SETTINGS -> {
                item {
                    AdminSettingsAndRbacTab(
                        appConfig = appConfig,
                        staffUsers = staffUsers,
                        auditLogs = auditLogs,
                        onSaveConfig = { adminViewModel.saveAppConfig(it) },
                        onSaveStaff = { uid, email, name, role, active -> adminViewModel.saveStaffRole(uid, email, name, role, active) }
                    )
                }
            }
        }
    }

    editingStudent?.let { st ->
        var blocked by remember(st.userId) { mutableStateOf(st.blocked) }
        var status by remember(st.userId) { mutableStateOf(CounsellingStatus.safeValueOf(st.counsellingStatus)) }
        var counsellor by remember(st.userId) { mutableStateOf(st.assignedCounsellorName) }
        var notes by remember(st.userId) { mutableStateOf(st.internalNotes) }
        AlertDialog(
            onDismissRequest = { editingStudent = null },
            title = { Text("Manage ${st.fullName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = counsellor, onValueChange = { counsellor = it }, label = { Text("Assigned Counsellor") })
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Internal Notes") })
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = blocked, onClick = { blocked = !blocked }, label = { Text(if (blocked) "Blocked" else "Active") })
                        FilterChip(selected = status == CounsellingStatus.COMPLETED, onClick = { status = CounsellingStatus.COMPLETED }, label = { Text("Completed") })
                    }
                }
            },
            confirmButton = {
                Button(onClick = { adminViewModel.updateStudent(st.userId, blocked, status, counsellor, notes); editingStudent = null }) {
                    Text("Save")
                }
            },
            dismissButton = { TextButton(onClick = { editingStudent = null }) { Text("Cancel") } }
        )
    }

    reviewingPref?.let { pref ->
        var status by remember(pref.userId) { mutableStateOf(pref.status) }
        var counsellor by remember(pref.userId) { mutableStateOf(pref.assignedCounsellorName) }
        var notes by remember(pref.userId) { mutableStateOf(pref.adminNotes) }
        AlertDialog(
            onDismissRequest = { reviewingPref = null },
            title = { Text("${pref.studentName} Preferences") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    pref.items.forEach { item ->
                        Text("#${item.order} ${item.collegeName} - ${item.branchName}", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(value = counsellor, onValueChange = { counsellor = it }, label = { Text("Counsellor") })
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Counsellor Review Notes") })
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("DRAFT", "REVIEWED", "LOCKED").forEach { s ->
                            FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { adminViewModel.reviewPreferenceList(pref.userId, status, "IN_COUNSELLING", counsellor, notes); reviewingPref = null }) {
                    Text("Save Review")
                }
            },
            dismissButton = { TextButton(onClick = { reviewingPref = null }) { Text("Close") } }
        )
    }

    if (showAddCourse) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var duration by remember { mutableStateOf("4 Years") }
        var exam by remember { mutableStateOf("MHT-CET") }
        AlertDialog(
            onDismissRequest = { showAddCourse = false },
            title = { Text("New Course") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Course Name") })
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code (e.g. BTECH)") })
                    OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration") })
                    OutlinedTextField(value = exam, onValueChange = { exam = it }, label = { Text("Entrance Exam") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    adminViewModel.saveCourse(Course(name = name, code = code, description = name, eligibility = "10+2", duration = duration, entranceExam = exam))
                    showAddCourse = false
                }, enabled = name.isNotBlank() && code.isNotBlank()) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddCourse = false }) { Text("Cancel") } }
        )
    }

    if (showAddCollege) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var city by remember { mutableStateOf("Pune") }
        var fee by remember { mutableStateOf("110000") }
        AlertDialog(
            onDismissRequest = { showAddCollege = false },
            title = { Text("Add College") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("College Name") })
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("DTE/College Code") })
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") })
                    OutlinedTextField(value = fee, onValueChange = { fee = it }, label = { Text("Annual Fee (INR)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val f = fee.toLongOrNull() ?: 100000L
                    adminViewModel.saveCollege(
                        College(
                            name = name, code = code, university = "State University", city = city, district = city, state = "Maharashtra",
                            collegeType = CollegeType.PRIVATE.name, courseIds = listOf("course_btech"),
                            branches = listOf(CollegeBranch("${code}10", "Computer Engineering", "course_btech", 60, 12, f, 5000, 2000, 0, f + 7000))
                        )
                    )
                    showAddCollege = false
                }, enabled = name.isNotBlank() && code.isNotBlank()) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddCollege = false }) { Text("Cancel") } }
        )
    }

    if (showBulkCsv) {
        var csv by remember { mutableStateOf("code,name,city,type,fee,branch,courseId\n6175,Pimpri Chinchwad College of Engineering,Pune,PRIVATE,132000,Computer Engineering,course_btech") }
        AlertDialog(
            onDismissRequest = { showBulkCsv = false },
            title = { Text("Bulk CSV Import") },
            text = {
                OutlinedTextField(value = csv, onValueChange = { csv = it }, minLines = 5, label = { Text("CSV Rows") })
            },
            confirmButton = {
                Button(onClick = { adminViewModel.importCollegesCsv(csv); showBulkCsv = false }) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { showBulkCsv = false }) { Text("Cancel") } }
        )
    }

    if (showAddPackage) {
        var name by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("1999") }
        var discount by remember { mutableStateOf("300") }
        AlertDialog(
            onDismissRequest = { showAddPackage = false },
            title = { Text("New Package") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Package Name") })
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Original Price (INR)") })
                    OutlinedTextField(value = discount, onValueChange = { discount = it }, label = { Text("Fixed Discount (INR)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val orig = price.toLongOrNull() ?: 999L
                    val disc = (discount.toLongOrNull() ?: 0L).coerceIn(0L, orig)
                    adminViewModel.savePackage(
                        CounsellingPackage(
                            name = name, description = "Custom counselling package", tier = PackageTier.STANDARD.name,
                            courseIds = listOf("course_btech"), originalPrice = orig, discountType = DiscountType.FIXED.name,
                            discountValue = disc, finalPrice = orig - disc,
                            features = listOf("College Explorer", "Cutoff Analysis", "Preference List"), validityDays = 180L
                        )
                    )
                    showAddPackage = false
                }, enabled = name.isNotBlank()) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddPackage = false }) { Text("Cancel") } }
        )
    }
}
