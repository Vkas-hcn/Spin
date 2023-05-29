package com.spin.secure.utils

import android.content.Context
import com.android.installreferrer.api.ReferrerDetails
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.ResponseInfo
import com.spin.secure.BuildConfig
import com.spin.secure.ads.MAd
import com.spin.secure.getAppMmkv
import com.spin.secure.key.Constant
import com.spin.secure.net.NewHttpClient
import com.spin.secure.tryOkHttp
import com.xuexiang.xutil.app.AppUtils
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.system.DeviceUtils
import kotlinx.coroutines.runBlocking

object SpinOkHttpUtils {
    private var urlService = if (BuildConfig.DEBUG) {
        Constant.SERVER_DISTRIBUTION_ADDRESS_TEST_SPIN
    } else {
        Constant.SERVER_DISTRIBUTION_ADDRESS_SPIN
    }

    var urlTba = if (BuildConfig.DEBUG) {
        Constant.TBA_ADDRESS_TEST_SPIN
    } else {
        Constant.TBA_ADDRESS_SPIN
    }

    fun getCurrentIp() {
        tryOkHttp("IP--code Exception"){
            runBlocking {
                val response = NewHttpClient.get("https://ifconfig.me/ip")
                if (response.statusCode == 200) {
                    KLog.e("TAG", "IP----->${response.body}")
                    getAppMmkv().encode(Constant.CURRENT_IP_SPIN, response.body.toString())
                } else {
                    KLog.e("TAG", "IP--code Exception")
                    getAppMmkv().encode(Constant.CURRENT_IP_SPIN, "")
                }
            }
        }
    }

    /**
     * session事件上报
     */
    fun postSessionEvent() {
        val json = SpinTbaUtils.getSessionJson()
        KLog.e("TBA", "json--session-->${json}")
        tryOkHttp("session事件上报-Exception") {
            runBlocking {
                val response = NewHttpClient.post(urlTba, json)
                if (response.statusCode == 200) {
                    KLog.e("TBA", "session事件上报-成功->")

                } else {
                    getAppMmkv().encode(Constant.SESSION_JSON_SPIN, json)
                    KLog.e("TBA", "session事件上报-失败-->")
                }
            }
        }
    }


    /**
     * install事件上报
     */
    fun postInstallEvent(context: Context, referrerDetails: ReferrerDetails) {
        val json = SpinTbaUtils.install(context, referrerDetails)
        KLog.e("TBA", "json-install--->${json}")
        tryOkHttp("install事件上报-Exception") {
            runBlocking {
                val response = NewHttpClient.post(urlTba, json)
                if (response.statusCode == 200) {
                    KLog.e("TBA", "install事件上报-成功->")
                    getAppMmkv().encode(Constant.INSTALL_TYPE_SPIN, true)
                } else {
                    getAppMmkv().encode(Constant.INSTALL_TYPE_SPIN, false)
                    KLog.e("TBA", "install事件上报-失败-->")
                }
            }
        }
    }

    /**
     * 广告事件上报
     */
    fun postAdEvent(
        adValue: AdValue,
        responseInfo: ResponseInfo,
        baDetailBean: MAd?,
        adType: String,
        adKey: String,
    ) {
        val json = SpinTbaUtils.getAdJson(adValue, responseInfo, baDetailBean, adType, adKey)
        KLog.e("TBA", "json-Ad---$adKey---->${json}")

        tryOkHttp("${adType}广告事件上报-Exception") {
            runBlocking {
                val response = NewHttpClient.post(urlTba, json)
                if (response.statusCode == 200) {
                    KLog.e("TBA", "${adKey}广告事件上报-成功->")

                } else {
                    getAppMmkv().encode(Constant.INSTALL_TYPE_SPIN, false)
                    KLog.e("TBA", "${adType}广告事件上报-失败--")
                }
            }
        }

    }

    /**
     * Cloak接入，获取黑名单
     */
    fun getBlacklistData() {
        if (BuildConfig.DEBUG) {
            return
        }
        val params = SpinTbaUtils.cloakJson()
        tryOkHttp("Cloak接入-Exception") {
            runBlocking {
                val response = NewHttpClient.getParams(urlTba, params)
                if (response.statusCode == 200) {
                    KLog.e("TBA", "Cloak接入--成功--->${response.body}")
                    if (response.body == "pamela") {
                        getAppMmkv().encode(Constant.BLACKLIST_USER_SPIN, true)
                    } else {
                        getAppMmkv().encode(Constant.BLACKLIST_USER_SPIN, false)
                    }

                } else {
                    KLog.e("TBA", "Cloak接入--失败-- ${response.body}")
                    getAppMmkv().encode(Constant.BLACKLIST_USER_SPIN, true)
                }
            }
        }
    }

    /**
     * 获取下发数据
     */
    fun getDeliverData() {
        tryOkHttp("获取下发服务器数据-Exception") {
            runBlocking {
                val response = NewHttpClient.get(urlService)
                if (response.statusCode == 200) {
                    val fastData = SpinUtils.splitIntoFour(response.body as String)
                    getAppMmkv().encode(Constant.SEND_SERVER_DATA, fastData)
                    KLog.e("TBA", "获取下发服务器数据-成功->${fastData}")
                    val date = SpinUtils.getDataFromTheServer()
                    KLog.e("TBA", "获取下发服务器数据-成功-date>${JsonUtil.toJson(date)}")
                } else {
                    getAppMmkv().encode(Constant.SEND_SERVER_DATA, "")
                    KLog.e("TBA", "获取下发服务器数据-失败->${response.body}")
                }
            }
        }
    }

    /**
     * 心跳上报
     */
     fun getHeartbeatReporting(disaster: String, ss_ip: String) {
        //包名
        val halyards = AppUtils.getAppPackageName()
        // 版本号
        val action = AppUtils.getAppVersionName()
        //设备ID
        val mother = DeviceUtils.getAndroidID()
        //协议名称
        val windlasses = "SS"
        val urlParams =
            "https://${ss_ip}/qwq/sfg/?halyards=${halyards}&action=${action}&mother=$mother&disaster=$disaster&windlasses=$windlasses"

        KLog.e("TBA", "心跳上报---urlParams=${urlParams}")
        try {
            runBlocking {
                val response = NewHttpClient.get(urlParams)
                if (response.statusCode == 200) {
                    KLog.e("TBA", "心跳上报-成功->${response.body}")
                } else {
                    KLog.e("TBA", "心跳上报-失败->${response.body}")
                }
            }
        } catch (e: Exception) {
            KLog.e("TBA", "心跳上报---Exception=${e}")
        }
    }
}