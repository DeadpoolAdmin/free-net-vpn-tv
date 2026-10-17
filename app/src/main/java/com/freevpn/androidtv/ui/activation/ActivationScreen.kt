package com.freevpn.androidtv.ui.activation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.freevpn.androidtv.R
import com.freevpn.androidtv.ui.common.AppBackground
import com.freevpn.androidtv.ui.common.Eyebrow
import com.freevpn.androidtv.ui.common.PrimaryPillButton
import com.freevpn.androidtv.ui.common.SecondaryPillButton
import com.freevpn.androidtv.ui.common.appViewModelFactory
import com.freevpn.androidtv.ui.common.whiteA
import com.freevpn.androidtv.ui.theme.AccentCyan
import com.freevpn.androidtv.ui.theme.DmSans
import com.freevpn.androidtv.ui.theme.Primary
import com.freevpn.androidtv.ui.theme.TextDim
import com.freevpn.androidtv.ui.theme.TextPrimary
import com.freevpn.androidtv.ui.theme.TextTertiary

@Composable
fun ActivationScreen(
    onActivated: () -> Unit,
    viewModel: ActivationViewModel = viewModel(factory = appViewModelFactory()),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { viewModel.activated.collect { onActivated() } }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    val code = ui.deviceCode?.uppercase()?.filter { it.isLetterOrDigit() }.orEmpty()
    val chars = List(6) { code.getOrNull(it)?.toString() ?: "-" }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 80.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Eyebrow(
                text = stringResource(R.string.activation_optional),
                color = AccentCyan,
                align = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.activation_title),
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.activation_instructions),
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 25.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 720.dp),
            )
            Spacer(Modifier.height(44.dp))

            // Code card
            Column(
                modifier = Modifier
                    .background(whiteA(0.04f), RoundedCornerShape(28.dp))
                    .border(1.dp, whiteA(0.09f), RoundedCornerShape(28.dp))
                    .padding(horizontal = 64.dp, vertical = 44.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Eyebrow(
                    text = stringResource(R.string.activation_code_label),
                    color = TextDim,
                    fontSize = 17.sp,
                    align = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    chars.forEach { ch -> CodeBox(ch) }
                }
                WaitingRow()
            }

            Spacer(Modifier.height(40.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                PrimaryPillButton(
                    text = stringResource(R.string.activation_check_again),
                    onClick = viewModel::checkNow,
                    modifier = Modifier.focusRequester(focusRequester),
                    fontSize = 24.sp,
                )
                SecondaryPillButton(
                    text = stringResource(R.string.activation_skip),
                    onClick = viewModel::continueAnonymous,
                    fontSize = 24.sp,
                    horizontal = 44.dp,
                    vertical = 20.dp,
                )
            }
        }
    }
}

@Composable
private fun CodeBox(ch: String) {
    Box(
        modifier = Modifier
            .size(width = 86.dp, height = 104.dp)
            .background(Primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .border(1.5.dp, AccentCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = ch,
            fontFamily = DmSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 56.sp,
            color = TextPrimary,
        )
    }
}

@Composable
private fun WaitingRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(AccentCyan, CircleShape),
        )
        Text(
            text = stringResource(R.string.activation_waiting),
            style = MaterialTheme.typography.titleSmall,
            color = TextTertiary,
        )
    }
}
