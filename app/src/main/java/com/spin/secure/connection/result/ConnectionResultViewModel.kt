package com.spin.secure.connection.result

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.spin.secure.base.BaseViewModel
import com.spin.secure.fixTimeUnitLength
import com.spin.secure.getHour
import com.spin.secure.getMin
import com.spin.secure.getSec
import com.spin.secure.main.core.connector.BaseConnector
import com.spin.secure.main.core.countryIconResId

class ConnectionResultViewModel(application: Application) : BaseViewModel(application) {
    val isConnected = MutableLiveData<Boolean>()
    val countryResId = MutableLiveData<Int>()
    val hour = MutableLiveData("00")
    val minutes = MutableLiveData("00")
    val second = MutableLiveData("00")
    val showResultNativeAd = MutableLiveData<Any>()

    private var mIsConnected = false
        set(value) {
            field = value
            isConnected.value = value
        }
    private val mConnectCallback by lazy {
        object : BaseConnector.Callback {
            override fun onConnectTimeChanged(time: Long) {
                parseConnectTime(time)
            }
        }
    }

    private fun parseConnectTime(time: Long) {
        hour.value = time.getHour().fixTimeUnitLength()
        minutes.value = time.getMin().fixTimeUnitLength()
        second.value = time.getSec().fixTimeUnitLength()
    }

    fun parseResult(connected: Boolean) {
        mIsConnected = connected
        countryResId.value = (if (connected)
            BaseConnector.connectData
        else BaseConnector.preConnectData).countryIconResId
        if (connected) {
            BaseConnector.registerCallback(mConnectCallback)
        } else {
            parseConnectTime(BaseConnector.preConnectTime)
        }
    }

    override fun onCleared() {
        if (mIsConnected) {
            BaseConnector.unregisterCallback(mConnectCallback)
        }
    }
}