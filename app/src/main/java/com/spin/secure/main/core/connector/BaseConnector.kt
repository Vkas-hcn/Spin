package com.spin.secure.main.core.connector

import android.app.Application
import android.net.VpnService
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import com.spin.secure.AppScope
import com.spin.secure.ads.AdLoadUtils
import com.spin.secure.asSpKeyAndExtract
import com.spin.secure.forEachRemain
import com.spin.secure.getAppMmkv
import com.spin.secure.key.Constant
import com.spin.secure.main.SpinActivity
import com.spin.secure.main.core.*
import com.spin.secure.main.core.connector.ss.ShadowsockConnector
import com.spin.secure.utils.KLog
import com.spin.secure.utils.SpinOkHttpUtils
import com.spin.secure.utils.SpinUtils
import kotlinx.coroutines.*

abstract class BaseConnector(protected val context: ComponentActivity) {
    private var jobHeart: Job? = null

    interface Callback {
        fun onConnectTimeChanged(time: Long) {}
        fun onStateChanged(state: ConnectState) {}
        fun onConnectionChanged(connection: MConnection) {}
    }

    companion object {
        private val callbacks = mutableSetOf<Callback>()

        fun registerCallback(cb: Callback) {
            callbacks.add(cb)
        }

        fun unregisterCallback(cb: Callback) {
            callbacks.remove(cb)
        }

        var state = ConnectState.Stopped
            set(value) {
                field = value
                callbacks.iterator().forEachRemain { it.onStateChanged(value) }
            }
        var connectData = SmartConnection
            set(value) {
                field = value
                callbacks.iterator().forEachRemain { it.onConnectionChanged(value) }
            }
        var preConnectData = SmartConnection
        private var mConnectTime = 0L
            set(value) {
                field = value
                getAppMmkv().encode("conn_time", value)
                callbacks.iterator().forEachRemain { it.onConnectTimeChanged(value) }
            }
            get() {
                return getAppMmkv().decodeLong("conn_time")
            }
        var preConnectTime = 0L
            private set(value) {
                field = value
                SpinUtils.toBuriedPointConnectionTimeSpin("spin_mie", (value / 1000).toInt())
                getAppMmkv().encode("conn_pre_time", value)
            }
            get() {
                return getAppMmkv().decodeLong("conn_pre_time")
            }
        private var connectTimeJob: Job? = null

         fun startTimer() {
            connectTimeJob?.cancel()
            connectTimeJob = AppScope.launch(Dispatchers.Main) {
                while (true) {
                    delay(1000L)
                    mConnectTime += 1000L
                }
            }
        }

        fun stopTimer(isReset: Boolean) {
            connectTimeJob?.cancel()
            connectTimeJob = null
            if (!isReset) {
                preConnectTime = mConnectTime
            }
            mConnectTime = 0L
        }

        fun initialize(app: Application) {
            ShadowsockConnector.initialize(app)
        }
    }

    private lateinit var permissionRequireContract: ActivityResultLauncher<Void?>
    private var permissionCallback: ((granted: Boolean) -> Unit)? = null
    protected var serviceStateCallback: ((serviceConnected: Boolean) -> Unit)? = null
        private set
    private val connectionRepository = ConnectionRepository()

    init {
        initLifecycle()
    }

    protected fun resetTimer() {
        stopTimer(true)
    }

    fun checkPermission(callback: (granted: Boolean) -> Unit) {
        if (VpnService.prepare(context) == null) {
            callback(true)
            return
        }
        permissionCallback = callback
        permissionRequireContract.launch(null)
    }

    private fun initLifecycle() {
        permissionRequireContract = context.registerForActivityResult(GetVpnPermission()) {
            if (!it) {
                SpinUtils.toBuriedPointSpin("spin_get")
            }
            permissionCallback?.invoke(!it)
            permissionCallback = null
        }
        context.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                initConnection()
            }

            override fun onStart(owner: LifecycleOwner) {
                updateBandwidthTimeout(300L)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                serviceStateCallback = null
                destroyConnection()
            }
        })
    }

    protected abstract fun initConnection()
    protected abstract fun destroyConnection()
    protected abstract fun updateBandwidthTimeout(bandwidthTimeout: Long)

     fun start() {
        val server = connectData
        doStart(
            if (server.smart) connectionRepository.listSmart().randomOrNull()
            else server
        )
    }

    protected abstract fun doStart(data: MConnection?)

     fun stop() {
        doStop()
    }

    protected abstract fun doStop()

    fun canStop(): Boolean = state == ConnectState.Connected

    abstract fun isConnected(): Boolean

    fun setServiceStateCallback(callback: (serviceConnected: Boolean) -> Unit) {
        serviceStateCallback = callback
    }

    fun doToggleConnection(
        forConnect: Boolean,
        onStartResult: (connected: Boolean) -> Unit
    ) {
        if (forConnect) {
            connectServiceInternal(onStartResult)
        } else {
            disconnectServiceInternal(onStartResult)
        }
    }

    private var connectServiceJob: Job? = null

    private fun connectServiceInternal(
        onStartResult: (connected: Boolean) -> Unit
    ) {
        stopTimer(true)
        destroyConnectJob()
        connectServiceJob = context.lifecycleScope.launch {
//            launch(Dispatchers.IO) {
//                start()
//            }
            launch {
                var connectConsumeTime = 0L
                val maxWaitTime = 10000L
                while (connectConsumeTime < maxWaitTime) {
                    delay(500L)
                    connectConsumeTime += 500L
                    if (isConnected()) {
                        connectConsumeTime = maxWaitTime
                    }
                }

                if (isConnected()) {
                    state = ConnectState.Connected
                    onConnected()
                    onStartResult(true)
                    SpinUtils.toBuriedPointSpin("spin_mop")
                    //心跳上报
                    getHeartbeatReportedConnect()
                } else {
                    destroyConnectJob()
                    stop()
                    state = ConnectState.Stopped
                    onDisconnected()
                    SpinUtils.toBuriedPointSpin("spin_mcp")
                    ToastUtils.showShort("Connect fail, please try again!")
                    onStartResult(false)
                }
            }
        }
    }

    private fun destroyConnectJob() {
        connectServiceJob?.cancel()
        connectServiceJob = null
    }

    private fun disconnectServiceInternal(
        onStartResult: (connected: Boolean) -> Unit
    ) {
        stop()
        state = ConnectState.Stopped
        onDisconnected()
        onStartResult(false)
        getHeartbeatReportedDisConnect()
    }

     fun onConnected() {
        preConnectData = connectData
        startTimer()
    }

    private fun onDisconnected() {
        stopTimer(false)
    }

    /**
     * 心跳上报(链接)
     */
    fun getHeartbeatReportedConnect() {
        jobHeart?.cancel()
        jobHeart = null
        jobHeart = context.lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                var data: String
                var ip: String
                if (isConnected()) {
                    data = "go"
                    ip = Constant.IP_AFTER_VPN_LINK_SPIN.asSpKeyAndExtract()
                } else {
                    data = "ba"
                    ip = Constant.CURRENT_IP_SPIN.asSpKeyAndExtract()
                }
                if (isConnected()) {
                    SpinOkHttpUtils.getHeartbeatReporting(data, ip)
                }
                delay(60000)
            }
        }
    }

    /**
     * 心跳上报(断开)
     */
    fun getHeartbeatReportedDisConnect() {
        jobHeart?.cancel()
        jobHeart = null
        GlobalScope.launch(Dispatchers.IO) {
            SpinOkHttpUtils.getHeartbeatReporting(
                "ba",
                Constant.CURRENT_IP_SPIN.asSpKeyAndExtract()
            )
        }
    }
}
