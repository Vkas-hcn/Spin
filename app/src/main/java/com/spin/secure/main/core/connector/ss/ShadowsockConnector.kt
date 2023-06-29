package com.spin.secure.main.core.connector.ss

import android.app.Application
import androidx.activity.ComponentActivity
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.DataStore
import com.spin.secure.SpinApp
import com.spin.secure.ads.AdLoadUtils
import com.spin.secure.getAppMmkv
import com.spin.secure.key.Constant
import com.spin.secure.main.SpinActivity
import com.spin.secure.main.core.ConnectState
import com.spin.secure.main.core.ConnectionRepository
import com.spin.secure.main.core.MConnection
import com.spin.secure.main.core.connector.BaseConnector
import com.spin.secure.runOnMainProgress
import com.spin.secure.utils.KLog
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.tip.ToastUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShadowsockConnector(context: ComponentActivity) : BaseConnector(context),
    ShadowsocksConnection.Callback {
    companion object {
        fun initialize(app: Application) {
            Core.init(app, null)
            runOnMainProgress {
                ConnectionRepository().listAll()?.let { ProfileManager.syncFromData(it) }
            }
        }
    }

    private val shadowsocksConnection = ShadowsocksConnection(true)
     var shadowsockState = BaseService.State.Idle

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {
        shadowsockState = state
        KLog.e("logTagSpin","连接状态=${state.name}")
        if (state.name == BaseService.State.Connected.name){
            SpinApp.isVpnGlobalLink = true
            if (!SpinActivity.whetherToImplementPlanA) {
                KLog.e("logTagSpin","清空加载")
                AdLoadUtils.loadAllAd()
                SpinActivity.whetherToImplementPlanA = true
            }
        }
        if(state.name == BaseService.State.Stopped.name){
            SpinApp.isVpnGlobalLink = false
        }
    }

    override fun onServiceConnected(service: IShadowsocksService) {
        val stat = BaseService.State.values()[service.state]
        KLog.e(Constant.logTagSpin,"onServiceConnected=${stat.name}")
        shadowsockState = stat
        if (stat == BaseService.State.Connected) {
            state = ConnectState.Connected
            onConnected()
        } else if (stat == BaseService.State.Stopped) {
            state = ConnectState.Stopped
            resetTimer()
        }
        serviceStateCallback?.invoke(true)
    }

    override fun onServiceDisconnected() {
        super.onServiceDisconnected()
        state = ConnectState.Stopped
        resetTimer()
        serviceStateCallback?.invoke(false)
    }

    override fun onBinderDied() {
        super.onBinderDied()
        shadowsocksConnection.disconnect(context)
        shadowsocksConnection.connect(context, this)
    }

    override fun initConnection() {
        shadowsocksConnection.connect(context, this)
    }

    override fun destroyConnection() {
        shadowsocksConnection.disconnect(context)
    }

    override fun updateBandwidthTimeout(bandwidthTimeout: Long) {
        shadowsocksConnection.bandwidthTimeout = bandwidthTimeout
    }

    override fun doStart(data: MConnection?) {
        if (data == null) return
        getAppMmkv()
            .encode(Constant.IP_AFTER_VPN_LINK_SPIN, data.host)
        getAppMmkv()
            .encode(Constant.IP_AFTER_VPN_CITY_SPIN, data.city)
        DataStore.profileId = data.toProfile().getDataId()
        GlobalScope.launch {
            delay(1000)
            KLog.e("logTagSpin","开始连接")
            Core.startService()
        }
    }

    override fun doStop() {
        KLog.e("logTagSpin","开始断开")

        Core.stopService()
    }

    override fun isConnected(): Boolean {
        return shadowsockState == BaseService.State.Connected
    }
}