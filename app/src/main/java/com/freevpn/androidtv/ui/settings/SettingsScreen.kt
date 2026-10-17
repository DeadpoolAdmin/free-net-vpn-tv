package com.freevpn.androidtv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.freevpn.androidtv.R
import com.freevpn.androidtv.ui.common.AppBackground
import com.freevpn.androidtv.ui.common.DesignCard
import com.freevpn.androidtv.ui.common.ScreenHeader
import com.freevpn.androidtv.ui.common.appViewModelFactory
import com.freevpn.androidtv.ui.common.whiteA
import com.freevpn.androidtv.ui.theme.AccentCyan
import com.freevpn.androidtv.ui.theme.DmSans
import com.freevpn.androidtv.ui.theme.StatusRed
import com.freevpn.androidtv.ui.theme.TextDim
import com.freevpn.androidtv.ui.theme.TextPrimary
import com.freevpn.androidtv.ui.theme.TextTertiary

private val TrackOn = Color(0xFF2A6E5C)
private val ClearTitle = Color(0xFFF2828A)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
    onActivationCleared: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = appViewModelFactory()),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { viewModel.activationCleared.collect { onActivationCleared() } }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 110.dp, vertical = 56.dp),
        ) {
            ScreenHeader(title = stringResource(R.string.settings_title), onBack = onBack)
            Spacer(Modifier.height(36.dp))

            Column(
                modifier = Modifier.widthIn(max = 1200.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ToggleRow(
                    title = stringResource(R.string.settings_auto_connect),
                    desc = stringResource(R.string.settings_auto_connect_desc),
                    on = ui.autoConnect,
                    onToggle = { viewModel.setAutoConnect(!ui.autoConnect) },
                    modifier = Modifier.focusRequester(firstFocus),
                )
                ToggleRow(
                    title = stringResource(R.string.settings_auto_reconnect),
                    desc = stringResource(R.string.settings_auto_reconnect_desc),
                    on = ui.autoReconnect,
                    onToggle = { viewModel.setAutoReconnect(!ui.autoReconnect) },
                )
                ToggleRow(
                    title = stringResource(R.string.settings_show_notification),
                    desc = stringResource(R.string.settings_show_notification_desc),
                    on = ui.showNotification,
                    onToggle = { viewModel.setShowNotification(!ui.showNotification) },
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .height(1.dp)
                        .background(whiteA(0.08f)),
                )

                NavRow(
                    title = stringResource(R.string.settings_privacy),
                    desc = stringResource(R.string.settings_privacy_desc),
                    onClick = onOpenPrivacy,
                )
                NavRow(
                    title = stringResource(R.string.settings_about),
                    desc = stringResource(R.string.settings_about_desc),
                    onClick = onOpenAbout,
                )
                DesignCard(
                    onClick = viewModel::clearActivation,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = StatusRed.copy(alpha = 0.06f),
                    focusedContainerColor = StatusRed.copy(alpha = 0.12f),
                    borderColor = StatusRed.copy(alpha = 0.22f),
                    focusedBorderColor = StatusRed.copy(alpha = 0.4f),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 24.dp)) {
                        Text(
                            text = stringResource(R.string.settings_clear_activation),
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 25.sp,
                            color = ClearTitle,
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = stringResource(R.string.settings_clear_activation_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    desc: String,
    on: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DesignCard(onClick = onToggle, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontFamily = DmSans, fontWeight = FontWeight.Bold, fontSize = 25.sp, color = TextPrimary)
                Spacer(Modifier.height(5.dp))
                Text(desc, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = stringResource(if (on) R.string.settings_on else R.string.settings_off),
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (on) AccentCyan else TextDim,
                )
                ToggleSwitch(on = on)
            }
        }
    }
}

@Composable
private fun ToggleSwitch(on: Boolean) {
    val knobX = if (on) 32.dp else 4.dp
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 36.dp)
            .background(if (on) TrackOn else whiteA(0.12f), RoundedCornerShape(999.dp)),
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobX, y = 4.dp)
                .size(28.dp)
                .background(TextPrimary, CircleShape),
        )
    }
}

@Composable
private fun NavRow(title: String, desc: String, onClick: () -> Unit) {
    DesignCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontFamily = DmSans, fontWeight = FontWeight.Bold, fontSize = 25.sp, color = TextPrimary)
                Spacer(Modifier.height(5.dp))
                Text(desc, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = TextDim,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
