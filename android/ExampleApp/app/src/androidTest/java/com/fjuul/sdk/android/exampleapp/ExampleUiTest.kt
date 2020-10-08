package com.fjuul.sdk.android.exampleapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
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
}
