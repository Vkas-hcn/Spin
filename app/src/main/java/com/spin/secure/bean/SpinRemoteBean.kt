package com.spin.secure.bean

import androidx.annotation.Keep

@Keep
data class SpinRemoteBean(
    val spin_start: String,
    val spin_may: String,
    val spin_show: String,
    val spin_lock: String,
)