package com.cjlu.studentapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cjlu.studentapp.auth.AuthManager
import com.cjlu.studentapp.data.AcademicRepository
import com.cjlu.studentapp.data.AppDatabase
import com.cjlu.studentapp.data.MessagesRepository
import com.cjlu.studentapp.data.RequestManager
import com.cjlu.studentapp.data.ServiceCatalogRepository
import com.cjlu.studentapp.localization.AppLanguage
import com.cjlu.studentapp.localization.LanguageManager
import com.cjlu.studentapp.navigation.AppNavigation
import com.cjlu.studentapp.navigation.Screen
import com.cjlu.studentapp.navigation.navigateToMainTab
import com.cjlu.studentapp.network.ApiLanguageStore
import com.cjlu.studentapp.network.RealtimeSyncCoordinator
import com.cjlu.studentapp.network.RealtimeSyncEffect
import com.cjlu.studentapp.notifications.CjluNotificationHelper
import com.cjlu.studentapp.notifications.FcmTokenRegistrar
import com.cjlu.studentapp.prefs.AppNotificationPrefs
import com.cjlu.studentapp.prefs.WidgetStatsStore
import com.cjlu.studentapp.ui.components.BottomNavItem
import com.cjlu.studentapp.ui.screens.LoginScreen
import com.cjlu.studentapp.ui.screens.ServiceItem
import com.cjlu.studentapp.ui.theme.CJLUStudentAppTheme
import com.cjlu.studentapp.widget.CjluAppWidget
import com.cjlu.studentapp.widget.CjluWidget
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val widgetOpenTabRoute = mutableStateOf<String?>(null)
    private val widgetOpenScreenRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        widgetOpenTabRoute.value = intent.getStringExtra(CjluWidget.EXTRA_OPEN_TAB)
        widgetOpenScreenRoute.value = intent.getStringExtra(CjluWidget.EXTRA_OPEN_ROUTE)
        CjluNotificationHelper.ensureChannels(this)
        requestNotificationPermissionIfNeeded()

        LanguageManager.applyLanguage(LanguageManager.getSavedLanguage(this))
        enableEdgeToEdge()

        setContent {
            val openTabRoute by widgetOpenTabRoute
            val openScreenRoute by widgetOpenScreenRoute
            CJLUStudentAppTheme(darkTheme = false, dynamicColor = false) {
                CJLUStudentApp(
                    initialLanguage = LanguageManager.getSavedLanguage(this@MainActivity),
                    openTabRoute = openTabRoute,
                    openScreenRoute = openScreenRoute,
                    onConsumeOpenTabRoute = { widgetOpenTabRoute.value = null },
                    onConsumeOpenScreenRoute = { widgetOpenScreenRoute.value = null },
                ) { language ->
                    LanguageManager.saveAndApplyLanguage(this@MainActivity, language)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        widgetOpenTabRoute.value = intent.getStringExtra(CjluWidget.EXTRA_OPEN_TAB)
        widgetOpenScreenRoute.value = intent.getStringExtra(CjluWidget.EXTRA_OPEN_ROUTE)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CJLUStudentApp(
    initialLanguage: AppLanguage,
    openTabRoute: String? = null,
    openScreenRoute: String? = null,
    onConsumeOpenTabRoute: () -> Unit = {},
    onConsumeOpenScreenRoute: () -> Unit = {},
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    val context = LocalContext.current
    var authSession by remember(context) {
        mutableStateOf(AuthManager.loadSession(context))
    }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = BottomNavItem.items.any { it.screen.route == currentRoute }
    var selectedLanguage by remember(initialLanguage) {
        mutableStateOf(initialLanguage)
    }

    LaunchedEffect(selectedLanguage) {
        ApiLanguageStore.setFromAppLanguage(selectedLanguage)
    }

    if (!authSession.isLoggedIn) {
        LoginScreen(
            defaultStudentId = AuthManager.DEFAULT_STUDENT_ID,
        ) { studentId, password ->
            val err = AuthManager.signIn(context, studentId, password)
            if (err == null) {
                authSession = AuthManager.loadSession(context)
            }
            err
        }
        return
    }

    val studentId = authSession.studentId
    val serviceRequests by RequestManager.observeRequests(context, studentId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val messages by MessagesRepository.observeMessages(context, studentId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var serviceItems by remember(studentId) {
        mutableStateOf(emptyList<ServiceItem>())
    }
    var isConnected by remember { mutableStateOf(true) }
    var backendAvailable by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    suspend fun refreshServicesOnly() {
        try {
            serviceItems = ServiceCatalogRepository.loadServices()
        } catch (e: Exception) {
            Log.e("CJLUStudentApp", "load services failed", e)
        }
    }

    suspend fun markBackendUnavailable() {
        val wasAvailable = backendAvailable
        backendAvailable = false
        if (wasAvailable) {
            snackbarHostState.showSnackbar(
                message = "Backend unavailable. Using local data where possible.",
            )
        }
    }

    fun markBackendAvailable() {
        backendAvailable = true
    }

    var academicRefreshNonce by remember { mutableStateOf(0) }
    var lastAttendanceNotificationAtMillis by remember { mutableStateOf(0L) }

    fun maybeShowAttendanceUpdatedNotification() {
        val now = SystemClock.elapsedRealtime()
        val debounceMillis = 3_000L
        if (now - lastAttendanceNotificationAtMillis < debounceMillis) return
        lastAttendanceNotificationAtMillis = now
        CjluNotificationHelper.showAttendanceUpdatedNotification(context)
    }

    val realtimeSync = remember(studentId, scope) {
        RealtimeSyncCoordinator(
            context = context,
            studentId = studentId,
            scope = scope,
            onConnectionChanged = {
                isConnected = it
                if (it) markBackendAvailable()
            },
            onEffect = { effect ->
                val notifyEnabled = AppNotificationPrefs.isNotifyUpdatesEnabled(context)
                when (effect) {
                    is RealtimeSyncEffect.AcademicCacheInvalidated -> {
                        authSession = AuthManager.loadSession(context)
                        academicRefreshNonce++
                        if (effect.notifyAttendance) {
                            maybeShowAttendanceUpdatedNotification()
                        }
                    }
                    is RealtimeSyncEffect.RequestUpdated -> {
                        if (effect.isBackground && notifyEnabled) {
                            CjluNotificationHelper.showRequestUpdatedNotification(
                                context,
                                effect.request,
                            )
                        }
                    }
                    is RealtimeSyncEffect.MessagesSynced -> {
                        if (effect.isBackground && notifyEnabled) {
                            val newUnread = effect.messages.filter { message ->
                                !message.isRead && message.id !in effect.previousMessageIds
                            }
                            if (newUnread.isNotEmpty()) {
                                val first = newUnread.first()
                                CjluNotificationHelper.showNewMessageNotification(
                                    context,
                                    first.id,
                                    first.title,
                                    first.body,
                                )
                            } else {
                                CjluNotificationHelper.showMessagesRefreshNotification(context)
                            }
                        }
                    }
                }
            },
        )
    }

    fun refreshRemoteData() {
        scope.launch {
            val isBackground = !ProcessLifecycleOwner.get()
                .lifecycle
                .currentState
                .isAtLeast(Lifecycle.State.RESUMED)
            val (_, requestsOnline) = RequestManager.syncRequests(context, studentId)
            val (_, messagesOnline) = MessagesRepository.syncMessages(context, studentId)
            backendAvailable = requestsOnline && messagesOnline
            if (backendAvailable) {
                refreshServicesOnly()
                realtimeSync.invalidateAcademicAndSyncProfile(isBackground)
            } else {
                markBackendUnavailable()
            }
        }
    }

    LaunchedEffect(studentId, selectedLanguage) {
        val (_, requestsOnline) = RequestManager.syncRequests(context, studentId)
        val (_, messagesOnline) = MessagesRepository.syncMessages(context, studentId)
        backendAvailable = requestsOnline && messagesOnline
        if (backendAvailable) {
            refreshServicesOnly()
            realtimeSync.invalidateAcademicAndSyncProfile(isBackground = false)
        } else {
            markBackendUnavailable()
        }
    }

    LaunchedEffect(authSession.isLoggedIn, authSession.studentId) {
        if (authSession.isLoggedIn && android.os.Build.MANUFACTURER.isNotBlank()) {
            FcmTokenRegistrar.registerCurrentToken(context)
        }
    }

    LaunchedEffect(serviceRequests, messages) {
        WidgetStatsStore.writeFromAppState(context, serviceRequests, messages)
        CjluAppWidget().updateAll(context)
    }

    LaunchedEffect(openScreenRoute) {
        val route = openScreenRoute ?: return@LaunchedEffect
        if (route == Screen.AttendanceDetail.route) {
            navController.navigate(Screen.AttendanceDetail.route)
            onConsumeOpenScreenRoute()
        }
    }

    LaunchedEffect(openTabRoute) {
        val tab = openTabRoute ?: return@LaunchedEffect
        val destination = when (tab) {
            CjluWidget.TAB_MESSAGES -> Screen.Messages.route
            CjluWidget.TAB_REQUESTS -> Screen.Home.route
            CjluWidget.TAB_HOME -> Screen.Home.route
            else -> return@LaunchedEffect
        }
        navController.navigateToMainTab(destination)
        onConsumeOpenTabRoute()
    }

    DisposableEffect(studentId) {
        realtimeSync.start()
        onDispose {
            realtimeSync.stop()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (!isConnected && showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(Color(0xFFFFEBEE))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.common_offline_mode),
                        color = Color(0xFFC62828),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
            if (!backendAvailable) {
                TopAppBar(
                    title = { Text(text = "Backend unavailable") },
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    BottomNavItem.items.forEach { item ->
                        val label = stringResource(item.labelRes)

                        NavigationBarItem(
                            selected = currentRoute == item.screen.route,
                            onClick = { navController.navigateToMainTab(item.screen.route) },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = label,
                                )
                            },
                            label = { Text(text = label) },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AppNavigation(
            navController = navController,
            selectedLanguage = selectedLanguage,
            currentStudentId = authSession.studentId,
            currentStudentName = authSession.studentName,
            currentStudyYear = authSession.studyYear,
            currentMajor = authSession.major,
            currentSchool = authSession.school,
            overallAttendancePercent = authSession.overallAttendancePercent,
            classUpdateNotice = authSession.classUpdateNotice,
            academicRefreshNonce = academicRefreshNonce,
            backendAvailable = backendAvailable,
            serviceRequests = serviceRequests,
            serviceItems = serviceItems,
            messages = messages,
            onRefreshRequests = { refreshRemoteData() },
            onReloadMessages = {
                scope.launch {
                    val (_, messagesOnline) = MessagesRepository.syncMessages(context, studentId)
                    backendAvailable = messagesOnline
                    if (!messagesOnline) {
                        markBackendUnavailable()
                    } else {
                        markBackendAvailable()
                    }
                }
            },
            onSetMessageRead = { messageId, read ->
                scope.launch {
                    val ok = MessagesRepository.setMessageRead(context, studentId, messageId, read)
                    if (!ok) markBackendUnavailable()
                }
            },
            onLogout = {
                val studentId = authSession.studentId
                AuthManager.signOut(context)
                if (studentId.isNotBlank()) {
                    scope.launch {
                        AcademicRepository.clearCacheForStudent(context, studentId)
                        AppDatabase.getDatabase(context)
                            .inboxMessageDao()
                            .deleteForStudent(studentId)
                    }
                }
                authSession = AuthManager.loadSession(context)
            },
            onChangePassword = { currentPassword, newPassword ->
                AuthManager.changePassword(context, currentPassword, newPassword)
            },
            onLanguageSelected = { language ->
                selectedLanguage = language
                onLanguageSelected(language)
            },
            onSaveProfile = { major, school ->
                val ok = AuthManager.updateProfileOnServer(context, major, school)
                if (ok) {
                    authSession = AuthManager.loadSession(context)
                }
                ok
            },
            onSubmitServiceRequest = { submission, uri ->
                RequestManager.createRequest(context, submission, uri)
            },
            onAfterRequestSubmitted = {
                scope.launch { RequestManager.syncRequests(context, studentId) }
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}
