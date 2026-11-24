package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AdminRole
import com.example.data.model.AdminUser
import com.example.data.model.AppConfig
import com.example.data.model.AppNotification
import com.example.data.model.AuditLog
import com.example.data.model.College
import com.example.data.model.CollegeBranch
import com.example.data.model.CollegeType
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
import com.example.data.model.mapPayloadFiltered
import com.example.data.model.serverTs
import com.example.data.remote.CatalogSeedData
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

class AdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = "abhishekkhulkhule@gmail.com"
    }

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Admin must be signed in.")

    suspend fun resolveCurrentAdminAccess(): AdminUser? {
        val user = auth.currentUser ?: return null
        if (user.isEmailVerified && user.email.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)) {
            return AdminUser(
                id = user.uid,
                uid = user.uid,
                email = user.email.orEmpty(),
                displayName = user.displayName ?: "Super Admin",
                role = AdminRole.SUPER_ADMIN.name,
                permissions = listOf("ALL_ACCESS", "RBAC_MANAGE", "PAYMENTS_REFUND", "EXPORT_REPORTS"),
                active = true
            )
        }
        return try {
            val snap = db.collection("admins").document(user.uid).get().await()
            val admin = snap.toObject(AdminUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (admin != null && admin.active) admin else null
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun <T> observeAdminCollection(path: String, mapper: (com.google.firebase.firestore.QuerySnapshot) -> List<T>): Flow<List<T>> = flow {
        val uid = requireUid()
        emitAll(
            db.collection(path).snapshots().map(mapper).catch { error ->
                val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                throw error
            }
        )
    }

    fun observeAllStudents(): Flow<List<StudentProfile>> = observeAdminCollection("users") { snap ->
        snap.toObjects(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    fun observeAllCourses(): Flow<List<Course>> = observeAdminCollection("courses") { snap ->
        snap.toObjects(Course::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.displayOrder }
    }

    fun observeAllColleges(): Flow<List<College>> = observeAdminCollection("colleges") { snap ->
        snap.toObjects(College::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.name }
    }

    fun observeAllPackages(): Flow<List<CounsellingPackage>> = observeAdminCollection("packages") { snap ->
        snap.toObjects(CounsellingPackage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.displayOrder }
    }

    fun observeAllPromoCodes(): Flow<List<PromoCode>> = observeAdminCollection("promo_codes") { snap ->
        snap.toObjects(PromoCode::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }

    fun observeAllPayments(): Flow<List<PaymentRecord>> = observeAdminCollection("payments") { snap ->
        snap.toObjects(PaymentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    fun observeAllPreferences(): Flow<List<StudentPreferenceList>> = observeAdminCollection("student_preferences") { snap ->
        snap.toObjects(StudentPreferenceList::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.updatedAt?.seconds ?: 0L }
    }

    fun observeAllAppointments(): Flow<List<CounsellingAppointment>> = observeAdminCollection("counselling_appointments") { snap ->
        snap.toObjects(CounsellingAppointment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    fun observeAllTickets(): Flow<List<SupportTicket>> = observeAdminCollection("support_tickets") { snap ->
        snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    fun observeAuditLogs(): Flow<List<AuditLog>> = observeAdminCollection("audit_logs") { snap ->
        snap.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    fun observeStaffUsers(): Flow<List<AdminUser>> = observeAdminCollection("admins") { snap ->
        snap.toObjects(AdminUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }

    private suspend fun writeAuditLog(action: String, entityType: String, entityId: String, summary: String) {
        val user = auth.currentUser ?: return
        val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        db.collection("audit_logs").document(logId).set(
            mapPayloadFiltered(
                "actorUid" to user.uid,
                "actorEmail" to (user.email ?: "").take(200),
                "action" to action.take(80),
                "entityType" to entityType.take(60),
                "entityId" to entityId.take(128),
                "summary" to summary.take(500),
                "createdAt" to serverTs()
            )
        ).await()
    }

    suspend fun seedStarterCatalog(): Result<Unit> {
        return try {
            val uid = requireUid()
            val existingCourses = db.collection("courses").get().await().documents.associateBy { it.id }
            val existingColleges = db.collection("colleges").get().await().documents.associateBy { it.id }
            val existingPackages = db.collection("packages").get().await().documents.associateBy { it.id }
            val existingPromos = db.collection("promo_codes").get().await().documents.associateBy { it.id }
            val existingNotifs = db.collection("notifications").get().await().documents.map { it.id }.toSet()

            val batch = db.batch()

            CatalogSeedData.starterCourses.forEach { course ->
                val ref = db.collection("courses").document(course.id)
                val existing = existingCourses[course.id]
                val map = mapPayloadFiltered(
                    "name" to course.name,
                    "code" to course.code,
                    "description" to course.description,
                    "eligibility" to course.eligibility,
                    "duration" to course.duration,
                    "entranceExam" to course.entranceExam,
                    "active" to course.active,
                    "displayOrder" to course.displayOrder,
                    "createdAt" to (existing?.getTimestamp("createdAt") ?: serverTs()),
                    "updatedAt" to serverTs()
                )
                if (existing == null) batch.set(ref, map) else batch.update(ref, map)
            }

            CatalogSeedData.starterColleges.forEach { col ->
                val ref = db.collection("colleges").document(col.id)
                val existing = existingColleges[col.id]
                val map = collegeToCreateMap(col).toMutableMap()
                if (existing == null) {
                    batch.set(ref, map)
                } else {
                    map.remove("createdAt")
                    batch.update(ref, map)
                }
            }

            CatalogSeedData.starterPackages.forEach { pkg ->
                val ref = db.collection("packages").document(pkg.id)
                val existing = existingPackages[pkg.id]
                val map = packageToCreateMap(pkg).toMutableMap()
                if (existing == null) {
                    batch.set(ref, map)
                } else {
                    map.remove("createdAt")
                    batch.update(ref, map)
                }
            }

            CatalogSeedData.starterPromoCodes.forEach { promo ->
                val docId = "promo_${promo.code.lowercase()}"
                val ref = db.collection("promo_codes").document(docId)
                val existing = existingPromos[docId]
                val map = mapPayloadFiltered(
                    "code" to promo.code,
                    "discountType" to promo.discountType,
                    "discountValue" to promo.discountValue,
                    "minOrderAmount" to promo.minOrderAmount,
                    "maxUsage" to promo.maxUsage,
                    "usedCount" to promo.usedCount,
                    "active" to promo.active,
                    "expiryEpochMs" to promo.expiryEpochMs,
                    "createdAt" to (existing?.getTimestamp("createdAt") ?: serverTs()),
                    "updatedAt" to serverTs()
                )
                if (existing == null) batch.set(ref, map) else batch.update(ref, map)
            }

            CatalogSeedData.starterNotifications.forEach { n ->
                if (n.id !in existingNotifs) {
                    batch.set(
                        db.collection("notifications").document(n.id),
                        mapPayloadFiltered(
                            "title" to n.title,
                            "message" to n.message,
                            "type" to n.type,
                            "targetUserId" to "ALL",
                            "audience" to "ALL",
                            "createdByUid" to uid,
                            "createdAt" to serverTs()
                        )
                    )
                }
            }

            val cfg = CatalogSeedData.defaultAppConfig
            batch.set(
                db.collection("app_config").document("settings"),
                mapPayloadFiltered(
                    "whatsappNumber" to cfg.whatsappNumber,
                    "helplinePhone" to cfg.helplinePhone,
                    "supportEmail" to cfg.supportEmail,
                    "academicYear" to cfg.academicYear,
                    "paymentMode" to cfg.paymentMode,
                    "backendUrl" to cfg.backendUrl,
                    "announcementBanner" to cfg.announcementBanner,
                    "updatedAt" to serverTs()
                )
            )

            batch.commit().await()
            writeAuditLog("SEED_CATALOG", "SYSTEM", "catalog_v1", "Published starter courses, colleges, packages & promos")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "seed_catalog")
            Result.failure(e)
        }
    }

    suspend fun saveCourse(course: Course): Result<Unit> {
        val docId = course.id.ifBlank { "course_${UUID.randomUUID().toString().replace("-", "").take(10)}" }
        return try {
            requireUid()
            val ref = db.collection("courses").document(docId)
            val exists = ref.get().await().exists()
            if (!exists) {
                ref.set(
                    mapPayloadFiltered(
                        "name" to course.name.trim().take(120),
                        "code" to course.code.trim().uppercase().take(40),
                        "description" to course.description.trim().take(1000),
                        "eligibility" to course.eligibility.trim().take(500),
                        "duration" to course.duration.trim().take(60),
                        "entranceExam" to course.entranceExam.trim().take(120),
                        "active" to course.active,
                        "displayOrder" to course.displayOrder,
                        "createdAt" to serverTs(),
                        "updatedAt" to serverTs()
                    )
                ).await()
            } else {
                ref.update(
                    mapPayloadFiltered(
                        "name" to course.name.trim().take(120),
                        "code" to course.code.trim().uppercase().take(40),
                        "description" to course.description.trim().take(1000),
                        "eligibility" to course.eligibility.trim().take(500),
                        "duration" to course.duration.trim().take(60),
                        "entranceExam" to course.entranceExam.trim().take(120),
                        "active" to course.active,
                        "displayOrder" to course.displayOrder,
                        "updatedAt" to serverTs()
                    )
                ).await()
            }
            writeAuditLog("SAVE_COURSE", "COURSE", docId, "Saved course ${course.name}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "courses/$docId")
            Result.failure(e)
        }
    }

    suspend fun saveCollege(college: College): Result<Unit> {
        val docId = college.id.ifBlank { "col_${UUID.randomUUID().toString().replace("-", "").take(10)}" }
        return try {
            requireUid()
            val ref = db.collection("colleges").document(docId)
            val exists = ref.get().await().exists()
            if (!exists) {
                ref.set(collegeToCreateMap(college)).await()
            } else {
                val map = collegeToCreateMap(college).toMutableMap()
                map.remove("createdAt")
                ref.update(map).await()
            }
            writeAuditLog("SAVE_COLLEGE", "COLLEGE", docId, "Saved college ${college.name}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "colleges/$docId")
            Result.failure(e)
        }
    }

    suspend fun importCollegesFromCsv(csvText: String): Result<Int> {
        return try {
            requireUid()
            val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isEmpty()) return Result.success(0)
            val existingIds = db.collection("colleges").get().await().documents.map { it.id }.toSet()
            val batch = db.batch()
            var count = 0
            for (line in lines) {
                if (line.lowercase().startsWith("code,") || line.lowercase().startsWith("name,")) continue
                val parts = line.split(",").map { it.trim() }
                if (parts.size < 5) continue
                val code = parts[0].ifBlank { "C$count" }.take(20)
                val name = parts[1].take(200)
                val city = parts[2].take(80)
                val type = CollegeType.safeValueOf(parts[3].uppercase()).name
                val fee = parts[4].toLongOrNull() ?: 95000L
                val branchName = parts.getOrNull(5)?.take(100) ?: "Computer Engineering"
                val courseId = parts.getOrNull(6)?.take(80) ?: "course_btech"
                val docId = "col_csv_${code.lowercase().replace(Regex("[^a-z0-9]"), "")}"
                val col = College(
                    id = docId,
                    name = name,
                    code = code,
                    university = "State University",
                    city = city,
                    district = city,
                    state = "Maharashtra",
                    collegeType = type,
                    courseIds = listOf(courseId),
                    branches = listOf(CollegeBranch("${code}10", branchName, courseId, 60, 10, fee, 5000, 3000, 0, fee + 8000)),
                    cutoffs = emptyList(),
                    hostelAvailable = false,
                    active = true
                )
                val ref = db.collection("colleges").document(docId)
                val map = collegeToCreateMap(col).toMutableMap()
                if (docId in existingIds) {
                    map.remove("createdAt")
                    batch.update(ref, map)
                } else {
                    batch.set(ref, map)
                }
                count++
            }
            if (count > 0) {
                batch.commit().await()
                writeAuditLog("CSV_IMPORT_COLLEGES", "COLLEGE", "bulk", "Imported $count colleges via CSV")
            }
            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "colleges")
            Result.failure(e)
        }
    }

    suspend fun savePackage(pkg: CounsellingPackage): Result<Unit> {
        val docId = pkg.id.ifBlank { "pkg_${UUID.randomUUID().toString().replace("-", "").take(10)}" }
        return try {
            requireUid()
            val ref = db.collection("packages").document(docId)
            val exists = ref.get().await().exists()
            if (!exists) {
                ref.set(packageToCreateMap(pkg)).await()
            } else {
                val map = packageToCreateMap(pkg).toMutableMap()
                map.remove("createdAt")
                ref.update(map).await()
            }
            writeAuditLog("SAVE_PACKAGE", "PACKAGE", docId, "Saved package ${pkg.name} (₹${pkg.finalPrice})")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "packages/$docId")
            Result.failure(e)
        }
    }

    suspend fun savePromoCode(code: String, discountType: DiscountType, value: Long, minOrder: Long, maxUsage: Long): Result<Unit> {
        val clean = code.trim().uppercase()
        val docId = "promo_${clean.lowercase()}"
        return try {
            requireUid()
            require(clean.length in 2..40) { "Promo code must be 2-40 characters." }
            val ref = db.collection("promo_codes").document(docId)
            val exists = ref.get().await().exists()
            val expiry = System.currentTimeMillis() + 180L * 24 * 60 * 60 * 1000
            if (!exists) {
                ref.set(
                    mapPayloadFiltered(
                        "code" to clean,
                        "discountType" to discountType.name,
                        "discountValue" to value.coerceAtLeast(1L),
                        "minOrderAmount" to minOrder.coerceAtLeast(0L),
                        "maxUsage" to maxUsage.coerceAtLeast(1L),
                        "usedCount" to 0L,
                        "active" to true,
                        "expiryEpochMs" to expiry,
                        "createdAt" to serverTs(),
                        "updatedAt" to serverTs()
                    )
                ).await()
            } else {
                ref.update(
                    mapPayloadFiltered(
                        "discountType" to discountType.name,
                        "discountValue" to value.coerceAtLeast(1L),
                        "minOrderAmount" to minOrder.coerceAtLeast(0L),
                        "maxUsage" to maxUsage.coerceAtLeast(1L),
                        "active" to true,
                        "updatedAt" to serverTs()
                    )
                ).await()
            }
            writeAuditLog("SAVE_PROMO", "PROMO", docId, "Configured promo $clean")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "promo_codes/$docId")
            Result.failure(e)
        }
    }

    suspend fun updateStudentAdminFields(
        studentUid: String,
        blocked: Boolean,
        counsellingStatus: CounsellingStatus,
        assignedCounsellorName: String,
        internalNotes: String
    ): Result<Unit> {
        return try {
            requireUid()
            db.collection("users").document(studentUid).update(
                mapPayloadFiltered(
                    "blocked" to blocked,
                    "counsellingStatus" to counsellingStatus.name,
                    "assignedCounsellorName" to assignedCounsellorName.take(120),
                    "internalNotes" to internalNotes.take(1000),
                    "updatedAt" to serverTs()
                )
            ).await()
            writeAuditLog("UPDATE_STUDENT", "USER", studentUid, "Updated status=${counsellingStatus.name}, blocked=$blocked")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$studentUid")
            Result.failure(e)
        }
    }

    suspend fun updatePreferenceReview(
        prefUserId: String,
        status: String,
        counsellingStatus: String,
        assignedCounsellorName: String,
        adminNotes: String
    ): Result<Unit> {
        return try {
            requireUid()
            db.collection("student_preferences").document(prefUserId).update(
                mapPayloadFiltered(
                    "status" to status,
                    "counsellingStatus" to counsellingStatus,
                    "assignedCounsellorName" to assignedCounsellorName.take(120),
                    "adminNotes" to adminNotes.take(1000),
                    "updatedAt" to serverTs()
                )
            ).await()
            writeAuditLog("REVIEW_PREFERENCES", "PREFERENCE_LIST", prefUserId, "Updated preference status=$status")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "student_preferences/$prefUserId")
            Result.failure(e)
        }
    }

    suspend fun processRefund(paymentId: String, reason: String): Result<Unit> {
        return try {
            requireUid()
            db.collection("payments").document(paymentId).update(
                mapPayloadFiltered(
                    "status" to PaymentStatus.REFUNDED.name,
                    "failureReason" to reason.ifBlank { "Refund processed by admin" }.take(300),
                    "updatedAt" to serverTs()
                )
            ).await()
            writeAuditLog("REFUND_PAYMENT", "PAYMENT", paymentId, "Marked payment refunded: $reason")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "payments/$paymentId")
            Result.failure(e)
        }
    }

    suspend fun updateAppointment(
        appointmentId: String,
        status: CounsellingStatus,
        counsellorName: String,
        followUpDate: String,
        notes: String
    ): Result<Unit> {
        return try {
            requireUid()
            db.collection("counselling_appointments").document(appointmentId).update(
                mapPayloadFiltered(
                    "status" to status.name,
                    "counsellorName" to counsellorName.take(120),
                    "followUpDate" to followUpDate.take(40),
                    "notes" to notes.take(1000),
                    "updatedAt" to serverTs()
                )
            ).await()
            writeAuditLog("UPDATE_APPOINTMENT", "APPOINTMENT", appointmentId, "Status=${status.name}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "counselling_appointments/$appointmentId")
            Result.failure(e)
        }
    }

    suspend fun sendNotification(
        title: String,
        message: String,
        type: String,
        audience: String,
        targetUserId: String
    ): Result<Unit> {
        val notifId = "notif_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        return try {
            val uid = requireUid()
            db.collection("notifications").document(notifId).set(
                mapPayloadFiltered(
                    "title" to title.trim().take(160),
                    "message" to message.trim().take(1000),
                    "type" to type,
                    "targetUserId" to targetUserId.ifBlank { "ALL" },
                    "audience" to audience,
                    "createdByUid" to uid,
                    "createdAt" to serverTs()
                )
            ).await()
            writeAuditLog("SEND_NOTIFICATION", "NOTIFICATION", notifId, "Sent $type alert to $audience")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "notifications/$notifId")
            Result.failure(e)
        }
    }

    suspend fun saveStaffRole(uid: String, email: String, displayName: String, role: AdminRole, active: Boolean): Result<Unit> {
        return try {
            requireUid()
            require(uid.isNotBlank()) { "Staff Firebase UID required." }
            val perms = when (role) {
                AdminRole.SUPER_ADMIN -> listOf("ALL_ACCESS", "RBAC_MANAGE", "PAYMENTS_REFUND", "EXPORT_REPORTS")
                AdminRole.ADMIN -> listOf("MANAGE_STUDENTS", "MANAGE_CATALOG", "VIEW_PAYMENTS", "SEND_ALERTS")
                AdminRole.COUNSELLOR -> listOf("VIEW_PREFERENCES", "MANAGE_COUNSELLING")
                AdminRole.DATA_MANAGER -> listOf("MANAGE_CATALOG", "EXPORT_REPORTS")
            }
            db.collection("admins").document(uid.trim()).set(
                mapPayloadFiltered(
                    "uid" to uid.trim(),
                    "email" to email.trim(),
                    "displayName" to displayName.trim(),
                    "role" to role.name,
                    "permissions" to perms,
                    "active" to active,
                    "createdAt" to serverTs(),
                    "updatedAt" to serverTs()
                )
            ).await()
            writeAuditLog("SAVE_STAFF_ROLE", "ADMIN_USER", uid.trim(), "Role=${role.name}, active=$active")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "admins/$uid")
            Result.failure(e)
        }
    }

    suspend fun saveAppConfig(config: AppConfig): Result<Unit> {
        return try {
            requireUid()
            db.collection("app_config").document("settings").set(
                mapPayloadFiltered(
                    "whatsappNumber" to config.whatsappNumber.take(30),
                    "helplinePhone" to config.helplinePhone.take(30),
                    "supportEmail" to config.supportEmail.take(160),
                    "academicYear" to config.academicYear.take(30),
                    "paymentMode" to config.paymentMode,
                    "backendUrl" to config.backendUrl.take(300),
                    "announcementBanner" to config.announcementBanner.take(400),
                    "updatedAt" to serverTs()
                )
            ).await()
            writeAuditLog("UPDATE_CONFIG", "APP_CONFIG", "settings", "Updated mode=${config.paymentMode}, year=${config.academicYear}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "app_config/settings")
            Result.failure(e)
        }
    }

    private fun collegeToCreateMap(col: College): Map<String, Any> {
        val branchMaps = col.branches.take(30).map { b ->
            mapOf(
                "branchCode" to b.branchCode,
                "branchName" to b.branchName,
                "courseId" to b.courseId,
                "intake" to b.intake,
                "seatsAvailable" to b.seatsAvailable,
                "tuitionFee" to b.tuitionFee,
                "devFee" to b.devFee,
                "otherFee" to b.otherFee,
                "hostelFee" to b.hostelFee,
                "totalFee" to b.totalFee
            )
        }
        val cutoffMaps = col.cutoffs.take(80).map { c ->
            mapPayloadFiltered(
                "year" to c.year,
                "capRound" to c.capRound,
                "category" to c.category,
                "quota" to c.quota,
                "courseId" to c.courseId,
                "branchName" to c.branchName,
                "percentile" to c.percentile,
                "score" to c.score,
                "rank" to c.rank
            )
        }
        return mapPayloadFiltered(
            "name" to col.name.take(200),
            "code" to col.code.take(40),
            "university" to col.university.take(160),
            "address" to col.address.take(300),
            "city" to col.city.take(80),
            "district" to col.district.take(80),
            "state" to col.state.take(80),
            "collegeType" to col.collegeType,
            "courseIds" to col.courseIds.take(20),
            "branches" to branchMaps,
            "cutoffs" to cutoffMaps,
            "scholarships" to col.scholarships.take(500),
            "eligibility" to col.eligibility.take(500),
            "placementAvgLpa" to col.placementAvgLpa,
            "placementHighLpa" to col.placementHighLpa,
            "placementRate" to col.placementRate,
            "hostelAvailable" to col.hostelAvailable,
            "hostelSummary" to col.hostelSummary.take(400),
            "website" to col.website.take(250),
            "contactEmail" to col.contactEmail.take(160),
            "contactPhone" to col.contactPhone.take(40),
            "description" to col.description.take(1500),
            "imageKey" to col.imageKey.take(80),
            "active" to col.active,
            "createdAt" to serverTs(),
            "updatedAt" to serverTs()
        )
    }

    private fun packageToCreateMap(pkg: CounsellingPackage): Map<String, Any> {
        return mapPayloadFiltered(
            "name" to pkg.name.take(120),
            "description" to pkg.description.take(1000),
            "tier" to pkg.tier,
            "courseIds" to pkg.courseIds.take(20),
            "originalPrice" to pkg.originalPrice.coerceAtLeast(0L),
            "discountType" to pkg.discountType,
            "discountValue" to pkg.discountValue.coerceAtLeast(0L),
            "finalPrice" to pkg.finalPrice.coerceIn(0L, pkg.originalPrice.coerceAtLeast(0L)),
            "features" to pkg.features.take(20),
            "validityDays" to pkg.validityDays.coerceIn(1L, 3650L),
            "displayOrder" to pkg.displayOrder,
            "active" to pkg.active,
            "combo" to pkg.combo,
            "includedPackageIds" to pkg.includedPackageIds.take(10),
            "promoCode" to pkg.promoCode.take(40),
            "maxUsage" to pkg.maxUsage,
            "usedCount" to pkg.usedCount,
            "validUntilEpochMs" to pkg.validUntilEpochMs,
            "createdAt" to serverTs(),
            "updatedAt" to serverTs()
        )
    }
}
