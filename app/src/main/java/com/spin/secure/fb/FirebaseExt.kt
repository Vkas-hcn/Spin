package com.spin.secure.fb

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.spin.secure.getAppMmkv

fun FirebaseRemoteConfig.getStringAndSave(key: String) {
    getAppMmkv().encode(key, getString(key))
}