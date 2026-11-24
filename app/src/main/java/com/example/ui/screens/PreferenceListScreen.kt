package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.localization.AppStrings
import com.example.core.security.SecureScreenEffect
import com.example.data.model.College
import com.example.data.model.PreferenceItem
import com.example.data.model.PreferenceStatus
import com.example.data.model.ShortlistItem
import com.example.data.model.StudentPackage
import com.example.data.model.StudentPreferenceList
import com.example.domain.CounselingEngine
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ScreenShieldBanner
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SafeGreenBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferenceListScreen(
    shortlist: List<ShortlistItem>,
    allColleges: List<College>,
    preferenceItems: List<PreferenceItem>,
    serverPreferenceList: StudentPreferenceList?,
    activePackages: List<StudentPackage>,
    screenShieldEnabled: Boolean,
    strings: AppStrings,
    onUpdateShortlistNote: (String, String) -> Unit,
    onRemoveShortlist: (College) -> Unit,
    onAddPreference: (College, String, String) -> Unit,
    onMovePreference: (Int, Int) -> Unit,
    onRemovePreference: (Int) -> Unit,
    onSaveDraft: () -> Unit,
    onSubmitFinal: () -> Unit,
    onOpenPackages: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) }
    var editingNoteItem by remember { mutableStateOf<ShortlistItem?>(null) }
    var showAddBranchPicker by remember { mutableStateOf(false) }
    var showSubmitConfirm by remember { mutableStateOf(false) }

    val serverStatus = serverPreferenceList?.statusEnum() ?: PreferenceStatus.DRAFT
    val isLocked = serverStatus != PreferenceStatus.DRAFT
    val hasStandardOrHigher = CounselingEngine.hasStandardOrHigher(activePackages)

    SecureScreenEffect(enabled = screenShieldEnabled)

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("${strings.shortlistTab} (${shortlist.size})") },
                modifier = Modifier.testTag("tab_shortlist")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("${strings.preferenceTab} (${preferenceItems.size})") },
                modifier = Modifier.testTag("tab_preferences")
            )
        }

        if (selectedTab == 0) {
            if (shortlist.isEmpty()) {
                EmptyStateView(
                    title = "No shortlisted colleges",
                    subtitle = "Bookmark colleges in the Explorer to track notes and compare them.",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(shortlist, key = { it.collegeId }) { item ->
                        val collegeObj = allColleges.firstOrNull { it.id == item.collegeId }
                        Card(
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.collegeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(item.city, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { editingNoteItem = item }) {
                                        Icon(Icons.Default.EditNote, contentDescription = "Edit note")
                                    }
                                    if (collegeObj != null) {
                                        IconButton(onClick = { onRemoveShortlist(collegeObj) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                                if (item.note.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = "Note: ${item.note}",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (screenShieldEnabled) {
                    item { ScreenShieldBanner(strings = strings) }
                }

                // Privacy & Status Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLocked) SafeGreenBg.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isLocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (isLocked) SafeGreen else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Status: ${serverStatus.name}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (!isLocked) {
                                    OutlinedButton(
                                        onClick = { showAddBranchPicker = true },
                                        modifier = Modifier.testTag("add_pref_choice_btn")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add Choice")
                                    }
                                }
                            }
                            Text(
                                text = strings.listPrivacyNotice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val notes = serverPreferenceList?.adminNotes.orEmpty()
                            if (notes.isNotBlank()) {
                                Text(
                                    text = "Counsellor Note: $notes",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (!hasStandardOrHigher) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f))) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Unlock Official Preference Submission", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        "You can draft choices freely. Activate Standard or Premium package to submit & lock your preference list for counsellor audit.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = onOpenPackages, modifier = Modifier.testTag("unlock_pref_pkg_btn")) {
                                    Text("Upgrade")
                                }
                            }
                        }
                    }
                }

                if (preferenceItems.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "Preference list is empty",
                            subtitle = "Add college branches from Explorer or tap 'Add Choice' above to build Priority #1, #2, #3...",
                            actionLabel = if (!isLocked) "Add Choice" else null,
                            onAction = if (!isLocked) ({ showAddBranchPicker = true }) else null
                        )
                    }
                } else {
                    itemsIndexed(preferenceItems, key = { idx, item -> "${item.collegeId}_${item.branchName}_$idx" }) { index, item ->
                        Card(
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.collegeName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(item.branchName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        "${item.city} • Choice ${item.choiceCode}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!isLocked) {
                                    IconButton(
                                        onClick = { onMovePreference(index, -1) },
                                        enabled = index > 0,
                                        modifier = Modifier.testTag("pref_up_$index")
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move up")
                                    }
                                    IconButton(
                                        onClick = { onMovePreference(index, +1) },
                                        enabled = index < preferenceItems.lastIndex,
                                        modifier = Modifier.testTag("pref_down_$index")
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move down")
                                    }
                                    IconButton(
                                        onClick = { onRemovePreference(index) },
                                        modifier = Modifier.testTag("pref_del_$index")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    if (!isLocked) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onSaveDraft,
                                    modifier = Modifier.weight(1f).testTag("save_pref_draft_btn")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.saveDraft)
                                }
                                Button(
                                    onClick = {
                                        if (hasStandardOrHigher) showSubmitConfirm = true else onOpenPackages()
                                    },
                                    modifier = Modifier.weight(1f).testTag("submit_pref_final_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.submitFinalList)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingNoteItem?.let { item ->
        var noteText by remember(item.collegeId) { mutableStateOf(item.note) }
        AlertDialog(
            onDismissRequest = { editingNoteItem = null },
            title = { Text("Note for ${item.collegeName}") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Personal note") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { onUpdateShortlistNote(item.collegeId, noteText); editingNoteItem = null }) {
                    Text("Save")
                }
            },
            dismissButton = { TextButton(onClick = { editingNoteItem = null }) { Text("Cancel") } }
        )
    }

    if (showAddBranchPicker) {
        ModalBottomSheet(onDismissRequest = { showAddBranchPicker = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select College & Branch", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allColleges, key = { it.id }) { col ->
                        Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(col.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                col.branches.forEach { b ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${b.branchName} (${b.branchCode})", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                        TextButton(
                                            onClick = {
                                                onAddPreference(col, b.branchName, b.branchCode)
                                                showAddBranchPicker = false
                                            }
                                        ) {
                                            Text("Add")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSubmitConfirm) {
        AlertDialog(
            onDismissRequest = { showSubmitConfirm = false },
            title = { Text("Submit Final Preference List?") },
            text = {
                Text("Submitting locks your ${preferenceItems.size} ordered preferences and routes them privately to authorized MeritPath counsellors for review.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitFinal()
                        showSubmitConfirm = false
                    },
                    modifier = Modifier.testTag("confirm_submit_pref_btn")
                ) {
                    Text("Confirm & Submit")
                }
            },
            dismissButton = { TextButton(onClick = { showSubmitConfirm = false }) { Text("Cancel") } }
        )
    }
}
