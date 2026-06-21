package com.cjlu.studentapp.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Services : Screen("services")
    object Messages : Screen("messages")
    object Profile : Screen("profile")
    object BankCardInformation : Screen("bank_card_information")
    object AttendanceDetail : Screen("attendance_detail")
    object SchoolCalendar : Screen("school_calendar")
    object ServiceHub : Screen("service_hub/{serviceId}") {
        const val serviceIdArg = "serviceId"

        fun createRoute(serviceId: String): String {
            return "service_hub/$serviceId"
        }
    }

    object ServiceDetail : Screen("service_detail/{serviceId}?prefillCourse={prefillCourse}") {
        const val serviceIdArg = "serviceId"
        const val prefillCourseArg = "prefillCourse"

        fun createRoute(serviceId: String, prefillCourse: String? = null): String {
            return if (prefillCourse != null) {
                "service_detail/$serviceId?prefillCourse=$prefillCourse"
            } else {
                "service_detail/$serviceId"
            }
        }
    }
}
