package com.freevpn.androidtv.ui.account

import android.os.Build
import androidx.lifecycle.ViewModel
import com.freevpn.androidtv.data.AccountRepository

/** Exposes the cached account (for the user id) and this hardware's device type for the Account screen. */
class AccountViewModel(accountRepository: AccountRepository) : ViewModel() {

    val account = accountRepository.account

    /** Human-readable device model, e.g. "TCL TV", from the system build info. */
    val deviceType: String = buildDeviceType()

    private fun buildDeviceType(): String {
        val manufacturer = Build.MANUFACTURER?.trim().orEmpty()
        val model = Build.MODEL?.trim().orEmpty()
        val label = when {
            model.isEmpty() -> manufacturer.ifEmpty { "Android TV" }
            manufacturer.isEmpty() -> model
            model.startsWith(manufacturer, ignoreCase = true) -> model
            else -> "$manufacturer $model"
        }
        return label.replaceFirstChar { it.uppercase() }
    }
}
