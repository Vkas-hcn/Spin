package com.spin.secure.utils

import com.spin.secure.main.core.MConnection
import org.json.JSONObject

import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.system.DeviceUtils
import com.xuexiang.xutil.app.AppUtils
import com.xuexiang.xutil.data.DateUtils
import com.xuexiang.xutil.display.ScreenUtils
import com.xuexiang.xutil.net.NetworkUtils
import java.util.*
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.os.BatteryManager
import android.os.Build
import android.telephony.TelephonyManager
import android.webkit.WebSettings
import com.android.installreferrer.api.ReferrerDetails
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.RomUtils
import com.blankj.utilcode.util.ThreadUtils.runOnUiThread
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.gson.reflect.TypeToken
import com.spin.secure.ads.MAd
import com.spin.secure.asSpKeyAndExtract
import com.spin.secure.getAppMmkv
import com.spin.secure.key.Constant

object SpinTbaUtils {
    /**
     * 顶层json
     */
    private fun getTopLevelJsonData(isAd: Boolean = false, baDetailBean: MAd? = null): JSONObject {
        val jsonData = JSONObject()
        when {
            isAd -> {
                val bubbleLoadCity = baDetailBean?.spin_load_city ?: "null"
                val bubbleShowCity = baDetailBean?.spin_show_city ?: "null"
                jsonData.put("r_city~connally", bubbleLoadCity)
                jsonData.put("s_city~connally", bubbleShowCity)
            }
        }
        val brinkData = JSONObject().apply {
            put("huxtable", (RomUtils.getRomInfo().version) ?: "")
            put("dodson", AppUtils.getAppPackageName())
            put(
                "endoderm",
                NetworkUtils.getNetStateType().toString().replaceFirst("NET_", "")
                    .lowercase(Locale.getDefault())
            )


            put("toolkit", Locale.getDefault().country)
            put("bighorn", "${Locale.getDefault().language}_${Locale.getDefault().country}")
            put("cot", DateUtils.getNowMills())
            put("cometh", "rove")
            put("asthma", AppUtils.getAppVersionName())
            put("hecuba", DeviceUtils.getDeviceModel())
            put("barrage", DeviceUtils.getManufacturer())
            put("nobody", "")
            put("mcintosh", whetherItIsCharging(XUtil.getContext()))
            put("gannett", DeviceUtils.getAndroidID())
            put("indigene", TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000)
            put("hate", Constant.GOOGLE_ADVERTISING_ID_SPIN.asSpKeyAndExtract())
            put("forest", UUID.randomUUID().toString())
            put("lovelorn", Constant.UUID_VALUE_SPIN.asSpKeyAndExtract())
            put("model", Constant.CURRENT_IP_SPIN.asSpKeyAndExtract())
            put("keyboard", DeviceUtils.getSDKVersionName())
            put("coulomb", com.blankj.utilcode.util.NetworkUtils.getNetworkOperatorName())
            put("quark", Constant.UUID_VALUE_SPIN.asSpKeyAndExtract())

        }

        jsonData.put("brink", brinkData)

        return jsonData
    }


    fun getSessionJson(): String {
        val topLevelJson = getTopLevelJsonData()
        val sessionJson = JSONObject().apply {
            put("raritan", JSONObject())
        }
        topLevelJson.put("raritan", sessionJson)
        return topLevelJson.toString()
    }

    fun getAdJson(
        adValue: AdValue,
        responseInfo: ResponseInfo,
        ufDetailBean: MAd?,
        adType: String,
        adKey: String
    ): String {
        val adJson = JSONObject().apply {
            put("macassar", ufDetailBean?.spin_load_ip ?: "") // ad_load_ip
            put("emanuel", ufDetailBean?.spin_show_ip ?: "") // ad_impression_ip
            put("beam", adValue.valueMicros) // ad_pre_ecpm
            put("dupont", adType) // ad_format
            put("antonym", getPrecisionType(adValue.precisionType)) // precision_type
            put("sachem", adValue.currencyCode) // currency
            put("tablet", null) // ad_rit_id
            put("tithing", null) // ad_sense
            put("embed", responseInfo.mediationAdapterClassName) // ad_network
            put("ambulant", "admob") // ad_source
            put("malaria", ufDetailBean?.dataId) // ad_code_id
            put("cobb", adKey) // ad_pos_id


        }

        return getTopLevelJsonData(true, ufDetailBean).apply {
            put("sweater", adJson)
        }.toString()
    }

    fun install(context: Context, referrerDetails: ReferrerDetails): String {
        return getTopLevelJsonData().apply {
            put("quota", "ebony")
            //build
            put("chive", "build/${Build.ID}")

            //referrer_url
            put("connote", referrerDetails.installReferrer)

            //install_version
            put("dolly", referrerDetails.installVersion)

            //user_agent
            put("bunyan", getMyDefaultUserAgent(context))

            //lat
            put("stylus", getLimitTracking(context))

            //referrer_click_timestamp_seconds
            put("rag", referrerDetails.referrerClickTimestampSeconds)

            //install_begin_timestamp_seconds
            put("examine", referrerDetails.installBeginTimestampSeconds)

            //referrer_click_timestamp_server_seconds
            put("dreg", referrerDetails.referrerClickTimestampServerSeconds)

            //install_begin_timestamp_server_seconds
            put("pauline", referrerDetails.installBeginTimestampServerSeconds)

            //install_first_seconds
            put("cosmic", timeTheAppWasFirstInstalled(context))

            //last_update_seconds
            put("grisly", timeLastUpdateWasApplied(context))

            //google_play_instant
            put("crowley", referrerDetails.googlePlayInstantParam)
        }.toString()
    }

    /**
     * cloak
     */
    fun cloakJson(): Map<String, Any> {
        return mapOf(
            "pent" to Constant.GOOGLE_ADVERTISING_ID_SPIN.asSpKeyAndExtract(), // idfv
            "hate" to DeviceUtils.getAndroidID(), // gaid
            "lovelorn" to Constant.UUID_VALUE_SPIN.asSpKeyAndExtract(), // distinct_id
            "model" to Constant.CURRENT_IP_SPIN.asSpKeyAndExtract(), // ip
            "coulomb" to com.blankj.utilcode.util.NetworkUtils.getNetworkOperatorName(), // operator
            "cot" to DateUtils.getNowMills(), // client_ts
            "cometh" to AppUtils.getAppVersionName(), // os
            "asthma" to AppUtils.getAppVersionName(), // app_version
            "hecuba" to DeviceUtils.getDeviceModel(), // device_model
            "dodson" to AppUtils.getAppPackageName(), // bundle_id
            "huxtable" to (RomUtils.getRomInfo().version ?: ""), // os_version
            "gannett" to "rinse", // android_id
            "quark" to Constant.UUID_VALUE_SPIN.asSpKeyAndExtract(), // key
        )
    }


    /**
     * 获取IP地址（https://ifconfig.me/ip）
     */
   suspend fun obtainIpAddress() {
        SpinOkHttpUtils.getCurrentIp()
        SpinUtils.getIpInformation()
    }

    /**
     * 获取Google广告ID
     */
    fun obtainGoogleAdvertisingId(activity: Activity) {
        runCatching {
            val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(activity)
            getAppMmkv().encode(Constant.GOOGLE_ADVERTISING_ID_SPIN, adInfo.id)
            KLog.e("TBA", "googleAdId---->${adInfo.id}")
        }.getOrNull()
    }

    /**
     * 获取用户是否启用了限制跟踪(IO使用)
     */
    private fun getLimitTracking(context: Context): String {
        return try {
            val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
            when{
                adInfo.isLimitAdTrackingEnabled->{
                    "hobbs"
                }
                else ->{
                    "sharpe"
                }
            }
        } catch (e: Exception) {
            "sharpe"
        }
    }



    /**
     * 应用首次安装的时间
     */
    private fun timeTheAppWasFirstInstalled(context: Context): Long {
        val packageInfo = getPackageInfo(context)
        val installTimeInMillis = getFirstInstallTime(packageInfo)
        return convertMillisToSeconds(installTimeInMillis)
    }
    /**
     * 应用最后一次更新的时间
     */
    private fun timeLastUpdateWasApplied(context: Context): Long {
        val packageInfo = getPackageInfo(context)
        val updateTimeInMillis = getLastUpdateTime(packageInfo)
        return convertMillisToSeconds(updateTimeInMillis)
    }

    private fun getPackageInfo(context: Context): PackageInfo {
        return context.packageManager.getPackageInfo(context.packageName, 0)
    }

    private fun getFirstInstallTime(packageInfo: PackageInfo): Long {
        return packageInfo.firstInstallTime
    }

    private fun getLastUpdateTime(packageInfo: PackageInfo): Long {
        return packageInfo.lastUpdateTime
    }

    private fun convertMillisToSeconds(timeInMillis: Long): Long {
        return timeInMillis / 1000L
    }



    /**
     * precisionType索引
     */
    private fun getPrecisionType(precisionType: Int): String {
        val precisionTypeMap = mapOf(
            0 to "UNKNOWN",
            1 to "ESTIMATED",
            2 to "PUBLISHER_PROVIDED",
            3 to "PRECISE"
        )
        return precisionTypeMap.getOrElse(precisionType) { "UNKNOWN" }
    }

        /**
     * 获取getDefaultUserAgent值
     */
    private fun getMyDefaultUserAgent(context: Context): String {
        return try {
            WebSettings.getDefaultUserAgent(context)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 是否在充电
     */
    fun whetherItIsCharging(context: Context): Boolean {
        val batteryStatus = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            .let { filter -> context.registerReceiver(null, filter) }
            ?: return false

        val chargingStatus = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return chargingStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                chargingStatus == BatteryManager.BATTERY_STATUS_FULL
    }

}