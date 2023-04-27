package com.spin.secure.utils

import android.content.Context
import com.android.installreferrer.api.ReferrerDetails
import com.blankj.utilcode.util.LogUtils
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.ResponseInfo
import com.spin.secure.BuildConfig
import com.spin.secure.ads.MAd
import com.spin.secure.getAppMmkv
import com.spin.secure.key.Constant
import com.spin.secure.main.core.MConnection
import com.spin.secure.net.HttpApi
import com.spin.secure.net.IHttpCallback
import com.spin.secure.net.OkHttpApi
import com.xuexiang.xutil.tip.ToastUtils
import com.xuexiang.xutil.app.AppUtils
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.system.DeviceUtils

object SpinOkHttpUtils {
    private val httpApi: HttpApi = OkHttpApi()
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
        httpApi.get(
            emptyMap(),
            "https://ifconfig.me/ip",
            object : IHttpCallback {
                override fun onSuccess(data: Any?) {
                    LogUtils.d("success result : ${data.toString()}")
                    KLog.e("TAG", "IP----->${data}")
                    getAppMmkv().encode(Constant.CURRENT_IP_SPIN, data.toString())
                }

                override fun onFailed(error: Any?) {
                    KLog.e("TAG", "IP--code Exception")
                    getAppMmkv().encode(Constant.CURRENT_IP_SPIN, "")
                }
            })
    }

    /**
     * session事件上报
     */
    fun postSessionEvent() {
        val json = SpinTbaUtils.getSessionJson()
        KLog.e("TBA", "json--session-->${json}")
        httpApi.post(
            json,
            urlTba,
            object : IHttpCallback {
                override fun onSuccess(data: Any?) {
                    KLog.e("TBA", "session事件上报-成功->")
                }

                override fun onFailed(error: Any?) {
                    getAppMmkv().encode(Constant.SESSION_JSON_SPIN, json)
                    KLog.e("TBA", "session事件上报-失败-->${error}")
                }
            })
    }


    /**
     * install事件上报
     */
    fun postInstallEvent(context: Context, referrerDetails: ReferrerDetails) {
        val json = SpinTbaUtils.install(context, referrerDetails)
        KLog.e("TBA", "json-install--->${json}")
        httpApi.post(
            json,
            urlTba,
            object : IHttpCallback {
                override fun onSuccess(data: Any?) {
                    KLog.e("TBA", "install事件上报-成功->")
                    getAppMmkv().encode(Constant.INSTALL_TYPE_SPIN, true)

                }

                override fun onFailed(error: Any?) {
                    getAppMmkv().encode(Constant.INSTALL_TYPE_SPIN, false)
                    KLog.e("TBA", "install事件上报-失败-->${error}")
                }
            })
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

        httpApi.post(
            json,
            urlTba,
            object : IHttpCallback {
                override fun onSuccess(data: Any?) {
                    KLog.e("TBA", "${adKey}广告事件上报-成功->")
                }

                override fun onFailed(error: Any?) {
                    KLog.e("TBA", "${adType}广告事件上报-失败-->${error}")
                }
            })
    }

    /**
     * Cloak接入，获取黑名单
     */
    fun getBlacklistData() {
        if (BuildConfig.DEBUG) {
            return
        }
        val params = SpinTbaUtils.cloakJson()
        httpApi.get(
            params,
            Constant.cloak_url_SPIN,
            object : IHttpCallback {
                override fun onSuccess(data: Any?) {
                    KLog.e("TBA", "Cloak接入--成功--->${data}")
                    if (data == "pamela") {
                        getAppMmkv().encode(Constant.BLACKLIST_USER_SPIN, true)
                    } else {
                        getAppMmkv().encode(Constant.BLACKLIST_USER_SPIN, false)
                    }
                }

                override fun onFailed(error: Any?) {
                    KLog.e("TBA", "Cloak接入--失败-- $error")
                    getAppMmkv().encode(Constant.BLACKLIST_USER_SPIN, true)
                }
            }, true
        )
    }

    /**
     * 获取下发数据
     */
    fun getDeliverData() {
        httpApi.get(
            mapOf(),
            urlService,
            object : IHttpCallback {
                override fun onSuccess(data: Any?) {
                    val fastData = SpinUtils.splitIntoFour(data as String)
                    getAppMmkv().encode(Constant.SEND_SERVER_DATA, fastData)
                    KLog.e("TBA", "获取下发服务器数据-成功->${fastData}")
                    val date = SpinUtils.getDataFromTheServer()
                    KLog.e("TBA", "获取下发服务器数据-成功-date>${JsonUtil.toJson(date)}")
                }

                override fun onFailed(error: Any?) {
                    getAppMmkv().encode(Constant.SEND_SERVER_DATA, "")
                    KLog.e("TBA", "获取下发服务器数据-失败->${error}")
                }
            })
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
        httpApi.get(
            mapOf(),
            urlParams,
            object : IHttpCallback {
                override fun onSuccess(data: Any?) {
                    KLog.e("TBA", "心跳上报-成功->${data}")
                }

                override fun onFailed(error: Any?) {
                    KLog.e("TBA", "心跳上报-失败->${error}")
                }
            })
    }
}