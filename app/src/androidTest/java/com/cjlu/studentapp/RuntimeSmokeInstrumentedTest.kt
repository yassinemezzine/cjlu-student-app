package com.cjlu.studentapp

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RuntimeSmokeInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearStoredSession() {
        composeRule.activity.getSharedPreferences("cjlu_auth", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        composeRule.activity.runOnUiThread {
            composeRule.activity.recreate()
        }
    }

    @Test
    fun appLaunchesToLoginWhenNoSessionExists() {
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.login_button))
            .assertIsDisplayed()
    }

    @Test
    fun loginScreenShowsDefaultStudentIdField() {
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.login_student_id_label))
            .assertIsDisplayed()
    }
}
