package com.spin.secure.main

import android.animation.ValueAnimator
import android.app.Application
import android.content.Intent
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.NetworkUtils
import com.spin.secure.*
import com.spin.secure.ads.AdLoadUtils
import com.spin.secure.ads.AdsCons
import com.spin.secure.base.BaseViewModel
import com.spin.secure.key.Constant
import com.spin.secure.main.core.ConnectState
import com.spin.secure.main.core.ConnectState.*
import com.spin.secure.main.core.IntentCons
import com.spin.secure.main.core.MConnection
import com.spin.secure.main.core.connector.BaseConnector
import com.spin.secure.main.core.countryIconResId
import com.spin.secure.utils.KLog
import com.spin.secure.utils.SpinUtils

class SpinViewModel(application: Application) : BaseViewModel(application), BaseConnector.Callback {
    class ConnectAdParam(
        val forConnect: Boolean,
        val res: Any,
        val next: (forConnect: Boolean) -> Unit
    )

    val connectionText = MutableLiveData("00:00:00")
    val connectProgress = MutableLiveData(0)
    val countryResId = MutableLiveData(R.mipmap.fast)
    val stateText = MutableLiveData(Stopped.text)
    val clickable = MutableLiveData(true)
    val showNetNotAccessDialog = MutableLiveData<Boolean>()
    val startResultActivity = MutableLiveData<Boolean>()
    val doToggleConnection = MutableLiveData<Boolean>()
    val startConnectingAnimation = MutableLiveData(false)
    val showConnectAd = MutableLiveData<ConnectAdParam>()
    val showHomeNativeAd = MutableLiveData<Any>()

    private var mConnector: BaseConnector? = null
    private var mNewConnection: MConnection? = null
    private var mConnectJob: ValueAnimator? = null
    private var mStartProgressJob: ValueAnimator? = null
    private var mConnection = BaseConnector.connectData
        set(value) {
            field = value
            countryResId.value = value.countryIconResId
        }
    private val mAdTask = Runnable {
        interceptToggleConnection(false)
    }

    fun initConnection(newConnector: () -> BaseConnector) {
        mConnector = newConnector()
        AdLoadUtils.registerTask(mAdTask)
        initUi()
        with(mConnector!!) {
            setServiceStateCallback {
                if (it && isConnected()) {
                    resumeConnectedState()
                }
            }
        }
        BaseConnector.registerCallback(this)
    }

    private fun initUi() {
        doOnConnector {
            if (isConnected()) {
                connectProgress.value = 100
            }
        }
        countryResId.value = BaseConnector.connectData.countryIconResId
        stateText.value = BaseConnector.state.text
    }

    override fun onConnectTimeChanged(time: Long) {
        connectionText.value = time.toTimeUnitStr()
    }

    override fun onConnectionChanged(connection: MConnection) {
        mConnection = connection
    }

    override fun onStateChanged(state: ConnectState) {
        when (state) {
            Connected -> SpinApp.isVpnGlobalLink = true
            Stopped -> SpinApp.isVpnGlobalLink = false
            else -> {
            }
        }
        stateText.value = state.text
    }

    private fun resumeConnectedState() {
        mConnection = BaseConnector.connectData
        startProgressAnimation(2, animated = false)
        stateText.value = Connected.text
    }

    private fun openConnection() {
        if (!NetworkUtils.isConnected()) {
            changeConnectDataReally()
            showNetNotAccessDialog.value = true
            return
        }
        doOnConnector {
            if (isConnected()) return@doOnConnector
            checkPermission {
                if (it) prepareToggleConnection(true)
                else changeConnectDataReally()
            }
        }
    }

    private fun changeConnectDataReally() {
        mNewConnection?.let {
            BaseConnector.connectData = it
            mNewConnection = null
        }
    }

    private fun closeConnection() {
        prepareToggleConnection(false)
    }

    private fun prepareToggleConnection(forConnect: Boolean) {
        clickable.value = false
        startProgressAnimation(1)
        startConnectingAnimation.value = true
        if (forConnect) {
            SpinUtils.toBuriedPointSpin("spin_ent")
            BaseConnector.state = Connecting
        } else {
            BaseConnector.state = Stopping
        }
        destroyConnectJob()
        prepareConnectAds()
        mConnectJob = ValueAnimator.ofInt(0, 100).apply {
            duration = 10000L
            addUpdateListener {
                val progress = it.animatedValue as Int
                if (progress in 20..99) {
                    AdLoadUtils.resultOf(AdsCons.POS_CONNECT)?.let { res ->
                        destroyConnectJob()
                        showConnectAd.value = ConnectAdParam(
                            forConnect = forConnect,
                            res = res,
                            next = { forConnect2 ->
                                doToggleConnection.value = forConnect2
                            }
                        )
                    }
                } else if (progress >= 100) {
                    doToggleConnection.value = forConnect
                }
            }
            start()
        }
    }

    private fun prepareConnectAds() {
        AdLoadUtils.loadOf(AdsCons.POS_RESULT)
        AdLoadUtils.loadOf(AdsCons.POS_CONNECT)
    }

    /**
     * @param position: 0-left 1-middle 2-end
     */
    private fun startProgressAnimation(position: Int, animated: Boolean = true) {
        val next = when (position) {
            0 -> 0
            1 -> 50
            2 -> 100
            else -> throw IllegalStateException("position: 0-left 1-middle 2-end")
        }
        if (!animated) {
            connectProgress.value = next
            return
        }
        destroyStartProgressJob()
        mStartProgressJob = ValueAnimator.ofInt(connectProgress.value!!, next)
            .apply {
                duration = 300L
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    connectProgress.value = it.animatedValue as Int
                }
                start()
            }
    }

    fun onPause() {
        mConnectJob?.pause()
        mStartProgressJob?.pause()
    }

    fun onResume() {
        mConnectJob?.resume()
        mStartProgressJob?.resume()
    }

    private fun destroyConnectJob() {
        mConnectJob?.dismiss()
        mConnectJob = null
    }

    fun doToggleConnection(forConnect: Boolean) {
        changeConnectDataReally()
        doOnConnector {
            doToggleConnection(forConnect) {
                startProgressAnimation(if (it) 2 else 0)
                if (forConnect) {
                    if (it) {
                        startResultActivity.postValue(it)
                    } else {
                        startConnectingAnimation.value = false
                        clickable.value = true
                    }
                } else if (!forConnect && !it) {
                    startResultActivity.postValue(it)
                } else {
                    startConnectingAnimation.value = false
                    clickable.value = true
                }
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            IntentCons.REQ_CODE_CONNECT_SERVERS -> {
                val connectData =
                    data?.getSerializableExtra(
                        IntentCons.KEY_SERVERS_CONNECT_DATA
                    ) as? MConnection ?: return
                when (val action =
                    data.getStringExtra(IntentCons.KEY_SERVERS_CONNECT_ACTION)) {
                    IntentCons.VALUE_SERVERS_CONNECT_ACTION_CONNECT ->
                        setNewConnectData(connectData, action)
                    IntentCons.VALUE_SERVERS_CONNECT_ACTION_DISCONNECT ->
                        setNewConnectData(connectData, action)
                }
            }
        }
    }

    private fun setNewConnectData(newConnectData: MConnection, action: String) {

        mNewConnection = newConnectData
        mNewConnection?.let { mConnection = it }
        when (action) {
            IntentCons.VALUE_SERVERS_CONNECT_ACTION_CONNECT -> openConnection()
            IntentCons.VALUE_SERVERS_CONNECT_ACTION_DISCONNECT -> closeConnection()
        }
    }

    fun onDestroy() {
        interceptToggleConnection(true)
        mConnector = null
        BaseConnector.unregisterCallback(this)
        destroyStartProgressJob()
        AdLoadUtils.unregisterTask(mAdTask)
    }

    private fun destroyStartProgressJob() {
        mStartProgressJob?.dismiss()
        mStartProgressJob = null
    }

    fun toggleConnection() {
        doOnConnector {
            if (canStop()) closeConnection() else openConnection()
        }
    }

    /**
     * b方案链接VPN
     */
    fun linkVpn() {
        doOnConnector {
            if (!canStop()) {
                SpinUtils.toBuriedPointSpin("spin_go")
                openConnection()
            }
        }
    }

    private fun doOnConnector(action: BaseConnector.() -> Unit) {
        mConnector?.let { action(it) }
    }

    private fun interceptToggleConnection(onDestroy: Boolean) {
        mConnectJob?.let {
            destroyConnectJob()
            resumeConnection(onDestroy)
            mNewConnection?.let {
                mNewConnection = null
                mConnection = BaseConnector.preConnectData
            }
            clickable.value = true
        }
    }

    private fun resumeConnection(onDestroy: Boolean) {
        val state = BaseConnector.state
        if (state == Connecting) {
            BaseConnector.state = Stopped
            if (!onDestroy) {
                startConnectingAnimation.value = false
                startProgressAnimation(0, animated = false)
            }
        } else if (state == Stopping) {
            BaseConnector.state = Connected
            if (!onDestroy) {
                startConnectingAnimation.value = false
                startProgressAnimation(2, animated = false)
            }
        }
    }

    /**
     * 是否是买量用户
     */
    fun isItABuyingUser(): Boolean {
        return SpinUtils.isValuableUser()
    }
}