package com.spin.secure.ads

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@Keep
@JsonClass(generateAdapter = true)
data class MAds(
    @Json(name = "si_o")
    val open: List<MAd> = emptyList(),
    @Json(name = "si_h")
    val home: List<MAd> = emptyList(),
    @Json(name = "si_c")
    val connect: List<MAd> = emptyList(),
    @Json(name = "si_b")
    val back: List<MAd> = emptyList(),
    @Json(name = "si_r")
    val result: List<MAd> = emptyList(),
    @Json(name = "si_plme")
    val click_num:Int = 0,
    @Json(name = "si_yioi")
    val show_num:Int = 0,
) : Serializable

@Keep
@JsonClass(generateAdapter = true)
data class MAd(
    @Json(name = "si_comp")
    val company: String = "",
    @Json(name = "si_dat")
    val dataId: String = "",
    @Json(name = "si_wy")
    val way: String = "",
    @Json(name = "si_imp")
    val importance: Int = 0,
    var spin_load_ip:String="",
    var spin_load_city:String="",
    var spin_show_ip:String="",
    var spin_show_city:String=""
) : Serializable
