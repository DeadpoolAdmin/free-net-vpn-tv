package com.freevpn.androidtv.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.freevpn.androidtv.R
import com.freevpn.androidtv.ui.common.AppBackground
import com.freevpn.androidtv.ui.common.DesignCard
import com.freevpn.androidtv.ui.common.ScreenHeader
import com.freevpn.androidtv.ui.common.appViewModelFactory
import com.freevpn.androidtv.ui.common.whiteA
import com.freevpn.androidtv.ui.theme.DmSans
import com.freevpn.androidtv.ui.theme.TextPrimary
import com.freevpn.androidtv.ui.theme.TextTertiary

@Composable
fun AccountScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = viewModel(factory = appViewModelFactory()),
) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    val backFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 110.dp, vertical = 56.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.account_title),
                onBack = onBack,
                backButtonModifier = Modifier.focusRequester(backFocus),
            )
            Spacer(Modifier.height(40.dp))

            val userId = account?.userId
            if (userId.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.account_empty),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextTertiary,
                    modifier = Modifier.widthIn(max = 760.dp),
                )
            } else {
                Column(
                    modifier = Modifier.widthIn(max = 1100.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    InfoRow(label = stringResource(R.string.account_user_id), value = userId)
                    InfoRow(label = stringResource(R.string.account_device_type), value = viewModel.deviceType)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    DesignCard(
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
        containerColor = whiteA(0.045f),
        borderColor = whiteA(0.1f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = TextTertiary,
                modifier = Modifier.widthIn(min = 220.dp),
            )
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                fontFamily = DmSans,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = TextPrimary,
            )
        }
    }
}
