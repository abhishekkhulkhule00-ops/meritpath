package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.preferences.ThemeMode
import com.example.core.preferences.UserSettings
import com.example.data.model.StudentProfile
import com.example.ui.components.ErrorStateBanner
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.StudentViewModel
import com.example.ui.viewmodel.UiState

enum class StudentTab(val icon: ImageVector) {
    HOME(Icons.Default.Home),
    COLLEGES(Icons.Default.AccountBalance),
    MY_LIST(Icons.Default.Checklist),
    ALERTS(Icons.Default.Notifications),
    PROFILE(Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentShell(
    studentViewModel: StudentViewModel,
    adminViewModel: AdminViewModel,
    userSettings: UserSettings,
    strings: AppStrings,
    onToggleLanguage: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onToggleScreenShield: (Boolean) -> Unit,
    onSignOut: () -> Unit
) {
    val profileUi by studentViewModel.profileState.collectAsStateWithLifecycle()
    val coursesUi by studentViewModel.coursesState.collectAsStateWithLifecycle()
    val filteredColleges by studentViewModel.filteredColleges.collectAsStateWithLifecycle()
    val allColleges by studentViewModel.allColleges.collectAsStateWithLifecycle()
    val packagesUi by studentViewModel.packagesState.collectAsStateWithLifecycle()
    val shortlist by studentViewModel.shortlistState.collectAsStateWithLifecycle()
    val preferenceUi by studentViewModel.preferenceListState.collectAsStateWithLifecycle()
    val draftPreferences by studentViewModel.draftPreferences.collectAsStateWithLifecycle()
    val activePackages by studentViewModel.activePackages.collectAsStateWithLifecycle()
    val payments by studentViewModel.paymentsState.collectAsStateWithLifecycle()
    val appointments by studentViewModel.appointmentsState.collectAsStateWithLifecycle()
    val notifications by studentViewModel.notificationsState.collectAsStateWithLifecycle()
    val readNotificationIds by studentViewModel.readNotificationIds.collectAsStateWithLifecycle()
    val tickets by studentViewModel.ticketsState.collectAsStateWithLifecycle()
    val filterState by studentViewModel.filterState.collectAsStateWithLifecycle()
    val compareIds by studentViewModel.compareIds.collectAsStateWithLifecycle()
    val checkoutState by studentViewModel.checkoutState.collectAsStateWithLifecycle()
    val appConfig by studentViewModel.appConfig.collectAsStateWithLifecycle()
    val isAdminAuthorized by studentViewModel.isAdminAuthorized.collectAsStateWithLifecycle()
    val toastMessage by studentViewModel.toastMessage.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(StudentTab.HOME) }
    var showPackagesScreen by remember { mutableStateOf(false) }
    var showAdminPortal by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            snackbarHostState.showSnackbar(toastMessage!!)
            studentViewModel.clearToast()
        }
    }

    if (showAdminPortal && isAdminAuthorized) {
        AdminPortalScreen(
            adminViewModel = adminViewModel,
            appConfig = appConfig,
            screenShieldEnabled = userSettings.screenShieldEnabled,
            onExitAdmin = { showAdminPortal = false }
        )
        return
    }

    val packages = (packagesUi as? UiState.Success)?.data ?: emptyList()
    if (showPackagesScreen) {
        PackagesAndPaymentScreen(
            packages = packages,
            activePackages = activePackages,
            payments = payments,
            checkoutState = checkoutState,
            appConfig = appConfig,
            screenShieldEnabled = userSettings.screenShieldEnabled,
            strings = strings,
            onBack = { showPackagesScreen = false },
            onSelectPackage = { studentViewModel.openPackageCheckout(it) },
            onDismissCheckout = { studentViewModel.dismissCheckout() },
            onPromoInputChange = { studentViewModel.updatePromoInput(it) },
            onApplyPromo = { studentViewModel.applyPromoCode() },
            onExecutePayment = { method, status -> studentViewModel.startCheckoutPayment(method, status) }
        )
        return
    }

    if (currentTab != StudentTab.HOME) {
        BackHandler { currentTab = StudentTab.HOME }
    }

    val unreadCount = notifications.count { !readNotificationIds.contains(it.id) }
    val tabLabels = mapOf(
        StudentTab.HOME to strings.navHome,
        StudentTab.COLLEGES to strings.navColleges,
        StudentTab.MY_LIST to strings.navMyList,
        StudentTab.ALERTS to strings.navNotifications,
        StudentTab.PROFILE to strings.navProfile
    )

    val profile = (profileUi as? UiState.Success)?.data
        ?: StudentProfile(userId = studentViewModel.currentUserId, fullName = studentViewModel.userDisplayName, email = studentViewModel.userEmail)
    val courses = (coursesUi as? UiState.Success)?.data ?: emptyList()
    val serverPref = (preferenceUi as? UiState.Success)?.data

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "${strings.appName} • ${tabLabels[currentTab] ?: ""}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        TextButton(
                            onClick = onToggleLanguage,
                            modifier = Modifier.testTag("topbar_lang_btn")
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = "Language")
                            Text(" ${if (userSettings.language == AppLanguage.ENGLISH) "मराठी" else "EN"}")
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        StudentTab.entries.forEach { tab ->
                            val label = tabLabels[tab].orEmpty()
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = {
                                    if (tab == StudentTab.ALERTS && unreadCount > 0) {
                                        BadgedBox(badge = { Badge { Text(unreadCount.toString()) } }) {
                                            Icon(tab.icon, contentDescription = label)
                                        }
                                    } else {
                                        Icon(tab.icon, contentDescription = label)
                                    }
                                },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag("bottom_tab_${tab.name}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        StudentTab.entries.forEach { tab ->
                            val label = tabLabels[tab].orEmpty()
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = label) },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (val pState = profileUi) {
                        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        is UiState.Error -> Box(Modifier.padding(16.dp)) {
                            ErrorStateBanner(
                                message = pState.message,
                                onRetry = { currentTab = StudentTab.HOME },
                                retryLabel = strings.retry
                            )
                        }
                        is UiState.Success -> {
                            when (currentTab) {
                                StudentTab.HOME -> StudentHomeScreen(
                                    profile = profile,
                                    courses = courses,
                                    activePackages = activePackages,
                                    preferenceList = serverPref,
                                    shortlistCount = shortlist.size,
                                    appointments = appointments,
                                    tickets = tickets,
                                    appConfig = appConfig,
                                    isAdminAuthorized = isAdminAuthorized,
                                    strings = strings,
                                    onNavigateToColleges = { currentTab = StudentTab.COLLEGES },
                                    onNavigateToPreferences = { currentTab = StudentTab.MY_LIST },
                                    onNavigateToProfile = { currentTab = StudentTab.PROFILE },
                                    onOpenPackages = { showPackagesScreen = true },
                                    onOpenAdminPortal = { showAdminPortal = true },
                                    onSelectCourse = { course ->
                                        studentViewModel.saveProfile(
                                            profile.copy(preferredCourseId = course.id, preferredCourseName = course.name)
                                        )
                                    },
                                    onToggleDocument = { studentViewModel.toggleDocumentItem(it) },
                                    onRequestAppointment = { t, d, s -> studentViewModel.requestAppointment(t, d, s) },
                                    onSubmitTicket = { sub, cat, msg -> studentViewModel.submitTicket(sub, cat, msg) }
                                )

                                StudentTab.COLLEGES -> CollegeExplorerScreen(
                                    colleges = filteredColleges,
                                    allColleges = allColleges,
                                    courses = courses,
                                    profile = profile,
                                    shortlistedIds = shortlist.map { it.collegeId }.toSet(),
                                    compareIds = compareIds,
                                    activePackages = activePackages,
                                    filterState = filterState,
                                    screenShieldEnabled = userSettings.screenShieldEnabled,
                                    strings = strings,
                                    onUpdateFilter = { studentViewModel.updateFilter(it) },
                                    onToggleShortlist = { studentViewModel.toggleShortlist(it) },
                                    onToggleCompare = { studentViewModel.toggleCompare(it) },
                                    onClearCompare = { studentViewModel.clearCompare() },
                                    onAddToPreference = { col, branch, code ->
                                        studentViewModel.addPreferenceItem(col, branch, code)
                                    },
                                    onOpenPackages = { showPackagesScreen = true }
                                )

                                StudentTab.MY_LIST -> PreferenceListScreen(
                                    shortlist = shortlist,
                                    allColleges = allColleges,
                                    preferenceItems = draftPreferences,
                                    serverPreferenceList = serverPref,
                                    activePackages = activePackages,
                                    screenShieldEnabled = userSettings.screenShieldEnabled,
                                    strings = strings,
                                    onUpdateShortlistNote = { id, note -> studentViewModel.updateShortlistNote(id, note) },
                                    onRemoveShortlist = { studentViewModel.toggleShortlist(it) },
                                    onAddPreference = { col, b, code -> studentViewModel.addPreferenceItem(col, b, code) },
                                    onMovePreference = { idx, delta -> studentViewModel.movePreference(idx, delta) },
                                    onRemovePreference = { idx -> studentViewModel.removePreferenceAt(idx) },
                                    onSaveDraft = { studentViewModel.saveOrSubmitPreferences(submitFinal = false) },
                                    onSubmitFinal = { studentViewModel.saveOrSubmitPreferences(submitFinal = true) },
                                    onOpenPackages = { showPackagesScreen = true }
                                )

                                StudentTab.ALERTS -> NotificationsScreen(
                                    notifications = notifications,
                                    readIds = readNotificationIds,
                                    onMarkRead = { studentViewModel.markNotificationRead(it) }
                                )

                                StudentTab.PROFILE -> StudentProfileScreen(
                                    profile = profile,
                                    courses = courses,
                                    userSettings = userSettings,
                                    isAdminAuthorized = isAdminAuthorized,
                                    strings = strings,
                                    onSaveProfile = { studentViewModel.saveProfile(it) },
                                    onToggleLanguage = onToggleLanguage,
                                    onSetThemeMode = onSetThemeMode,
                                    onToggleScreenShield = onToggleScreenShield,
                                    onOpenPackages = { showPackagesScreen = true },
                                    onOpenAdminPortal = { showAdminPortal = true },
                                    onDeleteAccount = {
                                        studentViewModel.deleteAccountData(onDeleted = onSignOut)
                                    },
                                    onSignOut = onSignOut
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
