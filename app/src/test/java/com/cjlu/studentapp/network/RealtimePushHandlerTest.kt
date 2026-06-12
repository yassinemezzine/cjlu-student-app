package com.cjlu.studentapp.network

import com.cjlu.studentapp.data.RequestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimePushHandlerTest {

    @Test
    fun parseRefreshToken() {
        assertEquals(RealtimePushAction.RefreshRequests, RealtimePushHandler.parse("REFRESH"))
    }

    @Test
    fun parseTypedMessages() {
        assertEquals(RealtimePushAction.RefreshMessages, RealtimePushHandler.parse("""{"type":"messages"}"""))
    }

    @Test
    fun parseRequestUpdate() {
        val json = """
            {
              "id": "req-1",
              "serviceId": "leave",
              "studentId": "20230901",
              "contactInfo": "a@b.c",
              "notes": "note",
              "status": "in_review",
              "createdAtMillis": 1000
            }
        """.trimIndent()
        val action = RealtimePushHandler.parse(json)
        assertTrue(action is RealtimePushAction.RequestUpdated)
        val updated = action as RealtimePushAction.RequestUpdated
        assertEquals("req-1", updated.request.id)
        assertEquals(RequestStatus.InReview, updated.request.status)
    }

    @Test
    fun parseUnknownPayload_returnsNull() {
        assertNull(RealtimePushHandler.parse("not-json"))
    }
}
