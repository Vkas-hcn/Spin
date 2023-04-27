package com.spin.secure.bean

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.squareup.moshi.JsonClass
import java.io.Serializable

@Keep
@JsonClass(generateAdapter = true)
data class SpinVpnBean(
    var code: String,
    var data: SpinData,
    var msg: String
) : Serializable