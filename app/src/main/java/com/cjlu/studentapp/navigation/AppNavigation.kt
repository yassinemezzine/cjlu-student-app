package com.cjlu.studentapp.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cjlu.studentapp.data.RequestSubmission
import com.cjlu.studentapp.data.StudentRequest
import com.cjlu.studentapp.localization.AppLanguage
import com.cjlu.studentapp.network.api.MessageDto
import com.cjlu.studentapp.ui.screens.AttendanceDetailScreen
import com.cjlu.studentapp.ui.screens.BankCardInformationScreen
import com.cjlu.studentapp.ui.screens.HomeScreen
import com.cjlu.studentapp.ui.screens.MessagesScreen
import com.cjlu.studentapp.ui.screens.ProfileScreen
import com.cjlu.studentapp.ui.screens.SchoolCalendarScreen
import com.cjlu.studentapp.ui.screens.ServiceDetailScreen
import com.cjlu.studentapp.ui.screens.ServiceHubScreen
import com.cjlu.studentapp.ui.screens.ServiceItem
import com.cjlu.studentapp.ui.screens.ServicesScreen
import com.cjlu.studentapp.ui.screens.StudentDefaults

@Composable
fun AppNavigation(
    navController: NavHostController,
    selectedLanguage: AppLanguage,
    currentStudentId: String,
    currentStudentName: String,
    currentStudyYear: String,
    currentMajor: String,
    currentSchool: String,
    overallAttendancePercent: Int,
    classUpdateNotice: String?,
    academicRefreshNonce: Int = 0,
    backendAvailable: Boolean = true,
    serviceRequests: List<StudentRequest>,
    serviceItems: List<ServiceItem>,
    messages: List<MessageDto>,
    onRefreshRequests: () -> Unit,
    onReloadMessages: suspend () -> Unit,
    onSetMessageRead: suspend (String, Boolean) -> Unit,
    onLogout: () -> Unit,
    onChangePassword: suspend (String, String) -> Boolean,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSaveProfile: suspend (String, String) -> Boolean,
    onSubmitServiceRequest: suspend (RequestSubmission, Uri?) -> Result<StudentRequest>,
    onAfterRequestSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val studentDefaults = StudentDefaults(
        studentId = currentStudentId,
        studentName = currentStudentName,
        studyYear = currentStudyYear,
        major = currentMajor,
        school = currentSchool,
    )
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                studentId = currentStudentId,
                studentName = currentStudentName,
                activeRequests = serviceRequests,
                serviceItems = serviceItems,
                overallAttendancePercent = overallAttendancePercent,
                classUpdateNotice = if (backendAvailable) classUpdateNotice else "Offline mode: showing cached data and limited live updates.",
                refreshNonce = academicRefreshNonce,
                onRefresh = onRefreshRequests,
                onServiceSelected = { serviceId ->
                    if (serviceId == "class_schedule") {
                        navController.navigate(Screen.ServiceDetail.createRoute(serviceId))
                    } else if (serviceId == "school_calendar") {
                        navController.navigate(Screen.SchoolCalendar.route)
                    } else {
                        navController.navigate(Screen.ServiceHub.createRoute(serviceId))
                    }
                },
                onAttendanceClick = {
                    navController.navigate(Screen.AttendanceDetail.route)
                },
            )
        }
        composable(Screen.Services.route) {
            ServicesScreen(
                serviceItems = serviceItems,
                requests = serviceRequests,
            ) { serviceId ->
                if (serviceId == "class_schedule") {
                    navController.navigate(Screen.ServiceDetail.createRoute(serviceId))
                } else if (serviceId == "school_calendar") {
                    navController.navigate(Screen.SchoolCalendar.route)
                } else {
                    navController.navigate(Screen.ServiceHub.createRoute(serviceId))
                }
            }
        }
        composable(Screen.Messages.route) {
            MessagesScreen(
                messages = messages,
                onReloadMessages = onReloadMessages,
                onSetMessageRead = onSetMessageRead,
            ) { serviceId ->
                if (serviceId == "class_schedule") {
                    navController.navigate(Screen.ServiceDetail.createRoute(serviceId))
                } else if (serviceId == "school_calendar") {
                    navController.navigate(Screen.SchoolCalendar.route)
                } else {
                    navController.navigate(Screen.ServiceHub.createRoute(serviceId))
                }
            }
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                studentName = currentStudentName,
                studentId = currentStudentId,
                studyYear = currentStudyYear,
                major = currentMajor,
                school = currentSchool,
                selectedLanguage = selectedLanguage,
                requests = serviceRequests,
                serviceItems = serviceItems,
                onRequestSelected = { serviceId ->
                    if (serviceId == "class_schedule") {
                        navController.navigate(Screen.ServiceDetail.createRoute(serviceId))
                    } else if (serviceId == "school_calendar") {
                        navController.navigate(Screen.SchoolCalendar.route)
                    } else {
                        navController.navigate(Screen.ServiceHub.createRoute(serviceId))
                    }
                },
                onLogout = onLogout,
                onChangePassword = onChangePassword,
                onLanguageSelected = onLanguageSelected,
                onSaveProfile = onSaveProfile,
            )
        }
        composable(Screen.BankCardInformation.route) {
            BankCardInformationScreen(
                serviceItems = serviceItems,
                studentDefaults = studentDefaults,
                onSubmitServiceRequest = onSubmitServiceRequest,
                onAfterRequestSubmitted = onAfterRequestSubmitted,
            ) { navController.popBackStack() }
        }
        composable(Screen.AttendanceDetail.route) {
            AttendanceDetailScreen(
                studentId = currentStudentId,
                refreshNonce = academicRefreshNonce,
            ) { navController.popBackStack() }
        }
        composable(Screen.SchoolCalendar.route) {
            SchoolCalendarScreen(
                studentId = currentStudentId,
                refreshNonce = academicRefreshNonce,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.ServiceHub.route,
            arguments = listOf(
                navArgument(Screen.ServiceHub.serviceIdArg) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val hubServiceId = backStackEntry.arguments?.getString(Screen.ServiceHub.serviceIdArg).orEmpty()
            ServiceHubScreen(
                serviceId = hubServiceId,
                studentId = currentStudentId,
                serviceItems = serviceItems,
                requests = serviceRequests,
                onNavigateToSubmit = { courseName ->
                    navController.navigate(Screen.ServiceDetail.createRoute(hubServiceId, courseName))
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.ServiceDetail.route,
            arguments = listOf(
                navArgument(Screen.ServiceDetail.serviceIdArg) {
                    type = NavType.StringType
                },
                navArgument(Screen.ServiceDetail.prefillCourseArg) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val detailServiceId = backStackEntry.arguments?.getString(Screen.ServiceDetail.serviceIdArg).orEmpty()
            val prefillCourse = backStackEntry.arguments?.getString(Screen.ServiceDetail.prefillCourseArg)
            ServiceDetailScreen(
                serviceId = detailServiceId,
                prefillCourse = prefillCourse,
                serviceItems = serviceItems,
                studentDefaults = studentDefaults,
                academicRefreshNonce = academicRefreshNonce,
                onSubmitServiceRequest = onSubmitServiceRequest,
                onAfterRequestSubmitted = onAfterRequestSubmitted,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
