package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppConfig
import com.example.data.model.AppNotification
import com.example.data.model.College
import com.example.data.model.CounsellingAppointment
import com.example.data.model.CounsellingPackage
import com.example.data.model.Course
import com.example.data.model.PackageTier
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentStatus
import com.example.data.model.PreferenceItem
import com.example.data.model.PreferenceStatus
import com.example.data.model.PromoCode
import com.example.data.model.ShortlistItem
import com.example.data.model.StudentPackage
import com.example.data.model.StudentPreferenceList
import com.example.data.model.StudentProfile
import com.example.data.model.SupportTicket
import com.example.data.payment.GatewayCheckoutOrder
import com.example.data.payment.PaymentGatewayService
import com.example.data.remote.CatalogSeedData
import com.example.data.repository.AdminRepository
import com.example.data.repository.StudentRepository
import com.example.domain.CounselingEngine
import com.example.domain.PricingBreakdown
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class CollegeSortOption {
    NAME, FEE_ASC, CUTOFF_DESC, PLACEMENT_DESC
}

data class CollegeFilterState(
    val query: String = "",
    val courseId: String = "",
    val city: String = "",
    val collegeType: String = "",
    val maxFee: Long? = null,
    val sortOption: CollegeSortOption = CollegeSortOption.NAME
)

data class CheckoutSessionState(
    val selectedPackage: CounsellingPackage? = null,
    val promoInput: String = "",
    val validatedPromo: PromoCode? = null,
    val promoMessage: String? = null,
    val pricing: PricingBreakdown? = null,
    val activeOrder: GatewayCheckoutOrder? = null,
    val processing: Boolean = false,
    val lastResultMessage: String? = null
)

class StudentViewModel(
    private val studentRepo: StudentRepository,
    private val adminRepo: AdminRepository,
    private val gatewayService: PaymentGatewayService,
    val currentUserId: String,
    val userDisplayName: String,
    val userEmail: String
) : ViewModel() {

    private fun <T> Flow<T>.toUiStateFlow(errorLabel: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                val root = fsErr ?: error
                emit(UiState.Error(root.message ?: errorLabel))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val appConfig: StateFlow<AppConfig> = studentRepo.observeAppConfig()
        .map { it ?: CatalogSeedData.defaultAppConfig }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(CatalogSeedData.defaultAppConfig)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CatalogSeedData.defaultAppConfig)

    val profileState: StateFlow<UiState<StudentProfile>> = studentRepo.observeStudentProfile(currentUserId)
        .map { it ?: StudentProfile(userId = currentUserId, fullName = userDisplayName, email = userEmail) }
        .toUiStateFlow("Failed to load student profile")

    val coursesState: StateFlow<UiState<List<Course>>> = studentRepo.observeCourses()
        .map { list -> list.ifEmpty { CatalogSeedData.starterCourses } }
        .toUiStateFlow("Failed to load courses")

    private val rawCollegesFlow = studentRepo.observeColleges()
        .map { list -> list.ifEmpty { CatalogSeedData.starterColleges } }

    val allColleges: StateFlow<List<College>> = rawCollegesFlow
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CatalogSeedData.starterColleges)

    val packagesState: StateFlow<UiState<List<CounsellingPackage>>> = studentRepo.observePackages()
        .map { list -> list.ifEmpty { CatalogSeedData.starterPackages } }
        .toUiStateFlow("Failed to load packages")

    val shortlistState: StateFlow<List<ShortlistItem>> = studentRepo.observeShortlist(currentUserId)
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val preferenceListState: StateFlow<UiState<StudentPreferenceList?>> =
        studentRepo.observePreferenceList(currentUserId)
            .toUiStateFlow("Failed to load preference list")

    val activePackages: StateFlow<List<StudentPackage>> = studentRepo.observeStudentPackages(currentUserId)
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val paymentsState: StateFlow<List<PaymentRecord>> = studentRepo.observePayments(currentUserId)
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val appointmentsState: StateFlow<List<CounsellingAppointment>> = studentRepo.observeAppointments(currentUserId)
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notificationsState: StateFlow<List<AppNotification>> = studentRepo.observeNotifications(currentUserId)
        .map { list -> list.ifEmpty { CatalogSeedData.starterNotifications } }
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val readNotificationIds: StateFlow<Set<String>> = studentRepo.observeReadNotificationIds(currentUserId)
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val ticketsState: StateFlow<List<SupportTicket>> = studentRepo.observeSupportTickets(currentUserId)
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _filterState = MutableStateFlow(CollegeFilterState())
    val filterState: StateFlow<CollegeFilterState> = _filterState.asStateFlow()

    val filteredColleges: StateFlow<List<College>> = combine(allColleges, _filterState) { colleges, f ->
        val q = f.query.trim().lowercase()
        colleges.filter { col ->
            val matchesQuery = q.isBlank() ||
                col.name.lowercase().contains(q) ||
                col.code.lowercase().contains(q) ||
                col.city.lowercase().contains(q) ||
                col.branches.any { it.branchName.lowercase().contains(q) }
            val matchesCourse = f.courseId.isBlank() || col.courseIds.contains(f.courseId)
            val matchesCity = f.city.isBlank() || col.city.equals(f.city, ignoreCase = true)
            val matchesType = f.collegeType.isBlank() || col.collegeType == f.collegeType
            val matchesFee = f.maxFee == null || col.minAnnualFee() <= f.maxFee
            matchesQuery && matchesCourse && matchesCity && matchesType && matchesFee
        }.let { list ->
            when (f.sortOption) {
                CollegeSortOption.NAME -> list.sortedBy { it.name }
                CollegeSortOption.FEE_ASC -> list.sortedBy { it.minAnnualFee() }
                CollegeSortOption.CUTOFF_DESC -> list.sortedByDescending { col ->
                    col.cutoffs.mapNotNull { it.percentile }.maxOrNull() ?: 0.0
                }
                CollegeSortOption.PLACEMENT_DESC -> list.sortedByDescending { it.placementAvgLpa }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CatalogSeedData.starterColleges)

    private val _compareIds = MutableStateFlow<List<String>>(emptyList())
    val compareIds: StateFlow<List<String>> = _compareIds.asStateFlow()

    private val _draftPreferences = MutableStateFlow<List<PreferenceItem>>(emptyList())
    val draftPreferences: StateFlow<List<PreferenceItem>> = _draftPreferences.asStateFlow()
    private var draftHydratedFromServer = false

    private val _checkoutState = MutableStateFlow(CheckoutSessionState())
    val checkoutState: StateFlow<CheckoutSessionState> = _checkoutState.asStateFlow()

    private val _isAdminAuthorized = MutableStateFlow(false)
    val isAdminAuthorized: StateFlow<Boolean> = _isAdminAuthorized.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        viewModelScope.launch {
            studentRepo.ensureStudentProfileExists(currentUserId, userDisplayName, userEmail)
            _isAdminAuthorized.value = adminRepo.resolveCurrentAdminAccess() != null
        }
        viewModelScope.launch {
            preferenceListState.collect { state ->
                if (!draftHydratedFromServer && state is UiState.Success) {
                    val serverItems = state.data?.items
                    if (serverItems != null) {
                        _draftPreferences.value = serverItems
                    }
                    draftHydratedFromServer = true
                }
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun updateFilter(transform: (CollegeFilterState) -> CollegeFilterState) {
        _filterState.update(transform)
    }

    fun toggleCompare(collegeId: String) {
        _compareIds.update { current ->
            if (current.contains(collegeId)) {
                current - collegeId
            } else if (current.size >= 3) {
                _toastMessage.value = "You can compare up to 3 colleges at a time."
                current
            } else {
                current + collegeId
            }
        }
    }

    fun clearCompare() {
        _compareIds.value = emptyList()
    }

    fun saveProfile(profile: StudentProfile, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val res = studentRepo.updateStudentProfile(profile)
            if (res.isSuccess) {
                _toastMessage.value = "Profile saved successfully."
                onSuccess()
            } else {
                _toastMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save profile."
            }
        }
    }

    fun toggleDocumentItem(docName: String) {
        val currentProfile = (profileState.value as? UiState.Success)?.data ?: return
        val nextList = if (currentProfile.documentChecklist.contains(docName)) {
            currentProfile.documentChecklist - docName
        } else {
            currentProfile.documentChecklist + docName
        }
        saveProfile(currentProfile.copy(documentChecklist = nextList))
    }

    fun toggleShortlist(college: College) {
        val saved = shortlistState.value.any { it.collegeId == college.id }
        viewModelScope.launch {
            val res = studentRepo.toggleShortlist(college, saved)
            if (res.isSuccess) {
                _toastMessage.value = if (saved) "Removed from shortlist." else "Added to shortlist."
            } else {
                _toastMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Shortlist update failed."
            }
        }
    }

    fun updateShortlistNote(collegeId: String, note: String) {
        viewModelScope.launch {
            val res = studentRepo.updateShortlistNote(collegeId, note)
            _toastMessage.value = if (res.isSuccess) "Note updated." else "Could not update note."
        }
    }

    fun addPreferenceItem(college: College, branchName: String, choiceCode: String) {
        val currentServerStatus = (preferenceListState.value as? UiState.Success)?.data?.statusEnum()
        if (currentServerStatus != null && currentServerStatus != PreferenceStatus.DRAFT) {
            _toastMessage.value = "Preference list already submitted and locked."
            return
        }
        _draftPreferences.update { list ->
            val exists = list.any { it.collegeId == college.id && it.branchName == branchName }
            if (exists) {
                _toastMessage.value = "This college branch is already in your preference list."
                list
            } else {
                val next = list + PreferenceItem(
                    order = (list.size + 1).toLong(),
                    collegeId = college.id,
                    collegeCode = college.code,
                    collegeName = college.name,
                    branchName = branchName,
                    city = college.city,
                    choiceCode = choiceCode
                )
                _toastMessage.value = "Added #${next.size} to Preference List."
                next
            }
        }
    }

    fun movePreference(index: Int, delta: Int) {
        _draftPreferences.update { list ->
            val target = index + delta
            if (index !in list.indices || target !in list.indices) return@update list
            val mutable = list.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(target, item)
            mutable.mapIndexed { idx, p -> p.copy(order = (idx + 1).toLong()) }
        }
    }

    fun removePreferenceAt(index: Int) {
        _draftPreferences.update { list ->
            if (index !in list.indices) return@update list
            list.toMutableList().apply { removeAt(index) }
                .mapIndexed { idx, p -> p.copy(order = (idx + 1).toLong()) }
        }
    }

    fun saveOrSubmitPreferences(submitFinal: Boolean) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        val tier = CounselingEngine.highestActiveTier(activePackages.value)?.name ?: "FREE"
        viewModelScope.launch {
            val res = studentRepo.savePreferenceList(
                profile = profile,
                items = _draftPreferences.value,
                packageTier = tier,
                submitFinal = submitFinal
            )
            _toastMessage.value = if (res.isSuccess) {
                if (submitFinal) "Final Preference List submitted securely!" else "Draft saved."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to save preference list."
            }
        }
    }

    fun openPackageCheckout(pkg: CounsellingPackage) {
        val pricing = CounselingEngine.calculatePackagePricing(pkg, null)
        _checkoutState.value = CheckoutSessionState(
            selectedPackage = pkg,
            pricing = pricing
        )
    }

    fun dismissCheckout() {
        _checkoutState.value = CheckoutSessionState()
    }

    fun updatePromoInput(code: String) {
        _checkoutState.update { it.copy(promoInput = code, promoMessage = null) }
    }

    fun applyPromoCode() {
        val state = _checkoutState.value
        val pkg = state.selectedPackage ?: return
        val code = state.promoInput.trim()
        if (code.isBlank()) return
        viewModelScope.launch {
            val res = studentRepo.lookupPromoCode(code)
            val promo = res.getOrNull()
            if (promo == null) {
                _checkoutState.update { it.copy(promoMessage = "Promo code '$code' not found.", validatedPromo = null) }
                return@launch
            }
            val pricing = CounselingEngine.calculatePackagePricing(pkg, promo)
            _checkoutState.update {
                it.copy(
                    validatedPromo = promo,
                    pricing = pricing,
                    promoMessage = pricing.promoError ?: "Promo '${promo.code}' applied! Saved ₹${pricing.promoDiscount}"
                )
            }
        }
    }

    fun startCheckoutPayment(method: String, simulatedOutcome: PaymentStatus) {
        val session = _checkoutState.value
        val pkg = session.selectedPackage ?: return
        val pricing = session.pricing ?: CounselingEngine.calculatePackagePricing(pkg, session.validatedPromo)
        val profile = (profileState.value as? UiState.Success)?.data
            ?: StudentProfile(userId = currentUserId, fullName = userDisplayName, email = userEmail)

        viewModelScope.launch {
            _checkoutState.update { it.copy(processing = true, lastResultMessage = null) }
            val idToken = try {
                Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
            } catch (_: Exception) {
                null
            }
            val cfg = appConfig.value
            val order = gatewayService.createOrder(
                backendUrl = cfg.backendUrl,
                idToken = idToken,
                packageId = pkg.id,
                promoCode = pricing.appliedPromoCode,
                finalAmountInr = pricing.finalPayable
            )
            val verification = gatewayService.verifyOutcome(
                backendUrl = cfg.backendUrl,
                idToken = idToken,
                order = order,
                packageId = pkg.id,
                simulatedOutcome = simulatedOutcome
            )
            val recordRes = studentRepo.recordVerifiedPaymentAndActivate(
                profile = profile,
                pkg = pkg,
                originalAmount = pricing.originalPrice,
                discountAmount = pricing.totalDiscount,
                promoCode = pricing.appliedPromoCode,
                finalAmount = pricing.finalPayable,
                orderId = order.orderId,
                paymentId = verification.gatewayPaymentId,
                signature = verification.signature,
                method = method,
                mode = order.mode,
                status = verification.status,
                receiptNumber = verification.receiptNumber,
                failureReason = verification.failureReason,
                verifiedByBackend = verification.verifiedByBackend
            )
            _checkoutState.update {
                it.copy(
                    processing = false,
                    activeOrder = order,
                    lastResultMessage = if (recordRes.isSuccess && verification.status == PaymentStatus.SUCCESS) {
                        "Payment verified (${verification.receiptNumber}). ${pkg.name} unlocked!"
                    } else if (recordRes.isSuccess) {
                        "Payment ${verification.status.name}: ${verification.failureReason}"
                    } else {
                        recordRes.exceptionOrNull()?.localizedMessage ?: "Payment recording failed."
                    }
                )
            }
            if (recordRes.isSuccess && verification.status == PaymentStatus.SUCCESS) {
                _toastMessage.value = "${pkg.name} activated!"
                dismissCheckout()
            }
        }
    }

    fun requestAppointment(topic: String, date: String, slot: String) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val res = studentRepo.requestAppointment(profile, topic, date, slot)
            _toastMessage.value = if (res.isSuccess) {
                "Counselling request submitted!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Could not request appointment."
            }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { studentRepo.markNotificationRead(id) }
    }

    fun submitTicket(subject: String, category: String, message: String) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val res = studentRepo.submitSupportTicket(profile, subject, category, message)
            _toastMessage.value = if (res.isSuccess) "Support ticket submitted." else "Failed to submit ticket."
        }
    }

    fun deleteAccountData(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val res = studentRepo.deleteStudentAccountData(currentUserId)
            if (res.isSuccess) onDeleted()
            else _toastMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete account data."
        }
    }
}
