package com.spin.secure

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.spin.secure.ads.AdLoadUtils
import com.spin.secure.fb.FirebaseConfiguration
import com.spin.secure.key.Constant
import com.spin.secure.main.core.connector.BaseConnector
import com.spin.secure.utils.KLog
import com.tencent.mmkv.MMKV
import com.xuexiang.xutil.XUtil
import java.util.*

class SpinApp : Application(), LifecycleObserver {

    companion object {
        const val DEBUG = true
        lateinit var self: SpinApp
        // 原生广告刷新
        var nativeAdRefreshBa = false
        // VPN是否链接
        var isVpnGlobalLink = false
    }

    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        runOnMainProgress {
            self = this
            FirebaseConfiguration.initAndActive(this)
            AdLoadUtils.init(this)
            XUtil.init(this)
            //是否开启打印日志
            KLog.init(BuildConfig.DEBUG)
        }
        BaseConnector.initialize(this)
        val data = Constant.UUID_VALUE_SPIN.asSpKeyAndExtract()
        if(data.isEmpty()){
            getAppMmkv().encode(Constant.UUID_VALUE_SPIN, UUID.randomUUID().toString())
        }
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        nativeAdRefreshBa =true
    }
}