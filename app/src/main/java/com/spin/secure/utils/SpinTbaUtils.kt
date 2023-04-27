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
    private fun getTopLevelJsonData(
        isAd: Boolean = false,
        baDetailBean: MAd? = null
    ): JSONObject {
        return JSONObject().apply {
            if (isAd) {
                val bubbleLoadCity = baDetailBean?.spin_load_city ?: "null"
                val bubbleShowCity = baDetailBean?.spin_show_city ?: "null"
                put("r_city~connally", bubbleLoadCity)
                put("s_city~connally", bubbleShowCity)
            }
            put("brink", JSONObject().apply {
                //network_type
                put(
                    "endoderm",
                    NetworkUtils.getNetStateType().toString().replaceFirst("NET_", "")
                        .lowercase(Locale.getDefault())
                )//网络类型：wifi，3g等，非必须，和产品确认是否需要分析网络类型相关的信息，此参数可能需要系统权限
                //os_version
                put("huxtable", (RomUtils.getRomInfo().version) ?: "")//操作系统版本号
                //bundle_id
                put("dodson", AppUtils.getAppPackageName())//当前的包名称，a.b.c
                //os
                put("cometh", "rove")//操作系统；映射关系：{“rove”: “android”, “lusty”: “ios”, “cluj”: “web”}
                //app_version
                put("asthma", AppUtils.getAppVersionName())//应用的版本
                //os_country
                put("toolkit", Locale.getDefault().country)//操作系统中的国家简写，例如 CN，US等
                //system_language
                put(
                    "bighorn",
                    "${Locale.getDefault().language}_${Locale.getDefault().country}"
                )// String locale = Locale.getDefault(); 拼接为：zh_CN的形式，下杠
                //client_ts
                put("cot", DateUtils.getNowMills())//日志发生的客户端时间，毫秒数
                //device_model
                put("hecuba", DeviceUtils.getDeviceModel())//手机型号
                //distinct_id
                put("lovelorn", Constant.UUID_VALUE_SPIN.asSpKeyAndExtract())//用户排重字段
                //ip
                put("model", Constant.CURRENT_IP_SPIN.asSpKeyAndExtract())
                //sdk_ver
                put("keyboard", DeviceUtils.getSDKVersionName())//安卓sdk版本号，数字
                //zone_offset
                put(
                    "indigene",
                    TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000
                )//客户端时区

                //gaid
                put("hate", Constant.GOOGLE_ADVERTISING_ID_SPIN.asSpKeyAndExtract())//原值，google广告id
                //log_id
                put("forest", UUID.randomUUID().toString())
                //operator
                put(
                    "coulomb",
                    com.blankj.utilcode.util.NetworkUtils.getNetworkOperatorName()
                )//网络供应商名称
                //key
                put("quark", Constant.UUID_VALUE_SPIN.asSpKeyAndExtract())//随机生成的uuid

                //battery_status
                put("mcintosh", isCharging(XUtil.getContext()))
                //android_id
                put("gannett", DeviceUtils.getAndroidID())
                //manufacturer
                put("barrage", DeviceUtils.getManufacturer())//手机厂商，huawei、oppo
                //brand
                put("nobody", "")//品牌

            })
        }
    }

    fun getSessionJson(): String {
        return getTopLevelJsonData().apply {
            put("raritan", JSONObject().apply{})
        }.toString()
    }

    fun getAdJson(
        adValue: AdValue,
        responseInfo: ResponseInfo,
        ufDetailBean: MAd?,
        adType: String,
        adKey: String
    ): String {
        return getTopLevelJsonData(true, ufDetailBean).apply {
            put("sweater", JSONObject().apply {
                //ad_pre_ecpm
                put("beam", adValue.valueMicros)//价格
                //currency
                put("sachem", adValue.currencyCode)//预估收益的货币单位
                //ad_network
                put(
                    "embed",
                    responseInfo.mediationAdapterClassName
                )//广告网络，广告真实的填充平台，例如admob的bidding，填充了Facebook的广告，此值为Facebook
                //ad_source
                put("ambulant", "admob")
                //ad_code_id
                put("malaria", ufDetailBean?.dataId)
                //ad_pos_id
                put("cobb", adKey)
                //ad_rit_id
                put("tablet", null)
                //ad_sense
                put("tithing", null)
                //ad_format
                put("dupont", adType)
                //precision_type
                put("antonym", getPrecisionType(adValue.precisionType))
                //ad_load_ip
                put("macassar", ufDetailBean?.spin_load_ip ?: "")
                //ad_impression_ip
                put("emanuel", ufDetailBean?.spin_show_ip ?: "")
//                //ad_sdk_ver
//                put("office", responseInfo.responseId)
            })
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
        return mapOf<String, Any>(
            //distinct_id
            "lovelorn" to (Constant.UUID_VALUE_SPIN.asSpKeyAndExtract()),
            //client_ts
            "cot" to  (DateUtils.getNowMills()),//日志发生的客户端时间，毫秒数
            //device_model
            "hecuba" to DeviceUtils.getDeviceModel(),
            //bundle_id
            "dodson" to (AppUtils.getAppPackageName()),//当前的包名称，a.b.c
            //os_version
            "huxtable" to RomUtils.getRomInfo().version,
            //idfv
            "pent" to (Constant.GOOGLE_ADVERTISING_ID_SPIN.asSpKeyAndExtract()),
            //gaid
            "hate" to DeviceUtils.getAndroidID(),
            //android_id
            "gannett" to "rinse",
            //os
            "cometh" to AppUtils.getAppVersionName(),
            //app_version
            "asthma" to AppUtils.getAppVersionName(),//应用的版本
            //key
            "quark" to (Constant.UUID_VALUE_SPIN.asSpKeyAndExtract()),//随机生成的uuid
            //ip
            "model" to (Constant.CURRENT_IP_SPIN.asSpKeyAndExtract()),
            //operator
            "coulomb" to (com.blankj.utilcode.util.NetworkUtils.getNetworkOperatorName())//网络供应商名称
        )
    }

    /**
     * 获取IP地址（https://ifconfig.me/ip）
     */
    fun obtainIpAddress() {
        SpinOkHttpUtils.getCurrentIp()
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

    fun getNetworkOperator(context: Context): String {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return telephonyManager.networkOperatorName
    }

    /**
     * 获取用户是否启用了限制跟踪(IO使用)
     */
    private fun getLimitTracking(context: Context): String {
        return try {
            if (AdvertisingIdClient.getAdvertisingIdInfo(context).isLimitAdTrackingEnabled) {
                "hobbs"
            } else {
                "sharpe"
            }
        } catch (e: Exception) {
            "sharpe"
        }
    }

    /**
     * 应用首次安装的时间
     */
    private fun timeTheAppWasFirstInstalled(context: Context): Long {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return (packageInfo.firstInstallTime / 1000L)
    }

    /**
     * 应用最后一次更新的时间
     */
    private fun timeLastUpdateWasApplied(context: Context): Long {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return (packageInfo.lastUpdateTime / 100L)
    }

    /**
     * precisionType索引
     */
    private fun getPrecisionType(precisionType: Int): String {
        return when (precisionType) {
            0 -> {
                "UNKNOWN"
            }
            1 -> {
                "ESTIMATED"
            }
            2 -> {
                "PUBLISHER_PROVIDED"
            }
            3 -> {
                "PRECISE"
            }
            else -> {
                "UNKNOWN"
            }
        }
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
     * 剩余电量
     */
    private fun getBatteryLevel(context: Context): Int {
        val batteryLevel: Int
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            ifilter.addAction(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(null, ifilter)
        }
        batteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        return batteryLevel
    }

    /**
     * 是否在充电
     */
    fun isCharging(context: Context): Boolean {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)
        val chargingStatus = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return chargingStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                chargingStatus == BatteryManager.BATTERY_STATUS_FULL
    }
}