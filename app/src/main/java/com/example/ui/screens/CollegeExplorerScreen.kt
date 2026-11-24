package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.localization.AppStrings
import com.example.core.security.SecureScreenEffect
import com.example.core.security.openExternalUrlSafe
import com.example.data.model.College
import com.example.data.model.CollegeType
import com.example.data.model.Course
import com.example.data.model.StudentPackage
import com.example.data.model.StudentProfile
import com.example.domain.CounselingEngine
import com.example.ui.components.EmptyStateView
import com.example.ui.components.GuidanceBadge
import com.example.ui.components.ScreenShieldBanner
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.CollegeFilterState
import com.example.ui.viewmodel.CollegeSortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollegeExplorerScreen(
    colleges: List<College>,
    allColleges: List<College>,
    courses: List<Course>,
    profile: StudentProfile,
    shortlistedIds: Set<String>,
    compareIds: List<String>,
    activePackages: List<StudentPackage>,
    filterState: CollegeFilterState,
    screenShieldEnabled: Boolean,
    strings: AppStrings,
    onUpdateFilter: ((CollegeFilterState) -> CollegeFilterState) -> Unit,
    onToggleShortlist: (College) -> Unit,
    onToggleCompare: (String) -> Unit,
    onClearCompare: () -> Unit,
    onAddToPreference: (College, String, String) -> Unit,
    onOpenPackages: () -> Unit
) {
    var selectedCollege by remember { mutableStateOf<College?>(null) }
    var showCompareSheet by remember { mutableStateOf(false) }

    val hasStandardOrHigher = CounselingEngine.hasStandardOrHigher(activePackages)
    val cities = remember(allColleges) { allColleges.map { it.city }.distinct().sorted() }

    // Apply FLAG_SECURE when viewing paid college details or cutoffs
    SecureScreenEffect(enabled = screenShieldEnabled && (selectedCollege != null || showCompareSheet))

    if (selectedCollege != null) {
        BackHandler { selectedCollege = null }
    }
    if (showCompareSheet) {
        BackHandler { showCompareSheet = false }
    }

    Scaffold(
        floatingActionButton = {
            if (compareIds.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        if (hasStandardOrHigher) showCompareSheet = true else onOpenPackages()
                    },
                    modifier = Modifier.testTag("compare_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = strings.compareSelected)
                        Spacer(Modifier.width(6.dp))
                        Text("${strings.compareSelected} (${compareIds.size})")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search & Filters Header
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                OutlinedTextField(
                    value = filterState.query,
                    onValueChange = { q -> onUpdateFilter { it.copy(query = q) } },
                    placeholder = { Text(strings.searchCollegesHint) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (filterState.query.isNotBlank()) {
                            IconButton(onClick = { onUpdateFilter { it.copy(query = "") } }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("college_search_input")
                )

                Spacer(Modifier.height(8.dp))

                // Course & Type Filter Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filterState.courseId.isBlank(),
                            onClick = { onUpdateFilter { it.copy(courseId = "") } },
                            label = { Text(strings.allCourses) }
                        )
                    }
                    items(courses, key = { it.id }) { c ->
                        FilterChip(
                            selected = filterState.courseId == c.id,
                            onClick = {
                                onUpdateFilter {
                                    it.copy(courseId = if (it.courseId == c.id) "" else c.id)
                                }
                            },
                            label = { Text(c.code) }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = false,
                            onClick = {
                                val nextSort = when (filterState.sortOption) {
                                    CollegeSortOption.NAME -> CollegeSortOption.CUTOFF_DESC
                                    CollegeSortOption.CUTOFF_DESC -> CollegeSortOption.FEE_ASC
                                    CollegeSortOption.FEE_ASC -> CollegeSortOption.PLACEMENT_DESC
                                    CollegeSortOption.PLACEMENT_DESC -> CollegeSortOption.NAME
                                }
                                onUpdateFilter { it.copy(sortOption = nextSort) }
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Sort: ${filterState.sortOption.name}") }
                        )
                    }
                    items(cities) { city ->
                        FilterChip(
                            selected = filterState.city == city,
                            onClick = {
                                onUpdateFilter {
                                    it.copy(city = if (it.city == city) "" else city)
                                }
                            },
                            label = { Text(city) }
                        )
                    }
                    items(CollegeType.entries) { type ->
                        FilterChip(
                            selected = filterState.collegeType == type.name,
                            onClick = {
                                onUpdateFilter {
                                    it.copy(collegeType = if (it.collegeType == type.name) "" else type.name)
                                }
                            },
                            label = { Text(type.label) }
                        )
                    }
                }
            }

            if (colleges.isEmpty()) {
                EmptyStateView(
                    title = "No matching colleges",
                    subtitle = "Try clearing your city, course, or fee filter.",
                    actionLabel = "Reset Filters",
                    onAction = { onUpdateFilter { CollegeFilterState() } },
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(colleges, key = { it.id }) { college ->
                        val topCutoff = college.cutoffs.firstOrNull { it.category == profile.category }
                            ?: college.cutoffs.firstOrNull()
                        val guidance = CounselingEngine.evaluateCutoffGuidance(
                            studentPercentile = profile.percentile,
                            studentRank = profile.rank,
                            cutoff = topCutoff
                        )
                        val isShortlisted = shortlistedIds.contains(college.id)
                        val isCompared = compareIds.contains(college.id)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCollege = college }
                                .testTag("college_card_${college.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Image(
                                        painter = painterResource(
                                            id = if (college.imageKey == "campus") R.drawable.img_hero_campus else R.drawable.img_college_placeholder
                                        ),
                                        contentDescription = college.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = college.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(3.dp))
                                            Text(
                                                text = "${college.city} • Code ${college.code} • ${college.collegeTypeEnum().label}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onToggleShortlist(college) },
                                        modifier = Modifier.testTag("shortlist_btn_${college.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (isShortlisted) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Shortlist",
                                            tint = if (isShortlisted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        GuidanceBadge(tier = guidance, strings = strings)
                                        val feeText = if (college.minAnnualFee() > 0L) "₹${college.minAnnualFee()}/yr" else strings.dataNotAvailable
                                        Text(
                                            text = feeText,
                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    FilterChip(
                                        selected = isCompared,
                                        onClick = { onToggleCompare(college.id) },
                                        label = { Text(if (isCompared) "Compared" else "Compare") },
                                        modifier = Modifier.testTag("compare_chip_${college.id}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // College Detail Sheet
    selectedCollege?.let { col ->
        val context = LocalContext.current
        ModalBottomSheet(onDismissRequest = { selectedCollege = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (screenShieldEnabled) {
                    ScreenShieldBanner(strings = strings)
                }

                Text(col.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "${col.university} • Code: ${col.code} • ${col.city}, ${col.state}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (col.description.isNotBlank()) {
                    Text(col.description, style = MaterialTheme.typography.bodyMedium)
                }

                // Placements & Hostel Summary
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Placements: Avg ₹${col.placementAvgLpa} LPA • High ₹${col.placementHighLpa} LPA • Rate ${col.placementRate}%",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "Hostel: ${if (col.hostelAvailable) col.hostelSummary.ifBlank { "Available" } else "Not Available"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (col.scholarships.isNotBlank()) {
                            Text("Scholarships: ${col.scholarships}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (col.website.isNotBlank()) {
                            TextButton(
                                onClick = { openExternalUrlSafe(context, col.website) },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Official Website: ${col.website}")
                            }
                        }
                    }
                }

                // Branches & Fees + Add to Preference List
                Text("Branches & Fee Structure", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (col.branches.isEmpty()) {
                    Text(strings.dataNotAvailable, style = MaterialTheme.typography.bodySmall)
                } else {
                    col.branches.forEach { branch ->
                        Card(
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(branch.branchName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Choice: ${branch.branchCode} • Intake: ${branch.intake} • Vacant: ${branch.seatsAvailable}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onAddToPreference(col, branch.branchName, branch.branchCode) },
                                        modifier = Modifier.testTag("add_pref_${branch.branchCode}")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add Pref")
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                if (hasStandardOrHigher) {
                                    Text(
                                        text = "Tuition ₹${branch.tuitionFee} + Dev ₹${branch.devFee} + Other ₹${branch.otherFee} + Hostel ₹${branch.hostelFee} = Total ₹${branch.totalFee}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    TextButton(onClick = onOpenPackages, contentPadding = PaddingValues(0.dp)) {
                                        Text("Total Annual Fee: ₹${branch.totalFee} (Unlock Standard for itemized fee breakup)")
                                    }
                                }
                            }
                        }
                    }
                }

                // Cutoffs Table
                Text("Historical CAP Cutoffs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = strings.historicalDisclaimer,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (col.cutoffs.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.dataNotAvailable,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    col.cutoffs.forEach { cut ->
                        val g = CounselingEngine.evaluateCutoffGuidance(profile.percentile, profile.rank, cut)
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${cut.branchName} (${cut.year})", style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        "${cut.capRound} • ${cut.category} • ${cut.quota}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val metricText = buildString {
                                        append(if (cut.percentile != null) "${cut.percentile}%ile " else "${strings.dataNotAvailable} ")
                                        if (cut.score != null) append("• Score ${cut.score} ")
                                        if (cut.rank != null) append("• Rank ${cut.rank}")
                                    }
                                    Text(metricText, style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily))
                                }
                                GuidanceBadge(tier = g, strings = strings)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Side-by-Side Comparison Sheet
    if (showCompareSheet) {
        val selectedList = allColleges.filter { compareIds.contains(it.id) }
        ModalBottomSheet(onDismissRequest = { showCompareSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("College Comparison (${selectedList.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { onClearCompare(); showCompareSheet = false }) { Text("Clear") }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    selectedList.forEach { c ->
                        Card(
                            modifier = Modifier.width(210.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(c.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2)
                                HorizontalDivider()
                                Text("City: ${c.city}", style = MaterialTheme.typography.bodySmall)
                                Text("Type: ${c.collegeTypeEnum().label}", style = MaterialTheme.typography.bodySmall)
                                Text("Min Fee: ₹${c.minAnnualFee()}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily))
                                val bestCutoff = c.cutoffs.firstOrNull()?.percentile?.let { "$it%ile" } ?: strings.dataNotAvailable
                                Text("Cutoff: $bestCutoff", style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily))
                                Text("Placement: ₹${c.placementAvgLpa} LPA (${c.placementRate}%)", style = MaterialTheme.typography.bodySmall)
                                Text("Hostel: ${if (c.hostelAvailable) "Yes" else "No"}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
