package com.cjlu.studentapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cjlu.studentapp.data.MessagesRepository
import com.cjlu.studentapp.data.RequestManager
import com.cjlu.studentapp.network.AuthTokenStore
import com.cjlu.studentapp.network.RetrofitClient
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NetworkScenarioInstrumentedTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        RetrofitClient.setTestServer(server.url("/").toString(), RetrofitClient.testClient())
        AuthTokenStore.accessToken = "test-token"
    }

    @After
    fun tearDown() {
        AuthTokenStore.accessToken = null
        RetrofitClient.clearTestServer()
        server.shutdown()
    }

    @Test
    fun backendDown_fallsBackToLocalData() = runBlocking {
        server.shutdown()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val (_, requestsOnline) = RequestManager.syncRequests(context, "20230937")
        val (_, messagesOnline) = MessagesRepository.syncMessages(context, "20230937")
        assertFalse(requestsOnline)
        assertFalse(messagesOnline)
    }

    @Test
    fun syncRecovery_recoversWhenServerComesBack() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val first = RequestManager.syncRequests(context, "20230937")
        assertTrue(first.second)
        val second = MessagesRepository.syncMessages(context, "20230937")
        assertTrue(second.second)
    }
}
