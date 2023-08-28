package com.spin.secure.bean

import androidx.annotation.Keep

@Keep
data class SpinRemoteBean(
    val spin_start: String,
    val spin_may: String,
    val spin_show: String,
    val spin_lock: String,
)

@Keep
data class SpinRoofBean(
    var spin_phanast: String?=null,//fb4a,facebook
    var spin_porcion: String?=null,//gclid
    var spin_compa: String?=null,//not%20set
    var spin_anyar: String?=null,//youtubeads
    var spin_easya: String?=null,//%7B%22
    var spin_aster: String?=null,//adjust
    var spin_cate: String?=null//bytedance
)