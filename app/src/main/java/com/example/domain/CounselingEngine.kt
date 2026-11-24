package com.example.domain

import com.example.data.model.CollegeCutoff
import com.example.data.model.CounsellingPackage
import com.example.data.model.DiscountType
import com.example.data.model.PackageTier
import com.example.data.model.PromoCode
import com.example.data.model.StudentPackage
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.roundToLong

enum class GuidanceTier {
    SAFE, MODERATE, AMBITIOUS, DATA_UNAVAILABLE
}

data class PricingBreakdown(
    val originalPrice: Long,
    val packageDiscount: Long,
    val promoDiscount: Long,
    val totalDiscount: Long,
    val finalPayable: Long,
    val appliedPromoCode: String?,
    val promoError: String? = null
)

object CounselingEngine {

    /**
     * Computes deterministic package & promo pricing.
     * Prevents negative totals and invalid discounts exceeding originalPrice.
     */
    fun calculatePackagePricing(
        pkg: CounsellingPackage,
        promo: PromoCode? = null,
        nowMs: Long = System.currentTimeMillis()
    ): PricingBreakdown {
        val original = max(0L, pkg.originalPrice)
        val rawPkgDiscount = when (pkg.discountTypeEnum()) {
            DiscountType.NONE -> 0L
            DiscountType.FIXED -> max(0L, pkg.discountValue)
            DiscountType.PERCENT -> ((original * pkg.discountValue.coerceIn(0L, 100L)) / 100.0).roundToLong()
        }
        val clampedPkgDiscount = rawPkgDiscount.coerceIn(0L, original)
        val afterPkgPrice = max(0L, original - clampedPkgDiscount)

        var promoDiscount = 0L
        var appliedCode: String? = null
        var promoError: String? = null

        if (promo != null) {
            when {
                !promo.active -> promoError = "Promo code is inactive."
                promo.expiryEpochMs in 1 until nowMs -> promoError = "Promo code has expired."
                promo.usedCount >= promo.maxUsage -> promoError = "Promo code usage limit reached."
                afterPkgPrice < promo.minOrderAmount -> promoError = "Minimum order ₹${promo.minOrderAmount} required."
                else -> {
                    val rawPromo = when (DiscountType.safeValueOf(promo.discountType)) {
                        DiscountType.FIXED -> max(0L, promo.discountValue)
                        DiscountType.PERCENT -> ((afterPkgPrice * promo.discountValue.coerceIn(0L, 100L)) / 100.0).roundToLong()
                        DiscountType.NONE -> 0L
                    }
                    promoDiscount = rawPromo.coerceIn(0L, afterPkgPrice)
                    appliedCode = promo.code
                }
            }
        }

        val finalPrice = max(0L, afterPkgPrice - promoDiscount)
        val totalDiscount = (original - finalPrice).coerceIn(0L, original)
        return PricingBreakdown(
            originalPrice = original,
            packageDiscount = clampedPkgDiscount,
            promoDiscount = promoDiscount,
            totalDiscount = totalDiscount,
            finalPayable = finalPrice,
            appliedPromoCode = appliedCode,
            promoError = promoError
        )
    }

    /**
     * Compares student's verified percentile/rank against historical CAP cutoffs.
     * Never fabricates missing numbers: returns DATA_UNAVAILABLE when cutoff data is absent.
     */
    fun evaluateCutoffGuidance(
        studentPercentile: Double,
        studentRank: Long,
        cutoff: CollegeCutoff?
    ): GuidanceTier {
        if (cutoff == null) return GuidanceTier.DATA_UNAVAILABLE
        val cutoffPercentile = cutoff.percentile
        val cutoffRank = cutoff.rank

        if (cutoffPercentile != null && studentPercentile > 0.0) {
            val delta = studentPercentile - cutoffPercentile
            return when {
                delta >= 1.5 -> GuidanceTier.SAFE
                delta >= -2.5 -> GuidanceTier.MODERATE
                else -> GuidanceTier.AMBITIOUS
            }
        }

        if (cutoffRank != null && cutoffRank > 0L && studentRank > 0L) {
            val ratio = studentRank.toDouble() / cutoffRank.toDouble()
            return when {
                ratio <= 0.85 -> GuidanceTier.SAFE
                ratio <= 1.25 -> GuidanceTier.MODERATE
                else -> GuidanceTier.AMBITIOUS
            }
        }

        return GuidanceTier.DATA_UNAVAILABLE
    }

    fun highestActiveTier(packages: List<StudentPackage>, nowMs: Long = System.currentTimeMillis()): PackageTier? {
        return packages
            .filter { it.isUnexpired(nowMs) }
            .maxByOrNull { it.tierEnum().rank }
            ?.tierEnum()
    }

    fun hasStandardOrHigher(packages: List<StudentPackage>, nowMs: Long = System.currentTimeMillis()): Boolean {
        val tier = highestActiveTier(packages, nowMs) ?: return false
        return tier.rank >= PackageTier.STANDARD.rank
    }

    fun hasPremiumOrHigher(packages: List<StudentPackage>, nowMs: Long = System.currentTimeMillis()): Boolean {
        val tier = highestActiveTier(packages, nowMs) ?: return false
        return tier.rank >= PackageTier.PREMIUM.rank
    }

    fun generateSandboxSignature(orderId: String, paymentId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$orderId|$paymentId|SANDBOX_VERIFIED".toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(32)
    }

    fun generateReceiptNumber(nowMs: Long = System.currentTimeMillis()): String {
        val tail = (nowMs % 1000000L).toString().padStart(6, '0')
        return "MP-RCPT-$tail"
    }
}
