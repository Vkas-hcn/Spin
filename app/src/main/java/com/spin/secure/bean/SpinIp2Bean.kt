package com.spin.secure.bean

import androidx.annotation.Keep

@Keep
data class SpinIp2Bean(
    val country: String,
    val cc: String,
    val ip: String
)