package com.spin.secure.main.core

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@Keep
@JsonClass(generateAdapter = true)
data class MConnection(
    @Json(name = "si_hs")
    var host: String = "",
    @Json(ignore = true)
    var smart: Boolean = false,
    @Json(name = "si_pr")
    var port: Int = 0,
    @Json(name = "si_mh")
    var method: String = "",
    @Json(name = "si_pw")
    var password: String = "",
    @Json(name = "si_cy")
    var city: String = "",
    @Json(name = "si_cty")
    var country: String = ""
) : Serializable

@Keep
@JsonClass(generateAdapter = true)
data class MConnectionLib(
    @Json(name = "data")
    val data: List<MConnection> = emptyList()
) : Serializable
