package com.spin.secure.utils

import android.content.Context
import android.util.Base64
import androidx.core.os.bundleOf
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.github.shadowsocks.database.ProfileManager
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.gson.reflect.TypeToken
import com.spin.secure.*
import com.spin.secure.ads.MAd
import com.spin.secure.bean.SpinIpBean
import com.spin.secure.bean.SpinRemoteBean
import com.spin.secure.bean.SpinVpnBean
import com.spin.secure.key.Constant
import com.spin.secure.key.Constant.logTagSpin
import com.spin.secure.main.core.MConnection
import com.spin.secure.main.core.MConnectionLib
import com.spin.secure.main.core.connector.ss.syncFromData
import com.squareup.moshi.Json
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.resource.ResourceUtils
import com.xuexiang.xutil.resource.ResourceUtils.readStringFromAssert
import com.xuexiang.xutil.security.Base64Utils
import kotlinx.coroutines.*

object SpinUtils {
    private var installReferrer: String = ""
    private val job = Job()
    private val timerThread = CoroutineScope(job)

    /**
     * 下发服务器转换
     */
    fun deliverServerTransitions(): Boolean {
        val data = getDataFromTheServer()
        return if (data == null) {
            SpinOkHttpUtils.getDeliverData()
            false
        } else {
            ProfileManager.syncFromData(data)
            true
        }
    }

    /**
     * 获取下发服务器数据
     */
    fun getDataFromTheServer(): List<MConnection>? {
        val data = Constant.SEND_SERVER_DATA.asSpKeyAndExtract()
        return runCatching {
            val spinVpnBean = data.toModelOrNull(SpinVpnBean::class.java)
            if (spinVpnBean?.data?.serverList?.isNotEmpty() == true) {
                spinVpnBean.data.serverList!!.map {
                    MConnection().apply {
                        host = it.ip.toString()
                        smart = false
                        port = it.port ?: 0
                        method = it.method.toString()
                        password = it.password.toString()
                        city = it.city.toString()
                        country = it.country.toString()
                    }
                }.toMutableList()
            } else {
                null
            }
        }.getOrElse {
            null
        }
    }

    fun getDataFastServerData(): List<MConnection>? {
        val data = Constant.SEND_SERVER_DATA.asSpKeyAndExtract()
        return runCatching {
            val spinVpnBean = data.toModelOrNull(SpinVpnBean::class.java)
            if (spinVpnBean?.data?.smartList?.isNotEmpty() == true) {
                spinVpnBean.data.smartList!!.map {
                    MConnection().apply {
                        host = it.ip.toString()
                        smart = false
                        port = it.port ?: 0
                        method = it.method.toString()
                        password = it.password.toString()
                        city = it.city.toString()
                        country = it.country.toString()
                    }
                }.toMutableList()
            } else {
                null
            }
        }.getOrElse {
            null
        }
    }

    fun referrer(
        context: Context,
    ) {
        installReferrer = "gclid"
//        installReferrer = "fb4a"
        getAppMmkv().encode(Constant.INSTALL_REFERRER, installReferrer)
        try {
            val referrerClient = InstallReferrerClient.newBuilder(context).build()
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(p0: Int) {
                    when (p0) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            if (!getAppMmkv().decodeBool(Constant.INSTALL_TYPE_SPIN)) {
                                runCatching {
                                    referrerClient?.installReferrer?.run {
                                        SpinOkHttpUtils.postInstallEvent(context, this)
                                    }
                                }.exceptionOrNull()
                            }
                            installReferrer =
                                referrerClient.installReferrer.installReferrer ?: ""
//                            getAppMmkv().encode(Constant.INSTALL_REFERRER, installReferrer)
                            KLog.e("TAG", "installReferrer====${installReferrer}")
                            referrerClient.endConnection()
                            return
                        }
                        else -> {
                            referrerClient.endConnection()
                        }
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                }
            })
        } catch (e: Exception) {
        }
    }

    fun isFacebookUser(): Boolean {
        val referrer = Constant.INSTALL_REFERRER.asSpKeyAndExtract()
        return referrer.contains("fb4a", true)
                || referrer.contains("facebook", true)
    }

    fun isValuableUser(): Boolean {
        val referrer = Constant.INSTALL_REFERRER.asSpKeyAndExtract()
        KLog.e("state", "referrer==${referrer}")
        return isFacebookUser()
                || referrer.contains("gclid", true)
                || referrer.contains("not%20set", true)
                || referrer.contains("youtubeads", true)
                || referrer.contains("%7B%22", true)
    }

    /**
     * 获取方案配置
     */
    fun getScenarioConfiguration(): SpinRemoteBean {
        val listType = object : TypeToken<SpinRemoteBean>() {}.type
        val data = Constant.spin_config.asSpKeyAndExtract()
        return runCatching {
            JsonUtil.fromJson<SpinRemoteBean>(
                data,
                listType
            )
        }.getOrNull() ?: JsonUtil.fromJson(
            readStringFromAssert(Constant.VPN_BOOT_LOCAL_FILE_NAME_SPIN),
            object : TypeToken<SpinRemoteBean?>() {}.type
        )
    }

    /**
     * 加载前链接信息设置
     */
    fun beforeLoadLinkSettingsBa(mAd: MAd): MAd {
        val ipAfterVpnLink = Constant.IP_AFTER_VPN_LINK_SPIN.asSpKeyAndExtract()
        val ipAfterVpnCity = Constant.IP_AFTER_VPN_CITY_SPIN.asSpKeyAndExtract()
        if (SpinApp.isVpnGlobalLink) {
            mAd.spin_load_ip = ipAfterVpnLink
            mAd.spin_load_city = ipAfterVpnCity
        } else {
            val ip = getIpBean().ip.toString()
            mAd.spin_load_ip = ip
            mAd.spin_load_city = "null"
        }
        return mAd
    }

    /**
     * 展示链接信息设置
     */
    fun afterLoadLinkSettingsBa(mAd: MAd): MAd {
        val ipAfterVpnLink = Constant.IP_AFTER_VPN_LINK_SPIN.asSpKeyAndExtract()
        val ipAfterVpnCity = Constant.IP_AFTER_VPN_CITY_SPIN.asSpKeyAndExtract()
        if (SpinApp.isVpnGlobalLink) {
            mAd.spin_show_ip = ipAfterVpnLink
            mAd.spin_show_city = ipAfterVpnCity
        } else {
            val ip = getIpBean().ip.toString()
            KLog.e("TAG", "getIpBean().ip${ip}")
            mAd.spin_show_ip = ip
            mAd.spin_show_city = "null"
        }
        return mAd
    }

    /**
     * 获取Ip Bean
     */
    fun getIpBean(): SpinIpBean {
        val ip = (Constant.CURRENT_IP_SPIN).asSpKeyAndExtract()
        val baIpBean = SpinIpBean()
        baIpBean.ip = ip
        return baIpBean

    }


    /**
     * 是否屏蔽插屏广告
     */
    fun isBlockScreenAds(spinRef: String): Boolean {
        when (spinRef) {
            "1" -> {
                return true
            }
            "2" -> {
                return isValuableUser()
            }
            "3" -> {
                return isFacebookUser()
            }
            "4" -> {
                return false
            }
            else -> {
                return true
            }
        }
    }

    /**
     * 埋点
     */
    fun toBuriedPointSpin(name: String) {
        if (!BuildConfig.DEBUG) {
            Firebase.analytics.logEvent(name, null)
        } else {
            KLog.d(logTagSpin, "触发埋点----name=${name}")
        }
    }

    /**
     * 埋点用户
     */
    fun toBuriedPointUserTypeSpin(name: String, value: String) {
        if (!BuildConfig.DEBUG) {
            Firebase.analytics.setUserProperty(name, value)
        } else {
            KLog.d(logTagSpin, "触发埋点----name=${name}-----value=${value}")
        }
    }

    /**
     * 埋点连接时长
     */
    fun toBuriedPointConnectionTimeSpin(name: String, time: Int) {
        if (!BuildConfig.DEBUG) {
            Firebase.analytics.logEvent(name, bundleOf("time" to time))
        } else {
            KLog.d(logTagSpin, "触发埋点----name=${name}---time=${time}")
        }
    }

    /**
     * 下发结果解码
     */
    fun sendResultDecoding(str: String): String {
        val processedStr =
            str.filterIndexed { index, _ -> index == 0 || (index > 0 && (index + 1) % 3 != 0) }
        KLog.e("base", "Processed string: $processedStr")
        val decodedBytes =
            Base64.decode(processedStr.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        return decodedBytes.toString(Charsets.UTF_8)
    }

    fun splitIntoFour(input: String): String {
        val chunkSize = input.length / 4
        val parts = mutableListOf<String>()
        for (i in 0..3) {
            val startIndex = i * chunkSize
            val endIndex = if (i == 3) input.length else (i + 1) * chunkSize
            parts.add(input.substring(startIndex, endIndex).dropLast(5))
        }
        return encodeWithBase64(parts)
    }

    fun encodeWithBase64(parts: List<String>): String {
        val combined = parts.joinToString("")
        return Base64Utils.decode(combined, "UTF-8")
    }
}