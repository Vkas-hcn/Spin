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
    private val sCacheInstallPackages = Collections.synchronizedSet(mutableSetOf<String>())
    private val mmkv by lazy {
        MMKV.mmkvWithID("Spin", MMKV.MULTI_PROCESS_MODE)
    }

    private fun getStrategy(): MStrategy {
        return runCatching {
            Moshi.Builder()
                .build()
                .adapter(MStrategy::class.java)
                .fromJson(mmkv.decodeString("brand_strategy") ?: "")
        }.getOrNull() ?: MStrategy()
    }

    fun brand(context: Context, builder: VpnService.Builder, myPackageName: String) {
        val strategy = getStrategy()
        when (strategy.strategy) {
            1 -> {
                (listOf(myPackageName) + listGmsPackages() + strategy.packages)
                    .iterator()
                    .forEachRemaining {
                        runCatching { builder.addDisallowedApplication(it) }
                    }
            }
            2 -> {
                getWhiteListPackages(context, myPackageName)
                    .iterator()
                    .forEachRemaining {
                        runCatching { builder.addAllowedApplication(it) }
                    }
            }
            else -> builder.addDisallowedApplication(myPackageName)
        }
    }

    private fun getWhiteListPackages(context: Context, myPackageName: String): List<String> {
//        return getAllInstalledPackages(context) - listGmsPackages() - listOf(myPackageName)
        return listOf(
            "com.android.chrome",
            "com.microsoft.emmx",
            "org.mozilla.firefox",
            "com.opera.browser",
            "com.google.android.googlequicksearchbox",
            "mark.via.gp",
            "com.UCMobile.intl",
            "com.brave.browser",
            "privacy.explorer.fast.safe.browser"
        )
    }

    fun init(context: Context) {
//        if (ProcessUtils.isMainProcess()) {
//            GlobalScope.launch(Dispatchers.IO) {
//                updateAndCacheInstalledPackages(context)
//            }
//        }
    }

//    private fun updateAndCacheInstalledPackages(context: Context) {
//        runCatching {
//            loadAllInstalledPackages(context) {
//                sCacheInstallPackages.addAll(it)
//                mmkv.encode(
//                    "spin_ins_packs", Moshi.Builder()
//                        .build()
//                        .adapter(MInstalledPackages::class.java)
//                        .toJson(MInstalledPackages(it))
//                )
//            }
//        }
//    }

//    private fun getCacheInstalledPackages(): MInstalledPackages {
//        return runCatching {
//            Moshi.Builder()
//                .build()
//                .adapter(MInstalledPackages::class.java)
//                .fromJson(
//                    mmkv.decodeString("spin_ins_packs") ?: "{}"
//                )
//        }.getOrNull() ?: MInstalledPackages()
//    }

//    private fun loadAllInstalledPackages(context: Context, onResult: (res: List<String>) -> Unit) {
//        val pm = context.packageManager
//        AppUtils.getAppsInfo()
//            .filter {
//                !it.isSystem
//                        && it.packageName.isNotBlank()
//                        && pm.getLaunchIntentForPackage(it.packageName) != null
//            }
//            .map {
//                it.packageName
//            }
//            .apply {
//                onResult(this)
//            }
//    }

//    private fun getAllInstalledPackages(context: Context): List<String> {
//        val cache = getCacheInstalledPackages().packages
//        if (cache.isNotEmpty()) {
//            sCacheInstallPackages.addAll(cache)
//            return cache
//        }
//        if (sCacheInstallPackages.isEmpty()) {
//            updateAndCacheInstalledPackages(context)
//        }
//        return sCacheInstallPackages.toList()
//    }

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