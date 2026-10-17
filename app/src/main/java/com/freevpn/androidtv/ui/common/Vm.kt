package com.freevpn.androidtv.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.freevpn.androidtv.VpnApp

/** Resolves the app's manual-DI [ViewModelProvider.Factory] inside a composable. */
@Composable
fun appViewModelFactory(): ViewModelProvider.Factory =
    (LocalContext.current.applicationContext as VpnApp).container.viewModelFactory
