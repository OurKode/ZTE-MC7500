package com.example.odumonitor.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.odumonitor.BuildConfig
import com.example.odumonitor.R
import com.example.odumonitor.data.local.AppearanceConfig
import com.example.odumonitor.data.local.HistoryRetention
import com.example.odumonitor.data.local.NotificationConfig
import com.example.odumonitor.data.local.RetroSkin
import com.example.odumonitor.data.local.RouterCredentials
import com.example.odumonitor.data.local.WidgetConfig
import com.example.odumonitor.ui.theme.InstrumentTabStrip
import com.example.odumonitor.ui.theme.InstrumentToggle
import com.example.odumonitor.ui.theme.LocalRetroSkin
import com.example.odumonitor.ui.theme.PlateLabel
import com.example.odumonitor.ui.theme.SignalExcellent
import com.example.odumonitor.ui.theme.SignalExcellentLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selectedSubTab: Int,
    horizontalPad: Dp,
    onSubTabSelected: (Int) -> Unit,
    config: WidgetConfig,
    appearanceConfig: AppearanceConfig = AppearanceConfig(),
    routerCreds: RouterCredentials,
    notifConfig: NotificationConfig,
    historyRetention: HistoryRetention = HistoryRetention.TWENTY_FOUR_HOURS,
    isTestingLogin: Boolean,
    loginStatusMessage: String?,
    loginStatusSuccess: Boolean? = null,
    onConfigChanged: (WidgetConfig) -> Unit,
    onAppearanceConfigChanged: (AppearanceConfig) -> Unit = {},
    onRouterCredentialsChanged: (RouterCredentials) -> Unit,
    onTestLogin: (RouterCredentials) -> Unit,
    onNotificationConfigChanged: (NotificationConfig) -> Unit,
    onHistoryRetentionChanged: (HistoryRetention) -> Unit = {},
    onClearHistory: () -> Unit = {}
) {
    val isDark = LocalRetroSkin.current == RetroSkin.CHARCOAL
    val okColor = if (isDark) SignalExcellent else SignalExcellentLight

    var hostInput by remember(routerCreds) { mutableStateOf(routerCreds.host) }
    var usernameInput by remember(routerCreds) { mutableStateOf(routerCreds.username) }
    var passwordInput by remember(routerCreds) { mutableStateOf(routerCreds.password) }
    var isLoginEnabled by remember(routerCreds) { mutableStateOf(routerCreds.isLoginEnabled) }
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = horizontalPad, vertical = 12.dp)
    ) {
        val tabTitles = listOf(
            stringResource(R.string.settings_tab_router),
            stringResource(R.string.settings_tab_display),
            stringResource(R.string.settings_tab_widget),
            stringResource(R.string.settings_tab_notifications)
        )
        InstrumentTabStrip(
            items = tabTitles,
            selectedIndex = selectedSubTab,
            onSelected = onSubTabSelected
        )

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(3.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                when (selectedSubTab) {
                    0 -> {
                        Text(
                            text = stringResource(R.string.settings_router_credentials_title).uppercase(),
                            style = PlateLabel,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_router_credentials_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.settings_auto_login),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            InstrumentToggle(
                                checked = isLoginEnabled,
                                onCheckedChange = {
                                    isLoginEnabled = it
                                    onRouterCredentialsChanged(
                                        routerCreds.copy(
                                            host = hostInput,
                                            username = usernameInput,
                                            password = passwordInput,
                                            isLoginEnabled = it
                                        )
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = hostInput,
                            onValueChange = { hostInput = it },
                            label = { Text(stringResource(R.string.settings_router_host)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text(stringResource(R.string.settings_username)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text(stringResource(R.string.settings_password)) },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(
                                    onClick = { showPassword = !showPassword },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = stringResource(R.string.settings_show_password)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val newCreds = RouterCredentials(
                                    host = hostInput,
                                    username = usernameInput,
                                    password = passwordInput,
                                    isLoginEnabled = isLoginEnabled
                                )
                                onRouterCredentialsChanged(newCreds)
                                onTestLogin(newCreds)
                            },
                            enabled = !isTestingLogin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isTestingLogin) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.settings_authenticating),
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.settings_save_and_test_login),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (loginStatusMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val isSuccess = loginStatusSuccess == true
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSuccess) okColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = loginStatusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSuccess) okColor else MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    1 -> {
                        // SUBTAB 1: APPEARANCE / RETRO SKIN
                        Text(
                            text = stringResource(R.string.settings_appearance_title).uppercase(),
                            style = PlateLabel,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_appearance_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 1. Skin
                        Text(
                            text = stringResource(R.string.settings_skin_label).uppercase(),
                            style = PlateLabel,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        InstrumentTabStrip(
                            items = listOf(
                                stringResource(R.string.settings_skin_cream),
                                stringResource(R.string.settings_skin_charcoal)
                            ),
                            selectedIndex = RetroSkin.entries.indexOf(appearanceConfig.skin),
                            onSelected = { index ->
                                onAppearanceConfigChanged(
                                    appearanceConfig.copy(skin = RetroSkin.entries[index])
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Chart element visibility

                        // 3. Chart element visibility
                        Text(
                            text = stringResource(R.string.settings_chart_elements_title).uppercase(),
                            style = PlateLabel,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        WidgetToggleItem(
                            label = stringResource(R.string.settings_chart_traffic),
                            checked = appearanceConfig.showTrafficChart,
                            onCheckedChange = { onAppearanceConfigChanged(appearanceConfig.copy(showTrafficChart = it)) }
                        )

                        WidgetToggleItem(
                            label = stringResource(R.string.settings_chart_5g),
                            checked = appearanceConfig.show5gChart,
                            onCheckedChange = { onAppearanceConfigChanged(appearanceConfig.copy(show5gChart = it)) }
                        )

                        WidgetToggleItem(
                            label = stringResource(R.string.settings_chart_4g),
                            checked = appearanceConfig.show4gChart,
                            onCheckedChange = { onAppearanceConfigChanged(appearanceConfig.copy(show4gChart = it)) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // 4. Additional modules
                        Text(
                            text = stringResource(R.string.settings_modules_title).uppercase(),
                            style = PlateLabel,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        WidgetToggleItem(
                            label = stringResource(R.string.settings_ca_card),
                            checked = appearanceConfig.showCarrierAggregationCard,
                            onCheckedChange = { onAppearanceConfigChanged(appearanceConfig.copy(showCarrierAggregationCard = it)) }
                        )
                    }

                    2 -> {
                        Text(
                            text = stringResource(R.string.settings_widget_title).uppercase(),
                            style = PlateLabel,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_widget_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        WidgetToggleItem(
                            label = stringResource(R.string.settings_widget_show_5g),
                            checked = config.show5g,
                            onCheckedChange = { onConfigChanged(config.copy(show5g = it)) }
                        )
                        WidgetToggleItem(
                            label = stringResource(R.string.settings_widget_show_4g),
                            checked = config.show4g,
                            onCheckedChange = { onConfigChanged(config.copy(show4g = it)) }
                        )
                        WidgetToggleItem(
                            label = stringResource(R.string.settings_widget_show_provider),
                            checked = config.showProviderStatus,
                            onCheckedChange = { onConfigChanged(config.copy(showProviderStatus = it)) }
                        )
                        WidgetToggleItem(
                            label = stringResource(R.string.settings_widget_show_pci),
                            checked = config.showPciTower,
                            onCheckedChange = { onConfigChanged(config.copy(showPciTower = it)) }
                        )
                        WidgetToggleItem(
                            label = stringResource(R.string.settings_widget_show_band),
                            checked = config.showBandInfo,
                            onCheckedChange = { onConfigChanged(config.copy(showBandInfo = it)) }
                        )
                        WidgetToggleItem(
                            label = stringResource(R.string.settings_widget_show_sinr),
                            checked = config.showSinrRsrq,
                            onCheckedChange = { onConfigChanged(config.copy(showSinrRsrq = it)) }
                        )
                        WidgetToggleItem(
                            label = stringResource(R.string.settings_widget_show_timestamp),
                            checked = config.showTimestamp,
                            onCheckedChange = { onConfigChanged(config.copy(showTimestamp = it)) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = stringResource(R.string.settings_sync_interval_title).uppercase(),
                            style = PlateLabel,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val intervals = listOf(
                            15 to stringResource(R.string.settings_interval_15),
                            30 to stringResource(R.string.settings_interval_30),
                            60 to stringResource(R.string.settings_interval_60),
                            180 to stringResource(R.string.settings_interval_180),
                            360 to stringResource(R.string.settings_interval_360),
                            -1 to stringResource(R.string.settings_interval_manual)
                        )

                        intervals.forEach { (min, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable { onConfigChanged(config.copy(updateIntervalMinutes = min)) }
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                RadioButton(
                                    selected = config.updateIntervalMinutes == min,
                                    onClick = { onConfigChanged(config.copy(updateIntervalMinutes = min)) }
                                )
                            }
                        }
                    }

                    3 -> {
                        Text(
                            text = stringResource(R.string.settings_notifications_title).uppercase(),
                            style = PlateLabel,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_notifications_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        WidgetToggleItem(
                            label = stringResource(R.string.settings_notif_service),
                            checked = notifConfig.isServiceEnabled,
                            onCheckedChange = { onNotificationConfigChanged(notifConfig.copy(isServiceEnabled = it)) }
                        )
                        WidgetToggleItem(
                            label = stringResource(R.string.settings_notif_band),
                            checked = notifConfig.notifyBandChange,
                            onCheckedChange = { onNotificationConfigChanged(notifConfig.copy(notifyBandChange = it)) }
                        )
                        WidgetToggleItem(
                            label = stringResource(R.string.settings_notif_wan_ip),
                            checked = notifConfig.notifyWanIpChange,
                            onCheckedChange = { onNotificationConfigChanged(notifConfig.copy(notifyWanIpChange = it)) }
                        )
                        WidgetToggleItem(
                            label = stringResource(R.string.settings_notif_offline),
                            checked = notifConfig.notifyOduOffline,
                            onCheckedChange = { onNotificationConfigChanged(notifConfig.copy(notifyOduOffline = it)) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = stringResource(R.string.settings_retention_title).uppercase(),
                            style = PlateLabel,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_retention_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        HistoryRetention.entries.forEach { retentionOption ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable { onHistoryRetentionChanged(retentionOption) }
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (retentionOption) {
                                        HistoryRetention.TWENTY_FOUR_HOURS -> stringResource(R.string.settings_retention_24h)
                                        HistoryRetention.ONE_WEEK -> stringResource(R.string.settings_retention_week)
                                        HistoryRetention.ONE_MONTH -> stringResource(R.string.settings_retention_month)
                                        HistoryRetention.ONE_YEAR -> stringResource(R.string.settings_retention_year)
                                        HistoryRetention.MANUAL -> stringResource(R.string.settings_retention_manual)
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                RadioButton(
                                    selected = historyRetention == retentionOption,
                                    onClick = { onHistoryRetentionChanged(retentionOption) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onClearHistory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.settings_clear_history),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(
                R.string.settings_footer_version,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun WidgetToggleItem(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        InstrumentToggle(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}
