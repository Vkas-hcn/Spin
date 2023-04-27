package com.spin.secure.main.core.connector.ss

import androidx.annotation.WorkerThread
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.spin.secure.main.core.MConnection
import com.spin.secure.main.core.name

fun MConnection.toProfile(): Profile {
    return Profile(
        id = 0L,
        name = name,
        host = host,
        remotePort = port,
        password = password,
        method = method
    )
}

@WorkerThread
fun Profile.createOrUpdateRecord() {
    var existed: Long? = null
    ProfileManager.getAllProfiles()?.forEach {
        if (it.host == host && it.remotePort == remotePort) {
            existed = it.id
            return@forEach
        }
    }
    if (existed == null) {
        ProfileManager.createProfile(this)
    } else {
        ProfileManager.updateProfile(this.apply {
            id = existed!!
        })
    }
}

@WorkerThread
fun Profile.getDataId(): Long {
    ProfileManager.getAllProfiles()?.forEach {
        if (it.host == host && it.remotePort == remotePort) {
            return it.id
        }
    }
    return 0L
}

@WorkerThread
fun ProfileManager.syncFromData(data: List<MConnection>) {
    data.iterator().forEach { it.toProfile().createOrUpdateRecord() }
}