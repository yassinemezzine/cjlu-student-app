package com.cjlu.studentapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestFiltersTest {

    private fun request(
        id: String,
        serviceId: String,
        status: RequestStatus = RequestStatus.Submitted,
        createdAtMillis: Long = 0L,
    ) = StudentRequest(
        id = id,
        serviceId = serviceId,
        studentId = "20230901",
        contactInfo = "test@cjlu.edu.cn",
        notes = "",
        status = status,
        createdAtMillis = createdAtMillis,
    )

    @Test
    fun forService_sortsNewestFirst() {
        val requests = listOf(
            request("a", "leave", createdAtMillis = 100),
            request("b", "leave", createdAtMillis = 300),
            request("c", "transcripts", createdAtMillis = 200),
        )
        val filtered = RequestFilters.forService(requests, "leave")
        assertEquals(listOf("b", "a"), filtered.map { it.id })
    }

    @Test
    fun countByService_ignoresCompleted() {
        val requests = listOf(
            request("a", "leave", RequestStatus.Submitted),
            request("b", "leave", RequestStatus.Completed),
            request("c", "transcripts", RequestStatus.InReview),
        )
        assertEquals(mapOf("leave" to 1, "transcripts" to 1), RequestFilters.countByService(requests))
    }

    @Test
    fun servicesWithActiveRequests_returnsDistinctServiceIds() {
        val requests = listOf(
            request("a", "leave", RequestStatus.Submitted),
            request("b", "leave", RequestStatus.ActionNeeded),
            request("c", "transcripts", RequestStatus.Completed),
        )
        assertEquals(setOf("leave"), RequestFilters.servicesWithActiveRequests(requests))
        assertTrue(RequestFilters.activeCount(requests, "leave") == 2)
    }
}
