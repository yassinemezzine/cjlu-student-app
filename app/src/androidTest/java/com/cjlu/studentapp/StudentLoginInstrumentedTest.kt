package com.cjlu.studentapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cjlu.studentapp.network.RetrofitClient
import com.cjlu.studentapp.network.api.LoginRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the debug APK can reach the host backend from the emulator (10.0.2.2:8080).
 * Requires backend running with dev API key; run: ./gradlew :backend-ktor:run
 */
@RunWith(AndroidJUnit4::class)
class StudentLoginInstrumentedTest {

    @Test
    fun login_student20230901_reachesBackend() = runBlocking {
        val response = RetrofitClient.instance.login(
            LoginRequest(studentId = "20230901", password = "20230901"),
        )
        assertEquals("20230901", response.profile.studentId)
        assertTrue(response.token.isNotBlank())
    }
}
