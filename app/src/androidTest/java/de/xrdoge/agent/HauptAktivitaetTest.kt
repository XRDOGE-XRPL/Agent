package de.xrdoge.agent

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.xrdoge.agent.ui.HauptAktivitaet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HauptAktivitaetTest {
    @get:Rule
    val regel = ActivityScenarioRule(HauptAktivitaet::class.java)

    @Test
    fun startknopfIstSichtbar() {
        onView(withId(R.id.knopfStart)).check(matches(isDisplayed()))
    }
}
