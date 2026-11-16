package com.example.odumonitor.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.odumonitor.R
import com.example.odumonitor.data.local.AppearanceConfig
import com.example.odumonitor.data.local.ChartTimeRange
import com.example.odumonitor.data.local.HistoryRetention
import com.example.odumonitor.data.local.NotificationConfig
import com.example.odumonitor.data.local.RetroSkin
import com.example.odumonitor.data.local.RouterCredentials
import com.example.odumonitor.data.local.WidgetConfig
import com.example.odumonitor.ui.common.ChartMetric
import com.example.odumonitor.ui.common.DashboardNavTab
import com.example.odumonitor.ui.dashboard.components.*
import com.example.odumonitor.ui.radio.RadioControlScreen
import com.example.odumonitor.ui.settings.SettingsScreen
import com.example.odumonitor.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onPollingIntervalSelected: (Long) -> Unit,
    onChartTimeRangeSelected: (ChartTimeRange) -> Unit = {},
    onSelect5gMetric: (String) -> Unit = {},
    onSelect4gMetric: (String) -> Unit = {},
    onSelectTab: (Int) -> Unit = {},
    onUpdateCustom4gBands: (Set<Int>) -> Unit = {},
    onUpdateCustom5gBands: (Set<Int>) -> Unit = {},
    onUpdateHistoryRetention: (HistoryRetention) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onAppearanceConfigChanged: (AppearanceConfig) -> Unit = {},
    onWidgetConfigChanged: (WidgetConfig) -> Unit,
    onRouterCredentialsChanged: (RouterCredentials) -> Unit = {},
    onTestRouterLogin: (RouterCredentials) -> Unit = {},
    onNotificationConfigChanged: (NotificationConfig) -> Unit = {},
    onSetNetworkSelect: (String) -> Unit = {},
    onSet4gBandLock: (String, String) -> Unit = { _, _ -> },
    onSet5gBandLock: (String, String) -> Unit = { _, _ -> },
    onLock4gCell: (Int, Int) -> Unit = { _, _ -> },
    onUnlock4gCell: () -> Unit = {},
    onLock5gCell: (Int, Int, Int) -> Unit = { _, _, _ -> },
    onUnlock5gCell: () -> Unit = {},
    onClearRadioMessage: () -> Unit = {}
) {
    val activeNavTab = DashboardNavTab.entries.getOrElse(uiState.selectedTab) { DashboardNavTab.MONITOR }
    var settingsSubTab by rememberSaveable { mutableIntStateOf(0) }
    val selected5gMetric = ChartMetric.entries.firstOrNull { it.name == uiState.selected5gMetric } ?: ChartMetric.SINR
    val selected4gMetric = ChartMetric.entries.firstOrNull { it.name == uiState.selected4gMetric } ?: ChartMetric.SINR

    val isConnected = uiState.signalState.isConnected && uiState.signalState.errorMessage == null
    val night = LocalRetroSkin.current == RetroSkin.CHARCOAL
    val motion = LocalMotionScale.current
    val monitorLabel = stringResource(R.string.nav_monitor)
    val radioLabel = stringResource(R.string.nav_radio)
    val settingsLabel = stringResource(R.string.nav_settings)
    val onlineColor = signalColor(0, night)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedWidth = maxWidth >= 600.dp
        val horizontalPad = if (isExpandedWidth) 24.dp else 14.dp

        val topBarContent: @Composable () -> Unit = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                        .padding(horizontal = horizontalPad, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            InstrumentWordmark(barHeight = 15.dp)
                            Spacer(Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SignalLed(level = if (isConnected) 0 else 2, size = 5.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isConnected) {
                                        stringResource(R.string.header_connected, uiState.routerCredentials.host)
                                    } else {
                                        stringResource(R.string.header_disconnected, uiState.routerCredentials.host)
                                    },
                                    style = UnitLabel,
                                    color = if (isConnected) onlineColor else MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(MaterialTheme.shapes.small)
                                .clickable(onClick = onRefresh),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.action_refresh),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                InstrumentDivider()
            }
        }

        val screenContent: @Composable (PaddingValues) -> Unit = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = activeNavTab,
                    transitionSpec = {
                        val enter = tween<IntOffset>(
                            durationMillis = (RetroMotion.ENTER_MS * motion).toInt(),
                            easing = RetroMotion.Easing
                        )
                        val exit = tween<IntOffset>(
                            durationMillis = (RetroMotion.EXIT_MS * motion).toInt(),
                            easing = RetroMotion.Easing
                        )
                        (
                            fadeIn(RetroMotion.enter(motion)) +
                                slideInVertically(enter) { it / 90 }
                            ).togetherWith(
                            fadeOut(RetroMotion.exit(motion)) +
                                slideOutVertically(exit) { -it / 140 }
                        )
                    },
                    label = "nav-tab"
                ) { tab ->
                when (tab) {
                    DashboardNavTab.MONITOR -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = horizontalPad, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ConnectionStatusBanner(
                                signal = uiState.signalState,
                                routerHost = uiState.routerCredentials.host,
                                pollingInterval = uiState.pollingIntervalMs,
                                chartTimeRange = uiState.chartTimeRange,
                                isRefreshing = uiState.isRefreshing,
                                onPollingIntervalSelected = onPollingIntervalSelected,
                                onChartTimeRangeSelected = onChartTimeRangeSelected,
                                onRefresh = onRefresh,
                                onOpenRouterSettings = {
                                    settingsSubTab = 0
                                    onSelectTab(DashboardNavTab.SETTINGS.ordinal)
                                }
                            )

                            if (isExpandedWidth) {
                                // 2-Column Adaptive Layout on Tablet/Landscape
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Column 1: Traffic + Carrier Aggregation
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        RouterTrafficCard(
                                            signal = uiState.signalState,
                                            history = uiState.signalHistory,
                                            chartTimeRange = uiState.chartTimeRange,
                                            isExpandedWidth = true,
                                            onOpenSettings = {
                                                settingsSubTab = 0
                                                onSelectTab(DashboardNavTab.SETTINGS.ordinal)
                                            },
                                            appearanceConfig = uiState.appearanceConfig
                                        )

                                        if (uiState.appearanceConfig.showCarrierAggregationCard) {
                                            CarrierAggregationCard(signal = uiState.signalState)
                                        }
                                    }

                                    // Column 2: 5G & 4G RF Tech Cards
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        SignalTechCard(
                                            title = stringResource(R.string.tech_5g),
                                            isConnected = isConnected,
                                            band = uiState.signalState.nrBand,
                                            rsrp = uiState.signalState.nrRsrp,
                                            sinr = uiState.signalState.nrSinr,
                                            rsrq = uiState.signalState.nrRsrq,
                                            pci = uiState.signalState.nrPci,
                                            cellId = uiState.signalState.nrCellId,
                                            earfcnOrArfcn = uiState.signalState.nrArfcn,
                                            dlFreqMhz = uiState.signalState.nrDlFreqMhz,
                                            bandwidth = uiState.signalState.nrBandwidth,
                                            history = uiState.signalHistory,
                                            chartTimeRange = uiState.chartTimeRange,
                                            selectedMetric = selected5gMetric,
                                            onMetricSelected = { onSelect5gMetric(it.name) },
                                            is5g = true,
                                            appearanceConfig = uiState.appearanceConfig
                                        )

                                        SignalTechCard(
                                            title = stringResource(R.string.tech_4g),
                                            isConnected = isConnected,
                                            band = uiState.signalState.lteBand,
                                            rsrp = uiState.signalState.lteRsrp,
                                            sinr = uiState.signalState.lteSinr,
                                            rsrq = uiState.signalState.lteRsrq,
                                            pci = uiState.signalState.ltePci,
                                            cellId = uiState.signalState.lteCellId,
                                            earfcnOrArfcn = uiState.signalState.lteEarfcn,
                                            dlFreqMhz = uiState.signalState.lteDlFreqMhz,
                                            bandwidth = uiState.signalState.lteBandwidth,
                                            history = uiState.signalHistory,
                                            chartTimeRange = uiState.chartTimeRange,
                                            selectedMetric = selected4gMetric,
                                            onMetricSelected = { onSelect4gMetric(it.name) },
                                            is5g = false,
                                            appearanceConfig = uiState.appearanceConfig
                                        )
                                    }
                                }
                            } else {
                                // Single-Column Reflowing Stack on Phone
                                RouterTrafficCard(
                                    signal = uiState.signalState,
                                    history = uiState.signalHistory,
                                    chartTimeRange = uiState.chartTimeRange,
                                    isExpandedWidth = false,
                                    onOpenSettings = {
                                        settingsSubTab = 0
                                        onSelectTab(DashboardNavTab.SETTINGS.ordinal)
                                    },
                                    appearanceConfig = uiState.appearanceConfig
                                )

                                SignalTechCard(
                                    title = stringResource(R.string.tech_5g),
                                    isConnected = isConnected,
                                    band = uiState.signalState.nrBand,
                                    rsrp = uiState.signalState.nrRsrp,
                                    sinr = uiState.signalState.nrSinr,
                                    rsrq = uiState.signalState.nrRsrq,
                                    pci = uiState.signalState.nrPci,
                                    cellId = uiState.signalState.nrCellId,
                                    earfcnOrArfcn = uiState.signalState.nrArfcn,
                                    dlFreqMhz = uiState.signalState.nrDlFreqMhz,
                                    bandwidth = uiState.signalState.nrBandwidth,
                                    history = uiState.signalHistory,
                                    chartTimeRange = uiState.chartTimeRange,
                                    selectedMetric = selected5gMetric,
                                    onMetricSelected = { onSelect5gMetric(it.name) },
                                    is5g = true,
                                    appearanceConfig = uiState.appearanceConfig
                                )

                                SignalTechCard(
                                    title = stringResource(R.string.tech_4g),
                                    isConnected = isConnected,
                                    band = uiState.signalState.lteBand,
                                    rsrp = uiState.signalState.lteRsrp,
                                    sinr = uiState.signalState.lteSinr,
                                    rsrq = uiState.signalState.lteRsrq,
                                    pci = uiState.signalState.ltePci,
                                    cellId = uiState.signalState.lteCellId,
                                    earfcnOrArfcn = uiState.signalState.lteEarfcn,
                                    dlFreqMhz = uiState.signalState.lteDlFreqMhz,
                                    bandwidth = uiState.signalState.lteBandwidth,
                                    history = uiState.signalHistory,
                                    chartTimeRange = uiState.chartTimeRange,
                                    selectedMetric = selected4gMetric,
                                    onMetricSelected = { onSelect4gMetric(it.name) },
                                    is5g = false,
                                    appearanceConfig = uiState.appearanceConfig
                                )

                                if (uiState.appearanceConfig.showCarrierAggregationCard) {
                                    CarrierAggregationCard(signal = uiState.signalState)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    DashboardNavTab.RADIO -> {
                        RadioControlScreen(
                            signalState = uiState.signalState,
                            horizontalPad = horizontalPad,
                            isExecutingRadioCommand = uiState.isExecutingRadioCommand,
                            radioCommandMessage = uiState.radioCommandMessage,
                            custom4gBands = uiState.custom4gBands,
                            custom5gBands = uiState.custom5gBands,
                            onUpdateCustom4gBands = onUpdateCustom4gBands,
                            onUpdateCustom5gBands = onUpdateCustom5gBands,
                            onSetNetworkSelect = onSetNetworkSelect,
                            onSet4gBandLock = onSet4gBandLock,
                            onSet5gBandLock = onSet5gBandLock,
                            onLock4gCell = onLock4gCell,
                            onUnlock4gCell = onUnlock4gCell,
                            onLock5gCell = onLock5gCell,
                            onUnlock5gCell = onUnlock5gCell,
                            onClearRadioMessage = onClearRadioMessage,
                            onGoToRouterLogin = {
                                settingsSubTab = 0
                                onSelectTab(DashboardNavTab.SETTINGS.ordinal)
                            }
                        )
                    }

                    DashboardNavTab.SETTINGS -> {
                        SettingsScreen(
                            selectedSubTab = settingsSubTab,
                            horizontalPad = horizontalPad,
                            onSubTabSelected = { settingsSubTab = it },
                            config = uiState.widgetConfig,
                            appearanceConfig = uiState.appearanceConfig,
                            routerCreds = uiState.routerCredentials,
                            notifConfig = uiState.notificationConfig,
                            historyRetention = uiState.historyRetention,
                            isTestingLogin = uiState.isTestingLogin,
                            loginStatusMessage = uiState.loginStatusMessage,
                            loginStatusSuccess = uiState.loginStatusSuccess,
                            onConfigChanged = onWidgetConfigChanged,
                            onAppearanceConfigChanged = onAppearanceConfigChanged,
                            onRouterCredentialsChanged = onRouterCredentialsChanged,
                            onTestLogin = onTestRouterLogin,
                            onNotificationConfigChanged = onNotificationConfigChanged,
                            onHistoryRetentionChanged = onUpdateHistoryRetention,
                            onClearHistory = onClearHistory
                        )
                    }
                }
                }
            }
        }

        if (isExpandedWidth) {
            // Tablet / Landscape Adaptive Layout with NavigationRail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    header = {
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = "ZTE MC7500",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                ) {
                    InstrumentNavRailItem(
                        selected = activeNavTab == DashboardNavTab.MONITOR,
                        onClick = { onSelectTab(DashboardNavTab.MONITOR.ordinal) },
                        icon = Icons.Default.SignalCellularAlt,
                        label = monitorLabel
                    )
                    InstrumentNavRailItem(
                        selected = activeNavTab == DashboardNavTab.RADIO,
                        onClick = { onSelectTab(DashboardNavTab.RADIO.ordinal) },
                        icon = Icons.Default.SettingsInputAntenna,
                        label = radioLabel
                    )
                    InstrumentNavRailItem(
                        selected = activeNavTab == DashboardNavTab.SETTINGS,
                        onClick = { onSelectTab(DashboardNavTab.SETTINGS.ordinal) },
                        icon = Icons.Default.Tune,
                        label = settingsLabel
                    )
                }

                Scaffold(
                    topBar = topBarContent,
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    screenContent(innerPadding)
                }
            }
        } else {
            // Standard Phone Portrait Layout with NavigationBar
            Scaffold(
                topBar = topBarContent,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        InstrumentNavBarItem(
                            selected = activeNavTab == DashboardNavTab.MONITOR,
                            onClick = { onSelectTab(DashboardNavTab.MONITOR.ordinal) },
                            icon = Icons.Default.SignalCellularAlt,
                            label = monitorLabel
                        )
                        InstrumentNavBarItem(
                            selected = activeNavTab == DashboardNavTab.RADIO,
                            onClick = { onSelectTab(DashboardNavTab.RADIO.ordinal) },
                            icon = Icons.Default.SettingsInputAntenna,
                            label = radioLabel
                        )
                        InstrumentNavBarItem(
                            selected = activeNavTab == DashboardNavTab.SETTINGS,
                            onClick = { onSelectTab(DashboardNavTab.SETTINGS.ordinal) },
                            icon = Icons.Default.Tune,
                            label = settingsLabel
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                screenContent(innerPadding)
            }
        }

        // The screen is a plate. No overlay, no scanlines, no filter.
    }
}
/**
 * Navigation without the Material pill. The indicator is a 2dp lit rule under
 * the label, which is how a switch bank marks the selected position, and the
 * space for it is always reserved so nothing shifts when the tab changes.
 */
@Composable
private fun RowScope.InstrumentNavBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(imageVector = icon, contentDescription = label) },
        label = { InstrumentNavLabel(label = label, selected = selected) },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = Color.Transparent,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun InstrumentNavRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(imageVector = icon, contentDescription = label) },
        label = { InstrumentNavLabel(label = label, selected = selected) },
        colors = NavigationRailItemDefaults.colors(
            indicatorColor = Color.Transparent,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun InstrumentNavLabel(label: String, selected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = UnitLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .width(18.dp)
                .height(2.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                )
        )
    }
}