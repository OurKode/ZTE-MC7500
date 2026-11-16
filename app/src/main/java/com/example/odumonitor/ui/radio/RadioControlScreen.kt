package com.example.odumonitor.ui.radio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.odumonitor.R
import com.example.odumonitor.data.local.RetroSkin
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.ui.theme.InstrumentChip
import com.example.odumonitor.ui.theme.LocalRetroSkin
import com.example.odumonitor.ui.theme.PlateLabel
import com.example.odumonitor.ui.theme.SignalFair
import com.example.odumonitor.ui.theme.SignalFairLight
import com.example.odumonitor.util.FrequencyConverter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioControlScreen(
    signalState: OduSignalState,
    horizontalPad: Dp,
    isExecutingRadioCommand: Boolean,
    radioCommandMessage: String?,
    custom4gBands: Set<Int> = setOf(1, 3, 8, 40),
    custom5gBands: Set<Int> = setOf(1, 3, 28, 40, 78),
    onUpdateCustom4gBands: (Set<Int>) -> Unit = {},
    onUpdateCustom5gBands: (Set<Int>) -> Unit = {},
    onSetNetworkSelect: (String) -> Unit,
    onSet4gBandLock: (String, String) -> Unit,
    onSet5gBandLock: (String, String) -> Unit,
    onLock4gCell: (Int, Int) -> Unit,
    onUnlock4gCell: () -> Unit,
    onLock5gCell: (Int, Int, Int) -> Unit,
    onUnlock5gCell: () -> Unit,
    onClearRadioMessage: () -> Unit,
    onGoToRouterLogin: () -> Unit
) {
    val isDark = LocalRetroSkin.current == RetroSkin.CHARCOAL
    val warningColor = if (isDark) SignalFair else SignalFairLight
    var pendingConfirmation by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    if (pendingConfirmation != null) {
        AlertDialog(
            onDismissRequest = { pendingConfirmation = null },
            title = {
                Text(
                    text = stringResource(R.string.radio_confirm_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = pendingConfirmation!!.first,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val action = pendingConfirmation!!.second
                        pendingConfirmation = null
                        action()
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.radio_apply), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingConfirmation = null },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.radio_cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = horizontalPad, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!radioCommandMessage.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(3.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isExecutingRadioCommand) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isExecutingRadioCommand) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(
                            text = radioCommandMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isExecutingRadioCommand) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }
                    if (!isExecutingRadioCommand) {
                        IconButton(
                            onClick = onClearRadioMessage,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.radio_close_message),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        if (!signalState.isConnected || !signalState.isRouterLoggedIn) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(3.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (!signalState.isConnected) Icons.Default.WifiOff else Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (!signalState.isConnected) {
                                stringResource(R.string.radio_odu_disconnected_title)
                            } else {
                                stringResource(R.string.radio_admin_required_title)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (!signalState.isConnected) {
                            stringResource(R.string.radio_odu_disconnected_body)
                        } else {
                            stringResource(R.string.radio_admin_required_body)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = onGoToRouterLogin,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.radio_open_router_login))
                    }
                }
            }
        } else {
            // Card 1: Network mode (bearer)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(3.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.radio_section_network_mode).uppercase(),
                        style = PlateLabel,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.radio_active_mode,
                            FrequencyConverter.formatBearerModeName(signalState.netSelect)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val bearerModes = listOf(
                        Triple("Auto (5G/4G/3G)", "WL_AND_5G", stringResource(R.string.radio_confirm_bearer_auto)),
                        Triple("5G NSA + 4G LTE", "LTE_AND_5G", stringResource(R.string.radio_confirm_bearer_nsa)),
                        Triple("5G SA Only", "Only_5G", stringResource(R.string.radio_confirm_bearer_sa)),
                        Triple("4G LTE Only", "Only_LTE", stringResource(R.string.radio_confirm_bearer_lte))
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bearerModes.chunked(2).forEach { rowModes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowModes.forEach { (label, modeCode, promptText) ->
                                    val isCurrent = signalState.netSelect == modeCode
                                    InstrumentChip(
                                        selected = isCurrent,
                                        onClick = {
                                            pendingConfirmation = Pair(promptText) {
                                                onSetNetworkSelect(modeCode)
                                            }
                                        },
                                        enabled = !isExecutingRadioCommand,
                                        label = label,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card 2: 4G LTE band lock
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(3.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.radio_section_4g_band).uppercase(),
                        style = PlateLabel,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val active4g = signalState.lteBandLock?.toLongOrNull()?.let {
                        FrequencyConverter.parse4gActiveBands(it)
                    } ?: emptyList()
                    val active4gLabel = if (active4g.isEmpty() || active4g.size >= 10) {
                        stringResource(R.string.radio_all_bands_auto)
                    } else {
                        "B" + active4g.joinToString(", B")
                    }
                    Text(
                        text = stringResource(R.string.radio_lock_status, active4gLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.radio_operator_profiles).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val operatorProfiles = listOf(
                        Triple("Telkomsel (B1,3,8,40)", listOf(1, 3, 8, 40), "Telkomsel (B1, B3, B8, B40)"),
                        Triple("XL Axiata (B1,3,8)", listOf(1, 3, 8), "XL Axiata (B1, B3, B8)"),
                        Triple("Indosat (B1,3,8)", listOf(1, 3, 8), "Indosat (B1, B3, B8)"),
                        Triple("Smartfren (B28,40)", listOf(28, 40), "Smartfren (B28, B40)")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        operatorProfiles.chunked(2).forEach { rowOps ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowOps.forEach { (btnText, bands, desc) ->
                                    val mask = FrequencyConverter.calculate4gBandMask(bands).toString()
                                    val confirmPrompt = stringResource(R.string.radio_confirm_lock_4g_profile, desc)
                                    OutlinedButton(
                                        onClick = {
                                            pendingConfirmation = Pair(confirmPrompt) {
                                                onSet4gBandLock(mask, desc)
                                            }
                                        },
                                        enabled = !isExecutingRadioCommand,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = btnText,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.radio_single_4g_band).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val single4gBands = listOf(
                        Pair("Band 1", 1),
                        Pair("Band 3", 3),
                        Pair("Band 8", 8),
                        Pair("Band 40", 40),
                        Pair("Band 28", 28)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        single4gBands.chunked(3).forEach { rowBands ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowBands.forEach { (lbl, bandNum) ->
                                    val mask = (1L shl (bandNum - 1)).toString()
                                    val confirmPrompt = stringResource(R.string.radio_confirm_lock_4g_single, bandNum)
                                    FilledTonalButton(
                                        onClick = {
                                            pendingConfirmation = Pair(confirmPrompt) {
                                                onSet4gBandLock(mask, "Band $bandNum")
                                            }
                                        },
                                        enabled = !isExecutingRadioCommand,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(text = lbl, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.radio_custom_4g_bands).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val custom4gCandidates = listOf(1, 3, 5, 8, 28, 40)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        custom4gCandidates.chunked(3).forEach { rowCandidates ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowCandidates.forEach { bNum ->
                                    val isSelected = custom4gBands.contains(bNum)
                                    InstrumentChip(
                                        selected = isSelected,
                                        onClick = {
                                            val updated = if (isSelected && custom4gBands.size > 1) {
                                                custom4gBands - bNum
                                            } else {
                                                custom4gBands + bNum
                                            }
                                            onUpdateCustom4gBands(updated)
                                        },
                                        label = "B" + bNum.toString(),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                        )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val sortedCustom4g = custom4gBands.sorted()
                    val custom4gLabel = "Custom (" + sortedCustom4g.joinToString(", ") { "B$it" } + ")"
                    val custom4gDesc = custom4gLabel
                    val custom4gMask = FrequencyConverter.calculate4gBandMask(sortedCustom4g).toString()
                    val confirmCustom4g = stringResource(R.string.radio_confirm_lock_4g_custom, custom4gLabel)
                    val unlockAll4gPrompt = stringResource(R.string.radio_confirm_unlock_all_4g)
                    val autoAllLabel = stringResource(R.string.radio_auto_all)

                    Button(
                        onClick = {
                            pendingConfirmation = Pair(confirmCustom4g) {
                                onSet4gBandLock(custom4gMask, custom4gDesc)
                            }
                        },
                        enabled = !isExecutingRadioCommand && sortedCustom4g.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.radio_apply_selection, custom4gLabel),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            pendingConfirmation = Pair(unlockAll4gPrompt) {
                                onSet4gBandLock(FrequencyConverter.FULL_4G_MASK.toString(), autoAllLabel)
                            }
                        },
                        enabled = !isExecutingRadioCommand,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.radio_unlock_all_4g),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Card 3: 5G NR band lock (SA)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(3.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.radio_section_5g_band).uppercase(),
                        style = PlateLabel,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.radio_5g_band_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val single5gBands = listOf("40", "1", "3", "78")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        single5gBands.chunked(2).forEach { rowBands ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowBands.forEach { b ->
                                    val confirmPrompt = stringResource(R.string.radio_confirm_lock_5g_single, b)
                                    FilledTonalButton(
                                        onClick = {
                                            pendingConfirmation = Pair(confirmPrompt) {
                                                onSet5gBandLock(b, "n$b")
                                            }
                                        },
                                        enabled = !isExecutingRadioCommand,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.radio_band_n, b),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.radio_custom_5g_bands).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val custom5gCandidates = listOf(1, 3, 28, 40, 78)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        custom5gCandidates.chunked(3).forEach { rowCandidates ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowCandidates.forEach { bNum ->
                                    val isSelected = custom5gBands.contains(bNum)
                                    InstrumentChip(
                                        selected = isSelected,
                                        onClick = {
                                            val updated = if (isSelected && custom5gBands.size > 1) {
                                                custom5gBands - bNum
                                            } else {
                                                custom5gBands + bNum
                                            }
                                            onUpdateCustom5gBands(updated)
                                        },
                                        label = "n" + bNum.toString(),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                        )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val sortedCustom5g = custom5gBands.sorted()
                    val custom5gLabel = "Custom (" + sortedCustom5g.joinToString(", ") { "n$it" } + ")"
                    val custom5gDesc = custom5gLabel
                    val custom5gPayload = sortedCustom5g.joinToString(",")
                    val confirmCustom5g = stringResource(R.string.radio_confirm_lock_5g_custom, custom5gLabel)
                    val unlockAll5gPrompt = stringResource(R.string.radio_confirm_unlock_all_5g)
                    val autoAll5gLabel = stringResource(R.string.radio_auto_all)

                    Button(
                        onClick = {
                            pendingConfirmation = Pair(confirmCustom5g) {
                                onSet5gBandLock(custom5gPayload, custom5gDesc)
                            }
                        },
                        enabled = !isExecutingRadioCommand && sortedCustom5g.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.radio_apply_selection, custom5gLabel),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            pendingConfirmation = Pair(unlockAll5gPrompt) {
                                val full = FrequencyConverter.FULL_5G_BANDS.joinToString(",")
                                onSet5gBandLock(full, autoAll5gLabel)
                            }
                        },
                        enabled = !isExecutingRadioCommand,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.radio_unlock_all_5g),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Card 4: Cell lock (PCI and channel)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(3.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.radio_section_cell_lock),
                        style = PlateLabel,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.radio_cell_lock_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val lock4gRaw = signalState.lockLteCell
                    val is4gCellLocked = !lock4gRaw.isNullOrBlank() && lock4gRaw != "0,0"
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(3.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.radio_cell_4g),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (is4gCellLocked) {
                                        stringResource(R.string.radio_cell_locked, lock4gRaw ?: "")
                                    } else {
                                        stringResource(R.string.radio_cell_free)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (is4gCellLocked) warningColor else MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val canLock4g = signalState.ltePci > 0 && signalState.lteEarfcn != null
                            val lock4gPci = signalState.ltePci
                            val lock4gEarfcn = signalState.lteEarfcn ?: 0
                            val confirmLock4gCell = stringResource(R.string.radio_confirm_lock_4g_cell, lock4gPci, lock4gEarfcn)
                            val confirmUnlock4gCell = stringResource(R.string.radio_confirm_unlock_4g_cell)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        pendingConfirmation = Pair(confirmLock4gCell) {
                                            onLock4gCell(lock4gPci, lock4gEarfcn)
                                        }
                                    },
                                    enabled = !isExecutingRadioCommand && canLock4g,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (canLock4g) {
                                            stringResource(R.string.radio_lock_pci, signalState.ltePci)
                                        } else {
                                            stringResource(R.string.radio_cell_4g_empty)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        pendingConfirmation = Pair(confirmUnlock4gCell) {
                                            onUnlock4gCell()
                                        }
                                    },
                                    enabled = !isExecutingRadioCommand && is4gCellLocked,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.radio_unlock),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val lock5gRaw = signalState.lockNrCell
                    val is5gCellLocked = !lock5gRaw.isNullOrBlank() && lock5gRaw != "0,0,0"
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(3.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.radio_cell_5g),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (is5gCellLocked) {
                                        stringResource(R.string.radio_cell_locked, lock5gRaw ?: "")
                                    } else {
                                        stringResource(R.string.radio_cell_free)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (is5gCellLocked) warningColor else MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val canLock5g = signalState.nrPci > 0 && signalState.nrArfcn != null
                            val nrBandNumber = signalState.nrBand.replace("n", "").trim().toIntOrNull() ?: 40
                            val lock5gPci = signalState.nrPci
                            val lock5gArfcn = signalState.nrArfcn ?: 0
                            val confirmLock5gCell = stringResource(
                                R.string.radio_confirm_lock_5g_cell,
                                lock5gPci,
                                lock5gArfcn,
                                nrBandNumber
                            )
                            val confirmUnlock5gCell = stringResource(R.string.radio_confirm_unlock_5g_cell)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        pendingConfirmation = Pair(confirmLock5gCell) {
                                            onLock5gCell(lock5gPci, lock5gArfcn, nrBandNumber)
                                        }
                                    },
                                    enabled = !isExecutingRadioCommand && canLock5g,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (canLock5g) {
                                            stringResource(R.string.radio_lock_pci, signalState.nrPci)
                                        } else {
                                            stringResource(R.string.radio_cell_5g_empty)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        pendingConfirmation = Pair(confirmUnlock5gCell) {
                                            onUnlock5gCell()
                                        }
                                    },
                                    enabled = !isExecutingRadioCommand && is5gCellLocked,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.radio_unlock),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.radio_cell_lock_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
