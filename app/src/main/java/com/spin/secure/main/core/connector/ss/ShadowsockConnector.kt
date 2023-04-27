package com.spin.secure.main.core.connector.ss

import android.app.Application
import androidx.activity.ComponentActivity
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.DataStore
import com.spin.secure.getAppMmkv
import com.spin.secure.key.Constant
import com.spin.secure.main.core.ConnectState
import com.spin.secure.main.core.ConnectionRepository
import com.spin.secure.main.core.MConnection
import com.spin.secure.main.core.connector.BaseConnector
import com.spin.secure.runOnMainProgress

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
    private var shadowsockState = BaseService.State.Idle

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {
        shadowsockState = state
    }

    override fun onServiceConnected(service: IShadowsocksService) {
        val stat = BaseService.State.values()[service.state]
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
        Core.startService()
    }

    override fun doStop() {
        Core.stopService()
    }

    override fun isConnected(): Boolean {
        return shadowsockState == BaseService.State.Connected
    }
}