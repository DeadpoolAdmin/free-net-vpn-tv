package com.freevpn.androidtv.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.freevpn.androidtv.BuildConfig
import com.freevpn.androidtv.R
import com.freevpn.androidtv.ui.common.AppBackground
import com.freevpn.androidtv.ui.common.AppLogo
import com.freevpn.androidtv.ui.common.FreeBadge
import com.freevpn.androidtv.ui.common.ScreenHeader
import com.freevpn.androidtv.ui.theme.DmSans
import com.freevpn.androidtv.ui.theme.TextDim
import com.freevpn.androidtv.ui.theme.TextPrimary
import com.freevpn.androidtv.ui.theme.TextTertiary

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val backFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 110.dp, vertical = 56.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.settings_about),
                onBack = onBack,
                backButtonModifier = Modifier.focusRequester(backFocus),
            )
            Spacer(Modifier.height(40.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                AppLogo(size = 92.dp)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontFamily = DmSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 34.sp,
                            color = TextPrimary,
                        )
                        FreeBadge()
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · MVP build",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 20.sp,
                        color = TextDim,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.about_body),
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 27.sp,
                lineHeight = 43.sp,
                color = TextTertiary,
                modifier = Modifier.widthIn(max = 900.dp),
            )
        }
    }
}
