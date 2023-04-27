package com.spin.secure.bean

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@Keep
@JsonClass(generateAdapter = true)
data class SpinData(
    @Json(name = "peGQNZHz")
    var serverList: MutableList<SpinServer>? = null,

    @Json(name = "egAZkhYksq")
    var smartList: MutableList<SpinServer>? = null
) : Serializable
