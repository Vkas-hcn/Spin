package com.github.shadowsocks.brand

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import java.io.Serializable

@Keep
@JsonClass(generateAdapter = true)
data class MStrategy(
    val strategy: Int = 0,
    val packages: List<String> = emptyList()
) : Serializable
