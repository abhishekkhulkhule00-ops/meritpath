package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.preferences.ThemeMode
import com.example.core.preferences.UserSettings
import com.example.data.model.AppNotification
import com.example.data.model.Course
import com.example.data.model.NotificationType
import com.example.data.model.StudentProfile
import com.example.ui.components.EmptyStateView

@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    readIds: Set<String>,
    onMarkRead: (String) -> Unit
) {
    var selectedType by remember { mutableStateOf("") }
    val filtered = remember(notifications, selectedType) {
        if (selectedType.isBlank()) notifications else notifications.filter { it.type == selectedType }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedType.isBlank(),
                    onClick = { selectedType = "" },
                    label = { Text("All (${notifications.size})") }
                )
            }
            items(NotificationType.entries) { type ->
                FilterChip(
                    selected = selectedType == type.name,
                    onClick = { selectedType = if (selectedType == type.name) "" else type.name },
                    label = { Text(type.label) }
                )
            }
        }

        if (filtered.isEmpty()) {
            EmptyStateView(
                title = "No notifications",
                subtitle = "CAP Round updates, payment confirmations, and counselling reminders appear here.",
                icon = Icons.Default.Notifications,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { notif ->
                    val isRead = readIds.contains(notif.id)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (!isRead) onMarkRead(notif.id) }
                            .testTag("notif_card_${notif.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRead) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = notif.typeEnum().label,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                if (!isRead) {
                                    TextButton(onClick = { onMarkRead(notif.id) }, contentPadding = PaddingValues(0.dp)) {
                                        Icon(Icons.Default.MarkEmailRead, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Mark read")
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(notif.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(notif.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentProfileScreen(
    profile: StudentProfile,
    courses: List<Course>,
    userSettings: UserSettings,
    isAdminAuthorized: Boolean,
    strings: AppStrings,
    onSaveProfile: (StudentProfile) -> Unit,
    onToggleLanguage: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onToggleScreenShield: (Boolean) -> Unit,
    onOpenPackages: () -> Unit,
    onOpenAdminPortal: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit
) {
    var fullName by remember(profile.userId, profile.fullName) { mutableStateOf(profile.fullName) }
    var mobile by remember(profile.userId, profile.mobile) { mutableStateOf(profile.mobile) }
    var dob by remember(profile.userId, profile.dob) { mutableStateOf(profile.dob) }
    var gender by remember(profile.userId, profile.gender) { mutableStateOf(profile.gender) }
    var state by remember(profile.userId, profile.state) { mutableStateOf(profile.state.ifBlank { "Maharashtra" }) }
    var district by remember(profile.userId, profile.district) { mutableStateOf(profile.district) }
    var domicile by remember(profile.userId, profile.domicile) { mutableStateOf(profile.domicile.ifBlank { "Maharashtra" }) }
    var entranceExam by remember(profile.userId, profile.entranceExam) { mutableStateOf(profile.entranceExam.ifBlank { "MHT-CET" }) }
    var examScore by remember(profile.userId, profile.examScore) { mutableStateOf(if (profile.examScore > 0) profile.examScore.toString() else "") }
    var percentile by remember(profile.userId, profile.percentile) { mutableStateOf(if (profile.percentile > 0) profile.percentile.toString() else "") }
    var rank by remember(profile.userId, profile.rank) { mutableStateOf(if (profile.rank > 0) profile.rank.toString() else "") }
    var category by remember(profile.userId, profile.category) { mutableStateOf(profile.category.ifBlank { "OPEN" }) }
    var subcategory by remember(profile.userId, profile.subcategory) { mutableStateOf(profile.subcategory) }
    var budgetMax by remember(profile.userId, profile.budgetMax) { mutableStateOf(if (profile.budgetMax > 0) profile.budgetMax.toString() else "") }
    var preferredLocations by remember(profile.userId, profile.preferredLocations) { mutableStateOf(profile.preferredLocations) }
    var tenthPct by remember(profile.userId, profile.tenthPercent) { mutableStateOf(if (profile.tenthPercent > 0) profile.tenthPercent.toString() else "") }
    var twelfthPct by remember(profile.userId, profile.twelfthPercent) { mutableStateOf(if (profile.twelfthPercent > 0) profile.twelfthPercent.toString() else "") }
    var selectedCourseId by remember(profile.userId, profile.preferredCourseId) { mutableStateOf(profile.preferredCourseId) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(profile.fullName.ifBlank { "Student" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${profile.email} • ID: ${profile.studentCode}", style = MaterialTheme.typography.bodySmall)
                    Text("Profile Completion: ${profile.computeCompletionPercent()}% • Referral: ${profile.referralCode}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (isAdminAuthorized) {
            item {
                Button(
                    onClick = onOpenAdminPortal,
                    modifier = Modifier.fillMaxWidth().testTag("profile_admin_portal_btn")
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.adminPortal)
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onOpenPackages,
                modifier = Modifier.fillMaxWidth().testTag("profile_packages_btn")
            ) {
                Icon(Icons.Default.Receipt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Packages, Payments & Receipts")
            }
        }

        // Settings Card (Language, Theme, Screen Shield)
        item {
            Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("App Preferences & Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Translate, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Language: ${userSettings.language.label}")
                        }
                        OutlinedButton(onClick = onToggleLanguage, modifier = Modifier.testTag("profile_lang_toggle")) {
                            Text(if (userSettings.language == AppLanguage.ENGLISH) "Switch to मराठी" else "Switch to English")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Theme")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = userSettings.themeMode == mode,
                                    onClick = { onSetThemeMode(mode) },
                                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Security, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Screenshot & Recording Shield")
                                Text(
                                    strings.flagSecureDisclaimer,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = userSettings.screenShieldEnabled,
                            onCheckedChange = onToggleScreenShield,
                            modifier = Modifier.testTag("screen_shield_switch")
                        )
                    }
                }
            }
        }

        // Academic & Personal Details Form
        item {
            Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Academic & Personal Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth().testTag("profile_name_input"))
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = dob, onValueChange = { dob = it }, label = { Text("DOB (YYYY-MM-DD)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = gender, onValueChange = { gender = it }, label = { Text("Gender (Optional)") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = state, onValueChange = { state = it }, label = { Text("State") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("District") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = domicile, onValueChange = { domicile = it }, label = { Text("Domicile State") }, modifier = Modifier.fillMaxWidth())

                    Text("Preferred Course", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(courses, key = { it.id }) { c ->
                            FilterChip(
                                selected = selectedCourseId == c.id,
                                onClick = { selectedCourseId = c.id },
                                label = { Text(c.code) }
                            )
                        }
                    }

                    OutlinedTextField(value = entranceExam, onValueChange = { entranceExam = it }, label = { Text("Entrance Exam (MHT-CET / NEET / JEE)") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = examScore, onValueChange = { examScore = it }, label = { Text("Exam Score") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = percentile, onValueChange = { percentile = it }, label = { Text("Percentile") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f).testTag("profile_percentile_input"))
                        OutlinedTextField(value = rank, onValueChange = { rank = it }, label = { Text("Rank") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (OPEN/OBC/SC/ST/EWS)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = subcategory, onValueChange = { subcategory = it }, label = { Text("Subcategory (TFWS/PWD/DEF)") }, modifier = Modifier.weight(1f))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = tenthPct, onValueChange = { tenthPct = it }, label = { Text("10th %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = twelfthPct, onValueChange = { twelfthPct = it }, label = { Text("12th %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = budgetMax, onValueChange = { budgetMax = it }, label = { Text("Max Annual Budget (INR)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = preferredLocations, onValueChange = { preferredLocations = it }, label = { Text("Preferred Cities") }, modifier = Modifier.weight(1f))
                    }

                    Button(
                        onClick = {
                            val chosenCourse = courses.firstOrNull { it.id == selectedCourseId }
                            onSaveProfile(
                                profile.copy(
                                    fullName = fullName.trim().ifBlank { profile.fullName },
                                    mobile = mobile.trim(),
                                    dob = dob.trim(),
                                    gender = gender.trim(),
                                    state = state.trim(),
                                    district = district.trim(),
                                    domicile = domicile.trim(),
                                    preferredCourseId = chosenCourse?.id ?: selectedCourseId,
                                    preferredCourseName = chosenCourse?.name ?: profile.preferredCourseName,
                                    entranceExam = entranceExam.trim(),
                                    examScore = examScore.toDoubleOrNull() ?: 0.0,
                                    percentile = (percentile.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0),
                                    rank = rank.toLongOrNull() ?: 0L,
                                    category = category.trim().uppercase().ifBlank { "OPEN" },
                                    subcategory = subcategory.trim(),
                                    budgetMax = budgetMax.toLongOrNull() ?: 0L,
                                    preferredLocations = preferredLocations.trim(),
                                    tenthPercent = (tenthPct.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0),
                                    twelfthPercent = (twelfthPct.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_profile_btn")
                    ) {
                        Text("Save Profile")
                    }
                }
            }
        }

        item {
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.weight(1f).testTag("sign_out_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.signOut)
                }
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f).testTag("delete_account_btn")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete Profile")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account Profile?") },
            text = { Text("This deletes your student profile document and signs you out. Audit & verified payment records remain immutable for compliance.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete & Sign Out")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}
