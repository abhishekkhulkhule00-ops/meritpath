package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminRole
import com.example.data.model.AdminUser
import com.example.data.model.AppConfig
import com.example.data.model.AuditLog
import com.example.data.model.College
import com.example.data.model.CounsellingAppointment
import com.example.data.model.CounsellingPackage
import com.example.data.model.CounsellingStatus
import com.example.data.model.Course
import com.example.data.model.DiscountType
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentStatus
import com.example.data.model.PromoCode
import com.example.data.model.StudentPreferenceList
import com.example.data.model.StudentProfile
import com.example.data.model.SupportTicket
import com.example.data.remote.CatalogSeedData
import com.example.data.repository.AdminRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminDashboardMetrics(
    val totalStudents: Int = 0,
    val blockedStudents: Int = 0,
    val totalRevenueInr: Long = 0L,
    val successfulPayments: Int = 0,
    val failedPayments: Int = 0,
    val pendingPayments: Int = 0,
    val totalCourses: Int = 0,
    val totalColleges: Int = 0,
    val totalPackages: Int = 0,
    val comboPackages: Int = 0,
    val submittedPreferences: Int = 0,
    val pendingCounselling: Int = 0
)

class AdminViewModel(
    private val adminRepo: AdminRepository
) : ViewModel() {

    private val _currentAdmin = MutableStateFlow<AdminUser?>(null)
    val currentAdmin: StateFlow<AdminUser?> = _currentAdmin.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    val students: StateFlow<List<StudentProfile>> = adminRepo.observeAllStudents()
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val courses: StateFlow<List<Course>> = adminRepo.observeAllCourses()
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val colleges: StateFlow<List<College>> = adminRepo.observeAllColleges()
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val packages: StateFlow<List<CounsellingPackage>> = adminRepo.observeAllPackages()
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val promoCodes: StateFlow<List<PromoCode>> = adminRepo.observeAllPromoCodes()
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val payments: StateFlow<List<PaymentRecord>> = adminRepo.observeAllPayments()
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val preferences: StateFlow<List<StudentPreferenceList>> = adminRepo.observeAllPreferences()
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val appointments: StateFlow<List<CounsellingAppointment>> = adminRepo.observeAllAppointments()
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val tickets: StateFlow<List<SupportTicket>> = adminRepo.observeAllTickets()
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = adminRepo.observeAuditLogs()
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val staffUsers: StateFlow<List<AdminUser>> = adminRepo.observeStaffUsers()
        .catch { if (it is CancellationException) throw it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val metrics: StateFlow<AdminDashboardMetrics> = combine(
        students,
        payments,
        courses,
        colleges,
        packages,
        preferences,
        appointments
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val stList = arr[0] as List<StudentProfile>
        @Suppress("UNCHECKED_CAST")
        val payList = arr[1] as List<PaymentRecord>
        @Suppress("UNCHECKED_CAST")
        val crsList = arr[2] as List<Course>
        @Suppress("UNCHECKED_CAST")
        val colList = arr[3] as List<College>
        @Suppress("UNCHECKED_CAST")
        val pkgList = arr[4] as List<CounsellingPackage>
        @Suppress("UNCHECKED_CAST")
        val prefList = arr[5] as List<StudentPreferenceList>
        @Suppress("UNCHECKED_CAST")
        val apptList = arr[6] as List<CounsellingAppointment>

        val successPays = payList.filter { it.statusEnum() == PaymentStatus.SUCCESS }
        AdminDashboardMetrics(
            totalStudents = stList.size,
            blockedStudents = stList.count { it.blocked },
            totalRevenueInr = successPays.sumOf { it.finalAmount },
            successfulPayments = successPays.size,
            failedPayments = payList.count { it.statusEnum() == PaymentStatus.FAILED },
            pendingPayments = payList.count { it.statusEnum() == PaymentStatus.PENDING || it.statusEnum() == PaymentStatus.INITIATED },
            totalCourses = crsList.size,
            totalColleges = colList.size,
            totalPackages = pkgList.size,
            comboPackages = pkgList.count { it.combo },
            submittedPreferences = prefList.count { it.status == "SUBMITTED" || it.status == "REVIEWED" },
            pendingCounselling = apptList.count { it.statusEnum() in listOf(CounsellingStatus.NEW, CounsellingStatus.PENDING) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AdminDashboardMetrics())

    init {
        viewModelScope.launch {
            _currentAdmin.value = adminRepo.resolveCurrentAdminAccess()
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun seedStarterCatalog() {
        viewModelScope.launch {
            val res = adminRepo.seedStarterCatalog()
            _statusBanner.value = if (res.isSuccess) {
                "Starter courses, colleges, packages & promos published to Firestore."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to publish catalog."
            }
        }
    }

    fun saveCourse(course: Course) {
        viewModelScope.launch {
            val res = adminRepo.saveCourse(course)
            _statusBanner.value = if (res.isSuccess) "Course saved." else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun saveCollege(college: College) {
        viewModelScope.launch {
            val res = adminRepo.saveCollege(college)
            _statusBanner.value = if (res.isSuccess) "College saved." else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun importCollegesCsv(csv: String) {
        viewModelScope.launch {
            val res = adminRepo.importCollegesFromCsv(csv)
            _statusBanner.value = if (res.isSuccess) {
                "Imported ${res.getOrDefault(0)} colleges from CSV."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "CSV import failed."
            }
        }
    }

    fun savePackage(pkg: CounsellingPackage) {
        viewModelScope.launch {
            val res = adminRepo.savePackage(pkg)
            _statusBanner.value = if (res.isSuccess) "Package saved." else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun savePromoCode(code: String, type: DiscountType, value: Long, minOrder: Long, maxUsage: Long) {
        viewModelScope.launch {
            val res = adminRepo.savePromoCode(code, type, value, minOrder, maxUsage)
            _statusBanner.value = if (res.isSuccess) "Promo code saved." else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun updateStudent(uid: String, blocked: Boolean, status: CounsellingStatus, counsellor: String, notes: String) {
        viewModelScope.launch {
            val res = adminRepo.updateStudentAdminFields(uid, blocked, status, counsellor, notes)
            _statusBanner.value = if (res.isSuccess) "Student updated." else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun reviewPreferenceList(prefUid: String, status: String, counsellingStatus: String, counsellor: String, notes: String) {
        viewModelScope.launch {
            val res = adminRepo.updatePreferenceReview(prefUid, status, counsellingStatus, counsellor, notes)
            _statusBanner.value = if (res.isSuccess) "Preference review updated." else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun refundPayment(paymentId: String, reason: String) {
        viewModelScope.launch {
            val res = adminRepo.processRefund(paymentId, reason)
            _statusBanner.value = if (res.isSuccess) "Refund recorded in audit log." else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun updateAppointment(id: String, status: CounsellingStatus, counsellor: String, followUp: String, notes: String) {
        viewModelScope.launch {
            val res = adminRepo.updateAppointment(id, status, counsellor, followUp, notes)
            _statusBanner.value = if (res.isSuccess) "Appointment updated." else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun sendNotification(title: String, message: String, type: String, audience: String, targetUserId: String) {
        viewModelScope.launch {
            val res = adminRepo.sendNotification(title, message, type, audience, targetUserId)
            _statusBanner.value = if (res.isSuccess) "Notification dispatched." else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun saveStaffRole(uid: String, email: String, name: String, role: AdminRole, active: Boolean) {
        viewModelScope.launch {
            val res = adminRepo.saveStaffRole(uid, email, name, role, active)
            _statusBanner.value = if (res.isSuccess) "Staff role updated." else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun saveAppConfig(config: AppConfig) {
        viewModelScope.launch {
            val res = adminRepo.saveAppConfig(config)
            _statusBanner.value = if (res.isSuccess) "Configuration saved." else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun buildCsvReport(reportType: String): String {
        return when (reportType) {
            "STUDENTS" -> buildString {
                appendLine("StudentCode,FullName,Email,Mobile,Course,Percentile,Rank,Category,Status,Blocked")
                students.value.forEach {
                    appendLine("${it.studentCode},\"${it.fullName}\",${it.email},${it.mobile},\"${it.preferredCourseName}\",${it.percentile},${it.rank},${it.category},${it.counsellingStatus},${it.blocked}")
                }
            }
            "PAYMENTS" -> buildString {
                appendLine("Receipt,OrderId,Student,Package,OriginalINR,DiscountINR,FinalINR,Method,Mode,Status")
                payments.value.forEach {
                    appendLine("${it.receiptNumber},${it.gatewayOrderId},\"${it.studentName}\",\"${it.packageName}\",${it.originalAmount},${it.discountAmount},${it.finalAmount},${it.paymentMethod},${it.mode},${it.status}")
                }
            }
            "PREFERENCES" -> buildString {
                appendLine("StudentCode,StudentName,Course,Percentile,Rank,Category,ChoicesCount,Status,Counsellor")
                preferences.value.forEach {
                    appendLine("${it.studentCode},\"${it.studentName}\",\"${it.courseName}\",${it.percentile},${it.rank},${it.category},${it.items.size},${it.status},\"${it.assignedCounsellorName}\"")
                }
            }
            "COLLEGES" -> buildString {
                appendLine("Code,Name,City,Type,University,BranchesCount,PlacementAvgLPA")
                (colleges.value.ifEmpty { CatalogSeedData.starterColleges }).forEach {
                    appendLine("${it.code},\"${it.name}\",${it.city},${it.collegeType},\"${it.university}\",${it.branches.size},${it.placementAvgLpa}")
                }
            }
            else -> "No data"
        }
    }
}
