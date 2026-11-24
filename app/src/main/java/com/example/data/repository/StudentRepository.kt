package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppConfig
import com.example.data.model.AppNotification
import com.example.data.model.College
import com.example.data.model.CounsellingAppointment
import com.example.data.model.CounsellingPackage
import com.example.data.model.CounsellingStatus
import com.example.data.model.Course
import com.example.data.model.PaymentMode
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
import com.example.data.model.TicketStatus
import com.example.data.model.mapPayloadFiltered
import com.example.data.model.serverTs
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StudentRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    private fun <T> observeListSafe(
        path: String,
        flowProvider: () -> Flow<List<T>>
    ): Flow<List<T>> = flow {
        val uid = requireUserId()
        emitAll(
            flowProvider().catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                }
                throw error
            }
        )
    }

    fun observeAppConfig(): Flow<AppConfig?> = flow {
        requireUserId()
        emitAll(
            db.collection("app_config").document("settings")
                .snapshots()
                .map { snap -> snap.toObject(AppConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    emit(null)
                }
        )
    }

    fun observeCourses(): Flow<List<Course>> = observeListSafe("courses") {
        db.collection("courses").snapshots().map { snap ->
            snap.toObjects(Course::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .filter { it.active }
                .sortedBy { it.displayOrder }
        }
    }

    fun observeColleges(): Flow<List<College>> = observeListSafe("colleges") {
        db.collection("colleges").snapshots().map { snap ->
            snap.toObjects(College::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .filter { it.active }
                .sortedBy { it.name }
        }
    }

    fun observePackages(): Flow<List<CounsellingPackage>> = observeListSafe("packages") {
        db.collection("packages").snapshots().map { snap ->
            snap.toObjects(CounsellingPackage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .filter { it.active }
                .sortedBy { it.displayOrder }
        }
    }

    fun observeStudentProfile(userId: String): Flow<StudentProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User mismatch" }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId).snapshots()
                .map { snap -> snap.toObject(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun ensureStudentProfileExists(userId: String, displayName: String, email: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Cross-user write blocked" }
            val docRef = db.collection("users").document(userId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.ifBlank { email.substringBefore("@").ifBlank { "Student" } }.take(120)
                val code = "MP-${uid.take(6).uppercase()}"
                val payload = mapPayloadFiltered(
                    "userId" to uid,
                    "studentCode" to code,
                    "fullName" to safeName,
                    "email" to email.take(200),
                    "mobile" to "",
                    "category" to "OPEN",
                    "counsellingStatus" to CounsellingStatus.NEW.name,
                    "blocked" to false,
                    "referralCode" to "REF-${uid.take(5).uppercase()}",
                    "documentChecklist" to listOf("10th Marksheet", "12th Marksheet"),
                    "createdAt" to serverTs(),
                    "updatedAt" to serverTs()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateStudentProfile(profile: StudentProfile): Result<Unit> {
        val path = "users/${profile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId) { "Cross-user profile update blocked" }
            require(profile.fullName.trim().length in 1..120) { "Full name must be 1 to 120 characters." }
            require(profile.percentile in 0.0..100.0) { "Percentile must be between 0 and 100." }
            val updates = mapPayloadFiltered(
                "fullName" to profile.fullName.trim().take(120),
                "email" to profile.email.trim().take(200),
                "mobile" to profile.mobile.trim().take(20),
                "dob" to profile.dob.trim().take(30),
                "gender" to profile.gender.trim().take(30),
                "state" to profile.state.trim().take(60),
                "district" to profile.district.trim().take(60),
                "domicile" to profile.domicile.trim().take(60),
                "preferredCourseId" to profile.preferredCourseId.take(128),
                "preferredCourseName" to profile.preferredCourseName.take(120),
                "entranceExam" to profile.entranceExam.trim().take(80),
                "examScore" to profile.examScore.coerceIn(0.0, 1000.0),
                "percentile" to profile.percentile.coerceIn(0.0, 100.0),
                "rank" to profile.rank.coerceIn(0L, 5000000L),
                "category" to profile.category.trim().take(40),
                "subcategory" to profile.subcategory.trim().take(60),
                "budgetMax" to profile.budgetMax.coerceIn(0L, 100000000L),
                "preferredLocations" to profile.preferredLocations.trim().take(250),
                "tenthPercent" to profile.tenthPercent.coerceIn(0.0, 100.0),
                "twelfthPercent" to profile.twelfthPercent.coerceIn(0.0, 100.0),
                "documentChecklist" to profile.documentChecklist.take(25),
                "updatedAt" to serverTs()
            )
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeShortlist(userId: String): Flow<List<ShortlistItem>> = observeListSafe("users/$userId/shortlist") {
        val uid = requireUserId()
        check(uid == userId)
        db.collection("users").document(userId).collection("shortlist")
            .snapshots()
            .map { snap ->
                snap.toObjects(ShortlistItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.priority }
            }
    }

    suspend fun toggleShortlist(college: College, currentlySaved: Boolean): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/shortlist/${college.id}"
        return try {
            val ref = db.collection("users").document(uid).collection("shortlist").document(college.id)
            if (currentlySaved) {
                ref.delete().await()
            } else {
                val payload = mapPayloadFiltered(
                    "userId" to uid,
                    "collegeId" to college.id,
                    "collegeName" to college.name.take(200),
                    "city" to college.city.take(80),
                    "note" to "",
                    "priority" to 1L,
                    "createdAt" to serverTs(),
                    "updatedAt" to serverTs()
                )
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateShortlistNote(collegeId: String, note: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/shortlist/$collegeId"
        return try {
            db.collection("users").document(uid).collection("shortlist").document(collegeId)
                .update(mapPayloadFiltered("note" to note.take(500), "updatedAt" to serverTs()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observePreferenceList(userId: String): Flow<StudentPreferenceList?> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "student_preferences/$userId"
        emitAll(
            db.collection("student_preferences").document(userId).snapshots()
                .map { snap -> snap.toObject(StudentPreferenceList::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun savePreferenceList(
        profile: StudentProfile,
        items: List<PreferenceItem>,
        packageTier: String,
        submitFinal: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        val path = "student_preferences/$uid"
        return try {
            require(items.size <= 100) { "Maximum 100 preferences supported." }
            if (submitFinal) {
                require(items.isNotEmpty()) { "Add at least one college preference before submitting." }
            }
            val orderedMaps = items.mapIndexed { idx, item ->
                mapOf(
                    "order" to (idx + 1L),
                    "collegeId" to item.collegeId,
                    "collegeCode" to item.collegeCode,
                    "collegeName" to item.collegeName,
                    "branchName" to item.branchName,
                    "city" to item.city,
                    "choiceCode" to item.choiceCode
                )
            }
            val docRef = db.collection("student_preferences").document(uid)
            val existing = docRef.get().await()
            val newStatus = if (submitFinal) PreferenceStatus.SUBMITTED.name else PreferenceStatus.DRAFT.name
            val submittedEpoch = if (submitFinal) System.currentTimeMillis() else null

            if (!existing.exists()) {
                val payload = mapPayloadFiltered(
                    "userId" to uid,
                    "studentCode" to profile.studentCode.ifBlank { "MP-${uid.take(6).uppercase()}" },
                    "studentName" to profile.fullName.ifBlank { "Student" }.take(120),
                    "mobile" to profile.mobile.take(20),
                    "courseId" to profile.preferredCourseId.ifBlank { "course_btech" },
                    "courseName" to profile.preferredCourseName.ifBlank { "B.Tech / B.E. Engineering" },
                    "percentile" to profile.percentile,
                    "examScore" to profile.examScore,
                    "rank" to profile.rank,
                    "category" to profile.category.ifBlank { "OPEN" },
                    "packageTier" to packageTier,
                    "items" to orderedMaps,
                    "status" to newStatus,
                    "counsellingStatus" to profile.counsellingStatus,
                    "submittedAtEpochMs" to submittedEpoch,
                    "createdAt" to serverTs(),
                    "updatedAt" to serverTs()
                )
                docRef.set(payload).await()
            } else {
                val currentStatus = existing.getString("status") ?: PreferenceStatus.DRAFT.name
                check(currentStatus == PreferenceStatus.DRAFT.name) {
                    "Submitted preference list is locked. Request an administrator unlock to edit."
                }
                val updates = mapPayloadFiltered(
                    "studentName" to profile.fullName.ifBlank { "Student" }.take(120),
                    "mobile" to profile.mobile.take(20),
                    "courseId" to profile.preferredCourseId.ifBlank { "course_btech" },
                    "courseName" to profile.preferredCourseName.ifBlank { "B.Tech / B.E. Engineering" },
                    "percentile" to profile.percentile,
                    "examScore" to profile.examScore,
                    "rank" to profile.rank,
                    "category" to profile.category.ifBlank { "OPEN" },
                    "packageTier" to packageTier,
                    "items" to orderedMaps,
                    "status" to newStatus,
                    "submittedAtEpochMs" to submittedEpoch,
                    "updatedAt" to serverTs()
                )
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun lookupPromoCode(code: String): Result<PromoCode?> {
        val clean = code.trim().lowercase()
        if (clean.isBlank()) return Result.success(null)
        val path = "promo_codes/promo_$clean"
        return try {
            requireUserId()
            val snap = db.collection("promo_codes").document("promo_$clean").get().await()
            val promo = snap.toObject(PromoCode::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(promo)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    fun observeStudentPackages(userId: String): Flow<List<StudentPackage>> = observeListSafe("student_packages") {
        val uid = requireUserId()
        check(uid == userId)
        db.collection("student_packages")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(StudentPackage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.activatedAtEpochMs }
            }
    }

    fun observePayments(userId: String): Flow<List<PaymentRecord>> = observeListSafe("payments") {
        val uid = requireUserId()
        check(uid == userId)
        db.collection("payments")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(PaymentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
    }

    /**
     * Records payment transaction and—ONLY when status == SUCCESS & verifiedByBackend == true—
     * atomically activates the StudentPackage entitlement in the same WriteBatch (enforced by firestore.rules existsAfter).
     */
    suspend fun recordVerifiedPaymentAndActivate(
        profile: StudentProfile,
        pkg: CounsellingPackage,
        originalAmount: Long,
        discountAmount: Long,
        promoCode: String?,
        finalAmount: Long,
        orderId: String,
        paymentId: String,
        signature: String,
        method: String,
        mode: PaymentMode,
        status: PaymentStatus,
        receiptNumber: String,
        failureReason: String,
        verifiedByBackend: Boolean
    ): Result<String> {
        val uid = requireUserId()
        val payDocId = "pay_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val path = "payments/$payDocId"
        return try {
            val batch = db.batch()
            val payRef = db.collection("payments").document(payDocId)
            val payPayload = mapPayloadFiltered(
                "userId" to uid,
                "studentName" to profile.fullName.ifBlank { "Student" }.take(120),
                "studentEmail" to profile.email.take(200),
                "packageId" to pkg.id,
                "packageName" to pkg.name.take(120),
                "packageTier" to pkg.tier,
                "originalAmount" to originalAmount,
                "discountAmount" to discountAmount,
                "promoCode" to promoCode?.takeIf { it.isNotBlank() },
                "finalAmount" to finalAmount,
                "currency" to "INR",
                "gatewayOrderId" to orderId,
                "gatewayPaymentId" to paymentId.takeIf { it.isNotBlank() },
                "gatewaySignature" to signature.takeIf { it.isNotBlank() },
                "paymentMethod" to method.take(60),
                "mode" to mode.name,
                "status" to status.name,
                "receiptNumber" to receiptNumber.takeIf { it.isNotBlank() },
                "failureReason" to failureReason.takeIf { it.isNotBlank() },
                "verifiedByBackend" to verifiedByBackend,
                "createdAt" to serverTs(),
                "updatedAt" to serverTs()
            )
            batch.set(payRef, payPayload)

            if (status == PaymentStatus.SUCCESS && verifiedByBackend) {
                val nowMs = System.currentTimeMillis()
                val expiryMs = nowMs + pkg.validityDays.coerceAtLeast(1L) * 24L * 60L * 60L * 1000L
                val entId = "${uid}_${pkg.id}"
                val entRef = db.collection("student_packages").document(entId)
                val entPayload = mapPayloadFiltered(
                    "userId" to uid,
                    "packageId" to pkg.id,
                    "packageName" to pkg.name.take(120),
                    "tier" to pkg.tier,
                    "paymentId" to payDocId,
                    "features" to pkg.features.take(20),
                    "active" to true,
                    "activatedAtEpochMs" to nowMs,
                    "expiresAtEpochMs" to expiryMs,
                    "createdAt" to serverTs(),
                    "updatedAt" to serverTs()
                )
                batch.set(entRef, entPayload)

                val notifId = "notif_pay_${UUID.randomUUID().toString().replace("-", "").take(10)}"
                val notifRef = db.collection("notifications").document(notifId)
                batch.set(
                    notifRef,
                    mapPayloadFiltered(
                        "title" to "Package Activated: ${pkg.name}",
                        "message" to "Payment of ₹$finalAmount verified (Receipt $receiptNumber). Paid features are now unlocked.",
                        "type" to "PACKAGE",
                        "targetUserId" to uid,
                        "targetPackageId" to pkg.id,
                        "audience" to "STUDENT",
                        "createdByUid" to uid,
                        "createdAt" to serverTs()
                    )
                )
            }

            val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            batch.set(
                db.collection("audit_logs").document(logId),
                mapPayloadFiltered(
                    "actorUid" to uid,
                    "actorEmail" to profile.email.take(200),
                    "action" to "PAYMENT_${status.name}",
                    "entityType" to "PAYMENT",
                    "entityId" to payDocId,
                    "summary" to "Package ${pkg.name} | ₹$finalAmount | ${mode.name} | ${status.name}",
                    "createdAt" to serverTs()
                )
            )

            batch.commit().await()
            Result.success(payDocId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeAppointments(userId: String): Flow<List<CounsellingAppointment>> = observeListSafe("counselling_appointments") {
        val uid = requireUserId()
        check(uid == userId)
        db.collection("counselling_appointments")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(CounsellingAppointment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
    }

    suspend fun requestAppointment(profile: StudentProfile, topic: String, preferredDate: String, slot: String): Result<Unit> {
        val uid = requireUserId()
        val id = "appt_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "counselling_appointments/$id"
        return try {
            require(topic.trim().length >= 2) { "Please describe your counselling topic." }
            val payload = mapPayloadFiltered(
                "userId" to uid,
                "studentName" to profile.fullName.ifBlank { "Student" }.take(120),
                "mobile" to profile.mobile.take(20),
                "courseName" to profile.preferredCourseName.take(120),
                "topic" to topic.trim().take(200),
                "scheduledDate" to preferredDate.trim().take(40),
                "scheduledSlot" to slot.trim().take(60),
                "status" to CounsellingStatus.NEW.name,
                "createdAt" to serverTs(),
                "updatedAt" to serverTs()
            )
            db.collection("counselling_appointments").document(id).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> = observeListSafe("notifications") {
        val uid = requireUserId()
        check(uid == userId)
        db.collection("notifications")
            .whereIn("targetUserId", listOf(uid, "ALL"))
            .snapshots()
            .map { snap ->
                snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
    }

    fun observeReadNotificationIds(userId: String): Flow<Set<String>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            db.collection("users").document(uid).collection("notification_reads")
                .snapshots()
                .map { snap -> snap.documents.map { it.id }.toSet() }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    emit(emptySet())
                }
        )
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/notification_reads/$notificationId"
        return try {
            db.collection("users").document(uid).collection("notification_reads").document(notificationId)
                .set(
                    mapPayloadFiltered(
                        "userId" to uid,
                        "notificationId" to notificationId,
                        "readAt" to serverTs()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeSupportTickets(userId: String): Flow<List<SupportTicket>> = observeListSafe("support_tickets") {
        val uid = requireUserId()
        check(uid == userId)
        db.collection("support_tickets")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
    }

    suspend fun submitSupportTicket(profile: StudentProfile, subject: String, category: String, message: String): Result<Unit> {
        val uid = requireUserId()
        val id = "tkt_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "support_tickets/$id"
        return try {
            require(subject.trim().length >= 2 && message.trim().length >= 2) {
                "Subject and message must be at least 2 characters."
            }
            val payload = mapPayloadFiltered(
                "userId" to uid,
                "studentName" to profile.fullName.ifBlank { "Student" }.take(120),
                "subject" to subject.trim().take(160),
                "category" to category.trim().take(60),
                "message" to message.trim().take(1500),
                "status" to TicketStatus.OPEN.name,
                "createdAt" to serverTs(),
                "updatedAt" to serverTs()
            )
            db.collection("support_tickets").document(id).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteStudentAccountData(userId: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
