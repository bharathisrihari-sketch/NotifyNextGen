package com.example.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.NotificationTrackerService
import com.example.ui.components.PermissionBanner
import com.example.ui.components.SearchBarWithFilters
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AppsOverviewScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TestNotificationDialog
import com.example.ui.screens.TimelineScreen
import com.example.ui.theme.Indigo40

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: NotificationHubViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Recheck permission when app resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()
    val isServiceConnected by viewModel.isServiceConnected.collectAsStateWithLifecycle()
    val filteredNotifications by viewModel.filteredNotifications.collectAsStateWithLifecycle()
    val appCounts by viewModel.appCounts.collectAsStateWithLifecycle()
    val analyticsState by viewModel.analyticsState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedPackage by viewModel.selectedPackage.collectAsStateWithLifecycle()
    val onlyStarred by viewModel.onlyStarred.collectAsStateWithLifecycle()
    val unreadOnly by viewModel.unreadOnly.collectAsStateWithLifecycle()
    val groupByMode by viewModel.groupByMode.collectAsStateWithLifecycle()

    var showTestAlertModal by remember { mutableStateOf(false) }

    // Handle back button when on non-home tabs or when filter is active
    BackHandler(enabled = activeTab != HubTab.TIMELINE || selectedPackage != null || searchQuery.isNotBlank()) {
        if (selectedPackage != null) {
            viewModel.selectPackage(null)
        } else if (searchQuery.isNotBlank()) {
            viewModel.updateSearchQuery("")
        } else {
            viewModel.setActiveTab(HubTab.TIMELINE)
        }
    }

    if (showTestAlertModal) {
        TestNotificationDialog(
            onDismiss = { showTestAlertModal = false },
            onSendAlert = { appName, title, content, category ->
                viewModel.postSimulatedNotification(context, appName, title, content, category)
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Indigo40),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "NotifyVault",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                )
                            )
                            Text(
                                text = "All App Notifications",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showTestAlertModal = true },
                        modifier = Modifier.testTag("action_simulate_alert")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAlert,
                            contentDescription = "Simulate Alert",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == HubTab.TIMELINE,
                    onClick = { viewModel.setActiveTab(HubTab.TIMELINE) },
                    icon = { Icon(Icons.Default.History, contentDescription = "Timeline") },
                    label = { Text("Timeline") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_timeline")
                )

                NavigationBarItem(
                    selected = activeTab == HubTab.APPS,
                    onClick = { viewModel.setActiveTab(HubTab.APPS) },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "By App") },
                    label = { Text("By App") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_apps")
                )

                NavigationBarItem(
                    selected = activeTab == HubTab.ANALYTICS,
                    onClick = { viewModel.setActiveTab(HubTab.ANALYTICS) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Insights") },
                    label = { Text("Insights") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_analytics")
                )

                NavigationBarItem(
                    selected = activeTab == HubTab.SETTINGS,
                    onClick = { viewModel.setActiveTab(HubTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Tools") },
                    label = { Text("Tools") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        },
        floatingActionButton = {
            if (activeTab == HubTab.TIMELINE) {
                FloatingActionButton(
                    onClick = { showTestAlertModal = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("fab_simulate_alert")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulate", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permission Notice Banner if disabled
            PermissionBanner(
                hasPermission = hasPermission,
                onGrantPermission = {
                    try {
                        context.startActivity(NotificationTrackerService.getSettingsIntent())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            )

            // Show Search & Filter chips if on Timeline tab
            if (activeTab == HubTab.TIMELINE) {
                SearchBarWithFilters(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.selectCategory(it) },
                    selectedPackage = selectedPackage,
                    onClearSelectedPackage = { viewModel.selectPackage(null) },
                    onlyStarred = onlyStarred,
                    onToggleOnlyStarred = { viewModel.toggleOnlyStarred() },
                    unreadOnly = unreadOnly,
                    onToggleUnreadOnly = { viewModel.toggleUnreadOnly() },
                    groupByMode = groupByMode,
                    onGroupByModeChange = { viewModel.setGroupByMode(it) },
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "hub_tab_transition",
                modifier = Modifier.weight(1f)
            ) { tab ->
                when (tab) {
                    HubTab.TIMELINE -> {
                        TimelineScreen(
                            notifications = filteredNotifications,
                            searchQuery = searchQuery,
                            groupByMode = groupByMode,
                            onClearSearch = { viewModel.updateSearchQuery("") },
                            onToggleStar = { viewModel.toggleStar(it) },
                            onToggleRead = { viewModel.toggleReadStatus(it) },
                            onDeleteNotification = { viewModel.deleteNotification(it) },
                            onMarkAllRead = { viewModel.markAllAsRead() },
                            onRequestSimulateAlert = { showTestAlertModal = true }
                        )
                    }
                    HubTab.APPS -> {
                        AppsOverviewScreen(
                            appCounts = appCounts,
                            onSelectApp = { pkg ->
                                viewModel.selectPackage(pkg)
                                viewModel.setActiveTab(HubTab.TIMELINE)
                            },
                            onDeleteAppHistory = { pkg ->
                                viewModel.deleteByPackage(pkg)
                            }
                        )
                    }
                    HubTab.ANALYTICS -> {
                        AnalyticsScreen(analytics = analyticsState)
                    }
                    HubTab.SETTINGS -> {
                        SettingsScreen(
                            hasPermission = hasPermission,
                            isServiceConnected = isServiceConnected,
                            onOpenPermissionSettings = {
                                try {
                                    context.startActivity(NotificationTrackerService.getSettingsIntent())
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            onRebindService = {
                                viewModel.rebindService(context)
                            },
                            onRequestSimulateAlert = { showTestAlertModal = true },
                            onExportLogs = {
                                val exportText = viewModel.buildShareExportText(filteredNotifications)
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "NotifyVault History Export")
                                    putExtra(Intent.EXTRA_TEXT, exportText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Export Notifications"))
                            },
                            onMarkAllRead = { viewModel.markAllAsRead() },
                            onCleanOlderThanDays = { days ->
                                viewModel.cleanupOlderThanDays(days)
                            },
                            onClearAllLogs = {
                                viewModel.clearAllNotifications()
                            }
                        )
                    }
                }
            }
        }
    }
}
