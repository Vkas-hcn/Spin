package com.spin.secure.bean

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@Keep
@JsonClass(generateAdapter = true)
data class SpinServer(
    @Json(name = "GLWAWwiBGZ")
    var mode: String?=null,//模式(协议)名

    @Json(name = "AeTN")
    var ip: String?=null,//ip地址

    @Json(name = "ulW")
    var port: Int?=null,//端口号

    @Json(name = "TjAicqj")
    var userd: String?=null,//用户名

    @Json(name = "hKWP")
    var password: String?=null,//密码

    @Json(name = "gFnSy")
    var method: String?=null,//加密方式

    @Json(name = "ZORqHnF")
    var city: String?=null,//城市名

    @Json(name = "LmxgG")
    var country: String?=null,//国家名称

    @Json(name = "ksAz")
    var countryCode: String?=null//国家码
) : Serializable
