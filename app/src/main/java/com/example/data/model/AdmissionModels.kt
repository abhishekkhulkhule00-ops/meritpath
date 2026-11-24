package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AdminRole {
    SUPER_ADMIN, ADMIN, COUNSELLOR, DATA_MANAGER;
    companion object {
        fun safeValueOf(raw: String): AdminRole = entries.firstOrNull { it.name == raw } ?: COUNSELLOR
    }
}

enum class CollegeType(val label: String) {
    GOVERNMENT("Government"),
    GOVT_AIDED("Govt Aided"),
    PRIVATE("Private"),
    AUTONOMOUS("Autonomous");
    companion object {
        fun safeValueOf(raw: String): CollegeType = entries.firstOrNull { it.name == raw } ?: PRIVATE
    }
}

enum class PackageTier(val rank: Int, val label: String) {
    BASIC(1, "Basic"),
    STANDARD(2, "Standard"),
    PREMIUM(3, "Premium"),
    COMBO(4, "Combo Bundle");
    companion object {
        fun safeValueOf(raw: String): PackageTier = entries.firstOrNull { it.name == raw } ?: BASIC
    }
}

enum class DiscountType {
    NONE, FIXED, PERCENT;
    companion object {
        fun safeValueOf(raw: String): DiscountType = entries.firstOrNull { it.name == raw } ?: NONE
    }
}

enum class PreferenceStatus {
    DRAFT, SUBMITTED, REVIEWED, LOCKED;
    companion object {
        fun safeValueOf(raw: String): PreferenceStatus = entries.firstOrNull { it.name == raw } ?: DRAFT
    }
}

enum class PaymentStatus {
    INITIATED, PENDING, SUCCESS, FAILED, CANCELLED, REFUND_INITIATED, REFUNDED;
    companion object {
        fun safeValueOf(raw: String): PaymentStatus = entries.firstOrNull { it.name == raw } ?: INITIATED
    }
}

enum class PaymentMode {
    SANDBOX, LIVE;
    companion object {
        fun safeValueOf(raw: String): PaymentMode = entries.firstOrNull { it.name == raw } ?: SANDBOX
    }
}

enum class CounsellingStatus(val label: String) {
    NEW("New"),
    PENDING("Pending"),
    CONTACTED("Contacted"),
    SCHEDULED("Appointment Scheduled"),
    IN_COUNSELLING("In Counselling"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");
    companion object {
        fun safeValueOf(raw: String): CounsellingStatus = entries.firstOrNull { it.name == raw } ?: NEW
    }
}

enum class NotificationType(val label: String) {
    ADMISSION("Admission Update"),
    CAP_ROUND("CAP Round"),
    CUTOFF("Cutoff Update"),
    PAYMENT("Payment"),
    PACKAGE("Package"),
    COUNSELLING("Counselling"),
    DEADLINE("Deadline"),
    GENERAL("General");
    companion object {
        fun safeValueOf(raw: String): NotificationType = entries.firstOrNull { it.name == raw } ?: GENERAL
    }
}

enum class TicketStatus {
    OPEN, IN_PROGRESS, RESOLVED;
    companion object {
        fun safeValueOf(raw: String): TicketStatus = entries.firstOrNull { it.name == raw } ?: OPEN
    }
}

@Keep
@IgnoreExtraProperties
data class AdminUser(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = AdminRole.COUNSELLOR.name,
    val permissions: List<String> = emptyList(),
    val active: Boolean = true,
    val specialization: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun roleEnum(): AdminRole = AdminRole.safeValueOf(role)
}

@Keep
@IgnoreExtraProperties
data class StudentProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studentCode: String = "",
    val fullName: String = "",
    val email: String = "",
    val mobile: String = "",
    val dob: String = "",
    val gender: String = "",
    val state: String = "",
    val district: String = "",
    val domicile: String = "",
    val preferredCourseId: String = "",
    val preferredCourseName: String = "",
    val entranceExam: String = "",
    val examScore: Double = 0.0,
    val percentile: Double = 0.0,
    val rank: Long = 0L,
    val category: String = "OPEN",
    val subcategory: String = "",
    val budgetMax: Long = 0L,
    val preferredLocations: String = "",
    val tenthPercent: Double = 0.0,
    val twelfthPercent: Double = 0.0,
    val counsellingStatus: String = CounsellingStatus.NEW.name,
    val assignedCounsellorId: String = "",
    val assignedCounsellorName: String = "",
    val internalNotes: String = "",
    val blocked: Boolean = false,
    val referralCode: String = "",
    val documentChecklist: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun computeCompletionPercent(): Int {
        val checks = listOf(
            fullName.isNotBlank(),
            email.isNotBlank(),
            mobile.isNotBlank(),
            dob.isNotBlank(),
            state.isNotBlank(),
            district.isNotBlank(),
            domicile.isNotBlank(),
            preferredCourseId.isNotBlank(),
            entranceExam.isNotBlank(),
            examScore > 0.0 || percentile > 0.0 || rank > 0L,
            category.isNotBlank(),
            budgetMax > 0L,
            preferredLocations.isNotBlank(),
            tenthPercent > 0.0,
            twelfthPercent > 0.0
        )
        return ((checks.count { it } * 100) / checks.size).coerceIn(0, 100)
    }
}

@Keep
@IgnoreExtraProperties
data class Course(
    @DocumentId val id: String = "",
    val name: String = "",
    val code: String = "",
    val description: String = "",
    val eligibility: String = "",
    val duration: String = "",
    val entranceExam: String = "",
    val active: Boolean = true,
    val displayOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CollegeBranch(
    val branchCode: String = "",
    val branchName: String = "",
    val courseId: String = "",
    val intake: Long = 0L,
    val seatsAvailable: Long = 0L,
    val tuitionFee: Long = 0L,
    val devFee: Long = 0L,
    val otherFee: Long = 0L,
    val hostelFee: Long = 0L,
    val totalFee: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class CollegeCutoff(
    val year: String = "",
    val capRound: String = "",
    val category: String = "",
    val quota: String = "",
    val courseId: String = "",
    val branchName: String = "",
    val percentile: Double? = null,
    val score: Double? = null,
    val rank: Long? = null
)

@Keep
@IgnoreExtraProperties
data class College(
    @DocumentId val id: String = "",
    val name: String = "",
    val code: String = "",
    val university: String = "",
    val address: String = "",
    val city: String = "",
    val district: String = "",
    val state: String = "",
    val collegeType: String = CollegeType.PRIVATE.name,
    val courseIds: List<String> = emptyList(),
    val branches: List<CollegeBranch> = emptyList(),
    val cutoffs: List<CollegeCutoff> = emptyList(),
    val scholarships: String = "",
    val eligibility: String = "",
    val placementAvgLpa: Double = 0.0,
    val placementHighLpa: Double = 0.0,
    val placementRate: Long = 0L,
    val hostelAvailable: Boolean = false,
    val hostelSummary: String = "",
    val website: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val description: String = "",
    val imageKey: String = "campus",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun collegeTypeEnum(): CollegeType = CollegeType.safeValueOf(collegeType)
    fun minAnnualFee(): Long = branches.minOfOrNull { it.totalFee } ?: 0L
}

@Keep
@IgnoreExtraProperties
data class CounsellingPackage(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val tier: String = PackageTier.BASIC.name,
    val courseIds: List<String> = emptyList(),
    val originalPrice: Long = 0L,
    val discountType: String = DiscountType.NONE.name,
    val discountValue: Long = 0L,
    val finalPrice: Long = 0L,
    val features: List<String> = emptyList(),
    val validityDays: Long = 180L,
    val displayOrder: Long = 0L,
    val active: Boolean = true,
    val combo: Boolean = false,
    val includedPackageIds: List<String> = emptyList(),
    val promoCode: String = "",
    val maxUsage: Long = 1000L,
    val usedCount: Long = 0L,
    val validUntilEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun tierEnum(): PackageTier = PackageTier.safeValueOf(tier)
    fun discountTypeEnum(): DiscountType = DiscountType.safeValueOf(discountType)
}

@Keep
@IgnoreExtraProperties
data class PromoCode(
    @DocumentId val id: String = "",
    val code: String = "",
    val discountType: String = DiscountType.FIXED.name,
    val discountValue: Long = 0L,
    val minOrderAmount: Long = 0L,
    val maxUsage: Long = 100L,
    val usedCount: Long = 0L,
    val active: Boolean = true,
    val expiryEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ShortlistItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val collegeId: String = "",
    val collegeName: String = "",
    val city: String = "",
    val note: String = "",
    val priority: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PreferenceItem(
    val order: Long = 1L,
    val collegeId: String = "",
    val collegeCode: String = "",
    val collegeName: String = "",
    val branchName: String = "",
    val city: String = "",
    val choiceCode: String = ""
)

@Keep
@IgnoreExtraProperties
data class StudentPreferenceList(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studentCode: String = "",
    val studentName: String = "",
    val mobile: String = "",
    val courseId: String = "",
    val courseName: String = "",
    val percentile: Double = 0.0,
    val examScore: Double = 0.0,
    val rank: Long = 0L,
    val category: String = "OPEN",
    val packageTier: String = "FREE",
    val items: List<PreferenceItem> = emptyList(),
    val status: String = PreferenceStatus.DRAFT.name,
    val assignedCounsellorId: String = "",
    val assignedCounsellorName: String = "",
    val counsellingStatus: String = CounsellingStatus.NEW.name,
    val adminNotes: String = "",
    val submittedAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): PreferenceStatus = PreferenceStatus.safeValueOf(status)
}

@Keep
@IgnoreExtraProperties
data class PaymentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studentName: String = "",
    val studentEmail: String = "",
    val packageId: String = "",
    val packageName: String = "",
    val packageTier: String = PackageTier.BASIC.name,
    val originalAmount: Long = 0L,
    val discountAmount: Long = 0L,
    val promoCode: String = "",
    val finalAmount: Long = 0L,
    val currency: String = "INR",
    val gatewayOrderId: String = "",
    val gatewayPaymentId: String = "",
    val gatewaySignature: String = "",
    val paymentMethod: String = "",
    val mode: String = PaymentMode.SANDBOX.name,
    val status: String = PaymentStatus.INITIATED.name,
    val receiptNumber: String = "",
    val failureReason: String = "",
    val verifiedByBackend: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): PaymentStatus = PaymentStatus.safeValueOf(status)
}

@Keep
@IgnoreExtraProperties
data class StudentPackage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val packageId: String = "",
    val packageName: String = "",
    val tier: String = PackageTier.BASIC.name,
    val paymentId: String = "",
    val features: List<String> = emptyList(),
    val active: Boolean = true,
    val activatedAtEpochMs: Long = 0L,
    val expiresAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun tierEnum(): PackageTier = PackageTier.safeValueOf(tier)
    fun isUnexpired(nowMs: Long = System.currentTimeMillis()): Boolean = active && expiresAtEpochMs >= nowMs
}

@Keep
@IgnoreExtraProperties
data class CounsellingAppointment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studentName: String = "",
    val mobile: String = "",
    val courseName: String = "",
    val counsellorId: String = "",
    val counsellorName: String = "",
    val topic: String = "",
    val scheduledDate: String = "",
    val scheduledSlot: String = "",
    val followUpDate: String = "",
    val status: String = CounsellingStatus.NEW.name,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): CounsellingStatus = CounsellingStatus.safeValueOf(status)
}

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = NotificationType.GENERAL.name,
    val targetUserId: String = "ALL",
    val targetCourseId: String = "",
    val targetPackageId: String = "",
    val audience: String = "ALL",
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun typeEnum(): NotificationType = NotificationType.safeValueOf(type)
}

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studentName: String = "",
    val subject: String = "",
    val message: String = "",
    val category: String = "General",
    val status: String = TicketStatus.OPEN.name,
    val adminReply: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): TicketStatus = TicketStatus.safeValueOf(status)
}

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val actorEmail: String = "",
    val action: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppConfig(
    @DocumentId val id: String = "",
    val whatsappNumber: String = "+919800012345",
    val helplinePhone: String = "+91 20 4000 5500",
    val supportEmail: String = "support@meritpath.in",
    val academicYear: String = "2026-27",
    val paymentMode: String = PaymentMode.SANDBOX.name,
    val backendUrl: String = "",
    val announcementBanner: String = "CAP Round 1 registration & choice filling dates updated for 2026-27.",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

fun mapPayloadFiltered(vararg pairs: Pair<String, Any?>): Map<String, Any> {
    val out = linkedMapOf<String, Any>()
    for ((k, v) in pairs) {
        if (v != null) out[k] = v
    }
    return out
}

fun serverTs(): Any = FieldValue.serverTimestamp()
