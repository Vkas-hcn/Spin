package com.spin.secure.bean

import androidx.annotation.Keep

@Keep
data class SpinIpBean(
    var country: String? = null,
    var country_code: String? = null,
    var country_code3: String? = null,
    var ip: String? = null
)