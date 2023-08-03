package com.spin.secure.utils

import android.content.Context
import android.util.Base64
import androidx.core.os.bundleOf
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.facebook.appevents.AppEventsLogger
import com.github.shadowsocks.database.ProfileManager
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.gson.reflect.TypeToken
import com.spin.secure.*
import com.spin.secure.ads.AdLoadUtils
import com.spin.secure.ads.AdsCons
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
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object SpinUtils {
    private var installReferrer: String = ""
    private val job = Job()

    /**
     * 下发服务器转换
     */
    suspend fun deliverServerTransitions(): Boolean {
        val data = getDataFromTheServer()
        val dataFast = getDataFastServerData()

        return if (data == null) {
            SpinOkHttpUtils.getDeliverData()
            false
        } else {
            ProfileManager.syncFromData(data)
            dataFast?.let { ProfileManager.syncFromData(it) }
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
                KLog.e("TAG", "获取下发服务器数据----->")
                null
            }
        }.getOrElse {
            KLog.e("TAG", "获取下发服务器数据----->")
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
                KLog.e("TAG", "获取下发Fast服务器数据----->")
                null

            }
        }.getOrElse {
            KLog.e("TAG", "获取下发Fast服务器数据----->")
            null
        }
    }

    fun referrer(
        context: Context,
    ) {
//        installReferrer = "gclid"
//        installReferrer = "fb4a"
//        getAppMmkv().encode(Constant.INSTALL_REFERRER, installReferrer)
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
                            getAppMmkv().encode(Constant.INSTALL_REFERRER, installReferrer)
                            toBuriedPointSpin("spi_sob")
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
            setVpnGlobalLinkSettings(mAd, ipAfterVpnLink, ipAfterVpnCity)
        } else {
            setNonVpnGlobalLinkSettings(mAd)
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
            setVpnGlobalLinkSettingsAfter(mAd, ipAfterVpnLink, ipAfterVpnCity)
        } else {
            setNonVpnGlobalLinkSettingsAfter(mAd)
        }

        return mAd
    }

    private fun setVpnGlobalLinkSettings(mAd: MAd, ipAfterVpnLink: String, ipAfterVpnCity: String) {
        mAd.spin_load_ip = ipAfterVpnLink
        mAd.spin_load_city = ipAfterVpnCity
    }

    private fun setNonVpnGlobalLinkSettings(mAd: MAd) {
        val ip = getIpBean().ip.toString()
        mAd.spin_load_ip = ip
        mAd.spin_load_city = "null"
    }

    private fun setVpnGlobalLinkSettingsAfter(mAd: MAd, ipAfterVpnLink: String, ipAfterVpnCity: String) {
        mAd.spin_show_ip = ipAfterVpnLink
        mAd.spin_show_city = ipAfterVpnCity
    }

    private fun setNonVpnGlobalLinkSettingsAfter(mAd: MAd) {
        val ip = getIpBean().ip.toString()
        mAd.spin_show_ip = ip
        mAd.spin_show_city = "null"
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
        return when (spinRef) {
            "1" -> true
            "2" -> isValuableUser()
            "3" -> isFacebookUser()
            "4" -> false
            else -> true
        }
    }


    /**
     * 埋点
     */
    fun toBuriedPointSpin(name: String) {
        if (BuildConfig.DEBUG) {
            KLog.d(logTagSpin, "触发埋点----name=$name")
            return
        }

        Firebase.analytics.logEvent(name, null)
    }

    /**
     * 埋点连接时长
     */
    fun toBuriedPointConnectionTimeSpin(name: String, time: Int) {
        if (BuildConfig.DEBUG) {
            KLog.d(logTagSpin, "触发埋点----name=$name---time=$time")
            return
        }

        Firebase.analytics.logEvent(name, bundleOf("time" to time))
    }
    /**
     * 埋点广告收益
     */
    fun toBuriedAdvertisingRevenue(name: String, value: Long,context:Context) {
        SpinOkHttpUtils.postAdPointReportingEvent(value,name)
        if (BuildConfig.DEBUG) {
            KLog.d(logTagSpin, "触发埋点----name=$name---value=$value")
            return
        }
        AppEventsLogger.newLogger(context).logPurchase(
            (value /1000000.0).toBigDecimal(),Currency.getInstance("USD"))
        Firebase.analytics.logEvent(name, bundleOf("value" to value))
    }


    /**
     * 下发结果解码
     */
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


    fun getIpInformation() {
        val ipInfo = getIpInfoFromUrl("https://ip.seeip.org/geoip/")
        if (ipInfo.isNotEmpty()) {
            getAppMmkv().encode(AdsCons.ip1, ipInfo)
        } else {
            getAppMmkv().encode(AdsCons.ip1, "")
            getIpInformation2()
        }
    }

    private fun getIpInfoFromUrl(urlString: String): String {
        val sb = StringBuffer()
        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            val code = conn.responseCode
            if (code == 200) {
                val inputStream = conn.inputStream
                val buffer = ByteArray(1024)
                var len: Int
                while (inputStream.read(buffer).also { len = it } != -1) {
                    sb.append(String(buffer, 0, len, Charset.forName("UTF-8")))
                }
                inputStream.close()
                conn.disconnect()
                KLog.e("tab-ip", "sb==$sb")
                return sb.toString()
            } else {
                KLog.e("tab-ip", "code==$code")
            }
        } catch (e: Exception) {
            KLog.e("tab-ip", "Exception==${e.message}")
        }
        return ""
    }

    private fun getIpInformation2() {
        val ipInfo = getIpInfoFromUrl("https://api.myip.com/")
        if (ipInfo.isNotEmpty()) {
            getAppMmkv().encode(AdsCons.ip2, ipInfo)
        } else {
            KLog.e("tab-ip", "Failed to retrieve IP information from the second URL")
        }
    }

    /**
     * 是否根据买量屏蔽
     */
    fun whetherBuyQuantityBan(): Boolean {
        val localVpnBootData = getScenarioConfiguration()
        KLog.e("logTagSpin", "cloak---${localVpnBootData.spin_lock}。。。")
        if (!isBlockScreenAds(localVpnBootData.spin_show)) {
            return true
        }
        return false
    }

    /**
     * 是否根据黑名单屏蔽
     */
    fun whetherBlackListBan(): Boolean {
        val localVpnBootData = getScenarioConfiguration()
        val blacklistUser =
            getAppMmkv().decodeBool(Constant.BLACKLIST_USER_SPIN, true)
        KLog.e("logTagSpin", "cloak---${localVpnBootData.spin_lock}。。。")
        KLog.e("logTagSpin", "blacklist_user---${blacklistUser}。。。")
        if (blacklistUser && localVpnBootData.spin_lock == "1") {
            return true
        }
        return false
    }


    //当日日期
    var adDateSpin = ""


    fun isAppOpenSameDaySpin() {
        adDateSpin = Constant.currentSpinDate.asSpKeyAndExtract()
        if (adDateSpin == "") {
            getAppMmkv().encode(Constant.currentSpinDate, formatDateNow())
        } else {
            if (dateAfterTime(adDateSpin, formatDateNow())) {
                getAppMmkv().encode(Constant.currentSpinDate, formatDateNow())
                getAppMmkv().encode(Constant.clicksSpinCount, 0)
                getAppMmkv().encode(Constant.showSpinCount, 0)
            }
        }
    }


    fun isThresholdReached(): Boolean {
        val clicksCount = Constant.clicksSpinCount.asSpKeyInt()
        val showCount = Constant.showSpinCount.asSpKeyInt()
        if (clicksCount >= AdLoadUtils.getInstData().click_num || showCount >= AdLoadUtils.getInstData().show_num) {
            return true
        }
        return false
    }


    fun recordNumberOfAdDisplaysSpin() {
        var showCount = Constant.showSpinCount.asSpKeyInt()
        showCount++
        getAppMmkv().encode(Constant.showSpinCount, showCount)

    }


    fun recordNumberOfAdClickSpin() {
        var clicksCount = Constant.clicksSpinCount.asSpKeyInt()
        clicksCount++
        getAppMmkv().encode(Constant.clicksSpinCount, clicksCount)
    }


    /**
     * @return 当前日期
     */
    fun formatDateNow(): String? {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        val date = Date()
        return simpleDateFormat.format(date)
    }

    //判断一个时间在另一个时间之后
    fun dateAfterTime(startTime: String?, endTime: String?): Boolean {
        val format = SimpleDateFormat("yyyy-MM-dd")
        try {
            val startDate: Date = format.parse(startTime)
            val endDate: Date = format.parse(endTime)
            val start: Long = startDate.getTime()
            val end: Long = endDate.getTime()
            if (end > start) {
                return true
            }
        } catch (e: ParseException) {
            e.printStackTrace()
            return false
        }
        return false
    }


}