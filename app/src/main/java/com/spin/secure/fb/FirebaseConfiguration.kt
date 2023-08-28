package com.spin.secure.fb

import android.app.Application
import androidx.annotation.MainThread
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.spin.secure.BuildConfig
import com.spin.secure.SpinApp
import com.spin.secure.key.Constant

@MainThread
object FirebaseConfiguration {
    fun initAndActive(app: Application) {
        if (BuildConfig.DEBUG) return
        Firebase.initialize(app)
        Firebase.remoteConfig
            .apply {
                setConfigSettingsAsync(
                    FirebaseRemoteConfigSettings
                        .Builder()
                        .setMinimumFetchIntervalInSeconds(3600L)
                        .build()
                )
            }
            .fetchAndActivate()
            .addOnSuccessListener {
                Firebase.remoteConfig.getStringAndSave("brand_strategy")
                Firebase.remoteConfig.getStringAndSave("si_ads")
                Firebase.remoteConfig.getStringAndSave("si_ssss")
                Firebase.remoteConfig.getStringAndSave("si_saaa")

                Firebase.remoteConfig.getStringAndSave(Constant.spin_config)
                Firebase.remoteConfig.getStringAndSave(Constant.spin_roof)

            }
    }
}