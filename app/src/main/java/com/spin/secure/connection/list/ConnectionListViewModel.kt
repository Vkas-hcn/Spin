package com.spin.secure.connection.list

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.spin.secure.base.BaseViewModel

class ConnectionListViewModel(application: Application) : BaseViewModel(application) {
    val showBackAd = MutableLiveData<Any>()
}