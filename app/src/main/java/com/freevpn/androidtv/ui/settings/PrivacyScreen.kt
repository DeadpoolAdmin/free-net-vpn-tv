package com.freevpn.androidtv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.freevpn.androidtv.R
import com.freevpn.androidtv.ui.common.AppBackground
import com.freevpn.androidtv.ui.common.ScreenHeader
import com.freevpn.androidtv.ui.theme.AccentCyan

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val backFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 110.dp, vertical = 56.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.settings_privacy),
                onBack = onBack,
                backButtonModifier = Modifier.focusRequester(backFocus),
            )
            Spacer(Modifier.height(40.dp))

            Row(
                modifier = Modifier
                    .widthIn(max = 1100.dp)
                    .background(AccentCyan.copy(alpha = 0.05f), RoundedCornerShape(22.dp))
                    .border(1.dp, AccentCyan.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 46.dp, vertical = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_shield_check),
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(40.dp).padding(top = 4.dp),
                )
                Text(
                    text = stringResource(R.string.privacy_body),
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 27.sp,
                    lineHeight = 45.sp,
                    color = Color(0xFFEDECF7),
                )
            }
        }
    }
}
