package com.fjuul.sdk.android.exampleapp

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.PatternMatcher
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.fjuul.sdk.android.exampleapp.R as appR

@RunWith(AndroidJUnit4::class)
@LargeTest
class ExampleUiTest {
    @get:Rule
    var activityRule = ActivityTestRule(
        MainActivity::class.java
    )

    @Test
    fun shouldStartFromOnboarding() {
        onView((withId(appR.id.toolbar))).check(matches(hasDescendant(withText("Onboarding"))))
    }

    @Test
    fun callbackFailureFeedback() {
        val fixtures = listOf(
            "&errorCode=oauth_cancelled" to "Connection Cancelled",
            "&errorCode=google_health_account_not_linked" to "Google Health Account Required",
            "&errorCode=future_code" to "Connection Failed",
            "&errorCode=" to "Connection Failed",
            "&unrelated=value" to "Connection Failed",
            "" to "Connection Failed",
        )
        for ((suffix, title) in fixtures) {
            deliverCallback("service=googlehealth&success=false$suffix")
            onView(withText(title)).inRoot(isDialog()).check(matches(withText(title)))
            if (title == "Google Health Account Required") {
                onView(withText("Account Setup")).inRoot(isDialog()).check(matches(withText("Account Setup")))
                onView(withText("Create a Google Health profile or migrate your Fitbit account, then return and retry the connection."))
                    .inRoot(isDialog())
                    .check(matches(withText("Create a Google Health profile or migrate your Fitbit account, then return and retry the connection.")))
            } else {
                onView(withText("Account Setup")).inRoot(isDialog()).check(doesNotExist())
            }
            onView(withText("OK")).inRoot(isDialog()).perform(click())
        }
    }

    @Test
    fun successIgnoresErrorCode() {
        deliverCallback("service=googlehealth&success=true&errorCode=google_health_account_not_linked")
        onView(withText("Google Health Account Required")).check(doesNotExist())
        onView(withText("Connection Failed")).check(doesNotExist())
        onView(withText("Connection Cancelled")).check(doesNotExist())
    }

    @Test
    fun coldStartCallbackFailure() {
        activityRule.finishActivity()
        activityRule.launchActivity(callbackIntent("service=googlehealth&success=false&errorCode=google_health_account_not_linked"))
        onView(withText("Google Health Account Required")).inRoot(isDialog()).check(matches(withText("Google Health Account Required")))
        onView(withText("Account Setup")).inRoot(isDialog()).check(matches(withText("Account Setup")))
    }

    @Test
    fun accountSetupOpensSignupUrl() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val filter = IntentFilter(Intent.ACTION_VIEW).apply {
            addDataScheme("https")
            addDataAuthority("fitbit.google.com", null)
            addDataPath("/auth/signup", PatternMatcher.PATTERN_LITERAL)
        }
        val monitor = instrumentation.addMonitor(filter, Instrumentation.ActivityResult(Activity.RESULT_OK, Intent()), true)
        try {
            deliverCallback("service=googlehealth&success=false&errorCode=google_health_account_not_linked")
            onView(withText("Account Setup")).inRoot(isDialog()).perform(click())
            assertEquals(1, monitor.hits)
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    private fun deliverCallback(query: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            instrumentation.callActivityOnNewIntent(activityRule.activity, callbackIntent(query))
        }
    }

    private fun callbackIntent(query: String): Intent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("fjuulsdk-exampleapp://external_connect?$query"),
            InstrumentationRegistry.getInstrumentation().targetContext,
            MainActivity::class.java,
        )
}
