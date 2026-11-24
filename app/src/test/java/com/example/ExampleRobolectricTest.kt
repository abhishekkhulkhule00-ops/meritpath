package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.core.localization.AppLanguage
import com.example.core.localization.StringsProvider
import com.example.data.model.CounsellingPackage
import com.example.data.model.DiscountType
import com.example.data.model.PackageTier
import com.example.ui.components.PackageCard
import com.example.ui.screens.OnboardingAndAuthScreen
import com.example.ui.theme.MeritPathTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    val dbId = context.getString(R.string.firestore_database_id)
    assertEquals("MeritPath", appName)
    assertTrue(dbId.isNotBlank())
  }

  @Test
  fun `onboarding screen displays google sign in and toggles marathi language`() {
    composeTestRule.setContent {
      var lang by remember { mutableStateOf(AppLanguage.ENGLISH) }
      MeritPathTheme {
        OnboardingAndAuthScreen(
          language = lang,
          strings = StringsProvider.get(lang),
          onToggleLanguage = {
            lang = if (lang == AppLanguage.ENGLISH) AppLanguage.MARATHI else AppLanguage.ENGLISH
          },
          onAuthSuccess = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Continue with Google").assertIsDisplayed()

    composeTestRule.onNodeWithTag("auth_language_toggle").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Google द्वारे सुरू करा").assertIsDisplayed()
  }

  @Test
  fun `package card displays discounted payable amount and action tag`() {
    val pkg = CounsellingPackage(
      id = "pkg_test_combo",
      name = "B.Tech + Pharmacy Combo",
      description = "Dual-course admission counselling support",
      tier = PackageTier.COMBO.name,
      originalPrice = 4000L,
      discountType = DiscountType.PERCENT.name,
      discountValue = 25L,
      features = listOf("College Explorer", "Private Preference List"),
      combo = true
    )

    composeTestRule.setContent {
      MeritPathTheme {
        PackageCard(
          pkg = pkg,
          owned = false,
          strings = StringsProvider.get(AppLanguage.ENGLISH),
          onSelect = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("B.Tech + Pharmacy Combo").assertIsDisplayed()
    composeTestRule.onNodeWithText("₹3000").assertIsDisplayed()
    composeTestRule.onNodeWithTag("select_pkg_pkg_test_combo")
      .assertIsDisplayed()
      .assertTextContains("₹3000", substring = true)
  }
}
