package com.spin.secure.ads

import com.spin.secure.SpinApp
import timber.log.Timber

val EmptyAds by lazy { MAds() }
fun String.printAdLog(where: String) {
    if (!SpinApp.DEBUG) return
    Timber.tag("logTagSpin[$where]").d(this)
}