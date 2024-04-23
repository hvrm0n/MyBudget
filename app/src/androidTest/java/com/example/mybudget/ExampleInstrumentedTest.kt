package com.example.mybudget

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudget.drawersection.HomePageActivity
import junit.framework.AssertionFailedError
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeoutException

/*
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @get:Rule
    val activityTestRule =  ActivityScenarioRule(HomePageActivity::class.java)
   *//* @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.mybudget", appContext.packageName)
    }
*//*
    @Test
    fun addSameBudget() {
       onView(withId(R.id.budgetsList)).waitUntilVisible(10000).perform( RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))
       onView(withId(R.id.nameBudgetNew)).waitUntilVisible(10000).perform(click()).perform(typeText("Тинькофф"))
       val amount = onView(withId(R.id.amountNew))
       amount.perform(click()).perform(typeText("1000"))
       onView(withText("Добавить"))
           .inRoot(isDialog())
           .check(matches(isDisplayed()))
           .perform(click())
       onView(withText("Счет с таким названием уже существует!"))
    }
}

fun ViewInteraction.waitUntilVisible(timeout: Long): ViewInteraction {
    val startTime = System.currentTimeMillis()
    val endTime = startTime + timeout

    do {
        try {
            check(matches(isDisplayed()))
            return this
        } catch (e: AssertionFailedError) {
            Thread.sleep(50)
        }
    } while (System.currentTimeMillis() < endTime)

    throw TimeoutException()
}*/
