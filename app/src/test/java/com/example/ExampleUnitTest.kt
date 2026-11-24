package com.example

import com.example.data.model.CollegeCutoff
import com.example.data.model.CounsellingPackage
import com.example.data.model.DiscountType
import com.example.data.model.PackageTier
import com.example.data.model.PromoCode
import com.example.data.model.StudentPackage
import com.example.data.model.StudentProfile
import com.example.domain.CounselingEngine
import com.example.domain.GuidanceTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun `package pricing applies percentage discount and promo code safely`() {
    val pkg = CounsellingPackage(
      id = "pkg_premium",
      name = "Premium CAP Guide",
      tier = PackageTier.PREMIUM.name,
      originalPrice = 5000L,
      discountType = DiscountType.PERCENT.name,
      discountValue = 20L
    )
    val promo = PromoCode(
      id = "promo_merit500",
      code = "MERIT500",
      discountType = DiscountType.FIXED.name,
      discountValue = 500L,
      minOrderAmount = 2000L,
      maxUsage = 100L,
      usedCount = 5L,
      active = true
    )

    val breakdown = CounselingEngine.calculatePackagePricing(pkg, promo, nowMs = 1000L)
    assertEquals(5000L, breakdown.originalPrice)
    assertEquals(1000L, breakdown.packageDiscount)
    assertEquals(500L, breakdown.promoDiscount)
    assertEquals(1500L, breakdown.totalDiscount)
    assertEquals(3500L, breakdown.finalPayable)
    assertEquals("MERIT500", breakdown.appliedPromoCode)
    assertNull(breakdown.promoError)
  }

  @Test
  fun `package pricing clamps excessive discount to zero floor`() {
    val pkg = CounsellingPackage(
      originalPrice = 1000L,
      discountType = DiscountType.FIXED.name,
      discountValue = 1500L
    )
    val breakdown = CounselingEngine.calculatePackagePricing(pkg)
    assertEquals(1000L, breakdown.packageDiscount)
    assertEquals(0L, breakdown.finalPayable)
  }

  @Test
  fun `expired promo code reports error and ignores discount`() {
    val pkg = CounsellingPackage(
      originalPrice = 3000L,
      discountType = DiscountType.NONE.name
    )
    val expired = PromoCode(
      code = "OLD2025",
      discountType = DiscountType.FIXED.name,
      discountValue = 500L,
      active = true,
      expiryEpochMs = 500L
    )
    val breakdown = CounselingEngine.calculatePackagePricing(pkg, expired, nowMs = 1000L)
    assertEquals(3000L, breakdown.finalPayable)
    assertEquals(0L, breakdown.promoDiscount)
    assertEquals("Promo code has expired.", breakdown.promoError)
  }

  @Test
  fun `cutoff guidance distinguishes safe moderate ambitious and missing data`() {
    val cutoff = CollegeCutoff(percentile = 95.0, rank = 5000L)
    assertEquals(GuidanceTier.SAFE, CounselingEngine.evaluateCutoffGuidance(97.0, 0L, cutoff))
    assertEquals(GuidanceTier.MODERATE, CounselingEngine.evaluateCutoffGuidance(94.0, 0L, cutoff))
    assertEquals(GuidanceTier.AMBITIOUS, CounselingEngine.evaluateCutoffGuidance(89.0, 0L, cutoff))
    assertEquals(GuidanceTier.DATA_UNAVAILABLE, CounselingEngine.evaluateCutoffGuidance(97.0, 0L, null))
    assertEquals(
      GuidanceTier.DATA_UNAVAILABLE,
      CounselingEngine.evaluateCutoffGuidance(97.0, 0L, CollegeCutoff(percentile = null, rank = null))
    )
  }

  @Test
  fun `student package entitlement respects expiry and tier hierarchy`() {
    val now = 10_000L
    val expiredPremium = StudentPackage(
      tier = PackageTier.PREMIUM.name,
      active = true,
      expiresAtEpochMs = 5_000L
    )
    val activeStandard = StudentPackage(
      tier = PackageTier.STANDARD.name,
      active = true,
      expiresAtEpochMs = 20_000L
    )
    val packages = listOf(expiredPremium, activeStandard)
    assertEquals(PackageTier.STANDARD, CounselingEngine.highestActiveTier(packages, now))
    assertTrue(CounselingEngine.hasStandardOrHigher(packages, now))
    assertFalse(CounselingEngine.hasPremiumOrHigher(packages, now))
  }

  @Test
  fun `student profile completion computes accurate percentage`() {
    val empty = StudentProfile()
    assertEquals(6, empty.computeCompletionPercent()) // default category = "OPEN" counts as 1/15 -> 6%

    val complete = StudentProfile(
      fullName = "Aarav Kulkarni",
      email = "aarav@example.com",
      mobile = "9876543210",
      dob = "2008-04-12",
      state = "Maharashtra",
      district = "Pune",
      domicile = "Maharashtra",
      preferredCourseId = "course_btech",
      entranceExam = "MHT-CET",
      percentile = 96.4,
      category = "OPEN",
      budgetMax = 150000L,
      preferredLocations = "Pune, Mumbai",
      tenthPercent = 92.0,
      twelfthPercent = 88.5
    )
    assertEquals(100, complete.computeCompletionPercent())
  }
}
