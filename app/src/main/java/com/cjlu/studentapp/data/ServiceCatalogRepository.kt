package com.cjlu.studentapp.data

import android.content.Context
import android.util.Log
import com.cjlu.studentapp.network.RetrofitClient
import com.cjlu.studentapp.network.api.CatalogServiceDto
import com.cjlu.studentapp.ui.screens.ServiceItem
import com.cjlu.studentapp.ui.screens.toServiceItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

object ServiceCatalogRepository {
    private const val PREFS_NAME = "service_catalog_prefs"
    private const val CACHE_KEY_SERVICES = "cached_services"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadServices(context: Context): List<ServiceItem> {
        return try {
            val fresh = RetrofitClient.instance.services()
            saveToCache(context, fresh)
            fresh.map { it.toServiceItem() }
        } catch (e: Exception) {
            Log.w("ServiceCatalogRepo", "Failed to load services from network, loading from cache", e)
            getCachedOrFallback(context)
        }
    }

    fun getCachedOrFallback(context: Context): List<ServiceItem> {
        val cached = loadFromCache(context)
        return if (cached.isNotEmpty()) {
            cached.map { it.toServiceItem() }
        } else {
            getFallbackServices().map { it.toServiceItem() }
        }
    }

    private fun saveToCache(context: Context, services: List<CatalogServiceDto>) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = json.encodeToString(ListSerializer(CatalogServiceDto.serializer()), services)
            prefs.edit().putString(CACHE_KEY_SERVICES, jsonStr).apply()
        } catch (e: Exception) {
            Log.e("ServiceCatalogRepo", "Failed to save services to cache", e)
        }
    }

    private fun loadFromCache(context: Context): List<CatalogServiceDto> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(CACHE_KEY_SERVICES, null) ?: return emptyList()
            json.decodeFromString(ListSerializer(CatalogServiceDto.serializer()), jsonStr)
        } catch (e: Exception) {
            Log.e("ServiceCatalogRepo", "Failed to load services from cache", e)
            emptyList()
        }
    }

    private fun getFallbackServices(): List<CatalogServiceDto> {
        return listOf(
            CatalogServiceDto("visa_extension", "International", "Visa Extension", "Visa and document extension support for international students.", "3-5 working days", emptyList(), true),
            CatalogServiceDto("residence_permit", "International", "Residence Permit", "Manage residence permit guidance and progress.", "3-5 working days", emptyList(), false),
            CatalogServiceDto("back_to_cjlu", "International", "Back to CJLU", "Complete the return-to-campus submission flow.", "3-5 working days", emptyList(), false),
            CatalogServiceDto("information_confirmation", "International", "Information Confirmation", "Confirm required personal or enrollment information.", "3-5 working days", emptyList(), false),
            CatalogServiceDto("change_major", "Academic", "Change Major", "Submit a request to adjust your current major pathway.", "2-4 working days", emptyList(), false),
            CatalogServiceDto("ask_leave", "Academic", "Sick Leave", "Apply for sick leave so approved absences do not lower your attendance.", "2-4 working days", emptyList(), true),
            CatalogServiceDto("transfer_school", "Academic", "Transfer School", "Manage transfer-related academic paperwork.", "2-4 working days", emptyList(), false),
            CatalogServiceDto("quit_school", "Academic", "Quit School", "Start the official school withdrawal process.", "2-4 working days", emptyList(), false),
            CatalogServiceDto("restudy", "Academic", "Restudy", "Arrange a restudy plan for a completed course.", "2-4 working days", emptyList(), false),
            CatalogServiceDto("self_study", "Academic", "Self-study", "Register for self-study related academic approval.", "2-4 working days", emptyList(), false),
            CatalogServiceDto("delay_exams", "Academic", "Delay Exams", "Apply to delay an exam due to approved reasons.", "2-4 working days", emptyList(), false),
            CatalogServiceDto("suspension_degree", "Academic", "Suspension of Degree", "Pause your current academic status through a formal request.", "2-4 working days", emptyList(), false),
            CatalogServiceDto("resume_school", "Academic", "Resume School", "Resume study after suspension or leave.", "2-4 working days", emptyList(), false),
            CatalogServiceDto("school_calendar", "Learning", "School Calendar", "View semester dates, holidays, exams, and key deadlines.", "Within 1 working day", emptyList(), false),
            CatalogServiceDto("scholarship", "Learning", "Scholarship", "Review scholarship information and application status.", "Within 1 working day", emptyList(), false),
            CatalogServiceDto("class_schedule", "Learning", "Class Schedule", "Open your timetable and classroom schedule.", "Within 1 working day", emptyList(), true),
            CatalogServiceDto("attendance_rate", "Learning", "Attendance Rate", "Track course attendance and participation records.", "Within 1 working day", emptyList(), false),
            CatalogServiceDto("transcripts", "Learning", "Transcripts", "View or request your academic transcript.", "Within 1 working day", emptyList(), false),
            CatalogServiceDto("education_plan", "Learning", "Education Plan", "Check program requirements and course planning details.", "Within 1 working day", emptyList(), false),
            CatalogServiceDto("changing_room", "Campus", "Changing Room", "Request a dormitory room change.", "1-3 working days", emptyList(), false),
            CatalogServiceDto("repair_request", "Campus", "Repair Request", "Report dormitory or campus facilities that need repair.", "1-3 working days", emptyList(), true),
            CatalogServiceDto("live_off_campus", "Campus", "Live Off Campus", "Manage requests and details for living off campus.", "1-3 working days", emptyList(), false),
            CatalogServiceDto("deposit_refund", "Campus", "Deposit Refund", "Start a dormitory or campus deposit refund request with room and bank details.", "1-3 working days", emptyList(), false)
        )
    }
}
