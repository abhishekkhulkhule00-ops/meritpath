package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.localization.AppLanguage
import com.example.core.localization.StringsProvider
import com.example.core.preferences.AppPreferencesRepository
import com.example.core.preferences.ThemeMode
import com.example.core.preferences.UserSettings
import com.example.data.payment.PaymentGatewayService
import com.example.data.repository.AdminRepository
import com.example.data.repository.AuthHelper
import com.example.data.repository.StudentRepository
import com.example.ui.screens.OnboardingAndAuthScreen
import com.example.ui.screens.StudentShell
import com.example.ui.theme.MeritPathTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.StudentViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeritPathAppRoot()
        }
    }
}

@Composable
fun MeritPathAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsRepo = remember { AppPreferencesRepository(context.applicationContext) }
    val userSettings by prefsRepo.settingsFlow.collectAsStateWithLifecycle(initialValue = UserSettings())

    val darkTheme = when (userSettings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val strings = remember(userSettings.language) { StringsProvider.get(userSettings.language) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    val credentialManager = remember { CredentialManager.create(context) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            AuthHelper.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val toggleLanguage: () -> Unit = {
        val next = if (userSettings.language == AppLanguage.ENGLISH) AppLanguage.MARATHI else AppLanguage.ENGLISH
        scope.launch { prefsRepo.setLanguage(next) }
    }

    MeritPathTheme(darkTheme = darkTheme) {
        val user = currentUser
        if (user == null) {
            OnboardingAndAuthScreen(
                language = userSettings.language,
                strings = strings,
                onToggleLanguage = toggleLanguage,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                }
            )
        } else {
            val uid = user.uid
            key(uid) {
                val studentViewModel: StudentViewModel = viewModel(
                    key = "student_$uid",
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY])
                            val dbId = app.getString(R.string.firestore_database_id)
                            val db = FirebaseFirestore.getInstance(dbId)
                            StudentViewModel(
                                studentRepo = StudentRepository(db),
                                adminRepo = AdminRepository(db),
                                gatewayService = PaymentGatewayService(),
                                currentUserId = uid,
                                userDisplayName = user.displayName ?: "Student",
                                userEmail = user.email ?: ""
                            )
                        }
                    }
                )
                val adminViewModel: AdminViewModel = viewModel(
                    key = "admin_$uid",
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY])
                            val dbId = app.getString(R.string.firestore_database_id)
                            val db = FirebaseFirestore.getInstance(dbId)
                            AdminViewModel(adminRepo = AdminRepository(db))
                        }
                    }
                )

                StudentShell(
                    studentViewModel = studentViewModel,
                    adminViewModel = adminViewModel,
                    userSettings = userSettings,
                    strings = strings,
                    onToggleLanguage = toggleLanguage,
                    onSetThemeMode = { mode -> scope.launch { prefsRepo.setThemeMode(mode) } },
                    onToggleScreenShield = { enabled -> scope.launch { prefsRepo.setScreenShieldEnabled(enabled) } },
                    onSignOut = {
                        AuthHelper.signOut(credentialManager = credentialManager, scope = scope)
                    }
                )
            }
        }
    }
}
