package com.spin.secure.splash

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.spin.secure.base.BaseViewModel

class SplashViewModel(application: Application) : BaseViewModel(application) {
    val progress = MutableLiveData(0)
    val gotoNext = MutableLiveData<Boolean>()
    val showOpen = MutableLiveData<Any>()
}