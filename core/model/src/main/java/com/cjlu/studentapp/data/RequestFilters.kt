package com.cjlu.studentapp.data

object RequestFilters {
    fun forService(requests: List<StudentRequest>, serviceId: String): List<StudentRequest> {
        return requests
            .filter { it.serviceId == serviceId }
            .sortedByDescending { it.createdAtMillis }
    }

    fun activeCount(requests: List<StudentRequest>, serviceId: String): Int {
        return requests.count { it.serviceId == serviceId && it.status != RequestStatus.Completed }
    }

    fun countByService(requests: List<StudentRequest>): Map<String, Int> {
        return requests
            .filter { it.status != RequestStatus.Completed }
            .groupingBy { it.serviceId }
            .eachCount()
    }

    fun servicesWithActiveRequests(requests: List<StudentRequest>): Set<String> {
        return requests
            .filter { it.status != RequestStatus.Completed }
            .map { it.serviceId }
            .toSet()
    }
}
