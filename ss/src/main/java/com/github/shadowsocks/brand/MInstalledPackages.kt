package com.github.shadowsocks.brand

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import java.io.Serializable

@Keep
@JsonClass(generateAdapter = true)
data class MInstalledPackages(
    val packages: List<String> = emptyList()
) : Serializable
