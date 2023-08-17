package com.github.shadowsocks.brand

import android.content.Context
import android.net.VpnService
//import com.blankj.utilcode.util.AppUtils
//import com.blankj.utilcode.util.ProcessUtils
import com.squareup.moshi.Moshi
import com.tencent.mmkv.MMKV
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.launch
import java.util.*

internal object BrandStrategy {


    fun brand(context: Context, builder: VpnService.Builder, myPackageName: String) {
        (listOf(myPackageName) + listGmsPackages())
            .iterator()
            .forEachRemaining {
                runCatching { builder.addDisallowedApplication(it) }
            }
    }

    private fun listGmsPackages(): List<String> {
        return listOf(
            "com.google.android.gms",
            "com.google.android.ext.services",
            "com.google.process.gservices",
            "com.android.vending",
            "com.google.android.gms.persistent",
            "com.google.android.cellbroadcastservice",
            "com.google.android.packageinstaller"
        )
    }
}