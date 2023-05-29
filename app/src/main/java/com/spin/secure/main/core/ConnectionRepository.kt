package com.spin.secure.main.core

import com.github.shadowsocks.database.ProfileManager
import com.spin.secure.getAppMmkv
import com.spin.secure.main.core.connector.ss.syncFromData
import com.spin.secure.toModelOrDefault
import com.spin.secure.utils.SpinUtils.getDataFastServerData
import com.spin.secure.utils.SpinUtils.getDataFromTheServer
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.tip.ToastUtils

class ConnectionRepository {
    companion object {
        private val local = listOf(
            MConnection(
                host = "213.183.48.10",
                port = 8116,
                method = "chacha20-ietf-poly1305",
                password = "gdhgosGH2=2Nnn",
                city = "Tokyo",
                country = "Japan"
            ),
            MConnection(
                host = "3.112.249.230",
                port = 800,
                method = "chacha20-ietf-poly1305",
                password = "G!151548",
                city = "New York",
                country = "United States"
            )
        )
    }

    fun listAll(): List<MConnection> {
        return getDataFromTheServer()?:MConnectionLib().data
    }

    private fun tryListRemote(key: String): List<MConnection> {
        val remote = (getAppMmkv().decodeString(key) ?: "")
            .toModelOrDefault(MConnectionLib::class.java) { MConnectionLib() }
        if (remote.data.isNotEmpty()) {
            ProfileManager.syncFromData(remote.data)
        }
        return remote.data
    }

    fun listSmart(): List<MConnection> {
        val data = getDataFastServerData()?:MConnectionLib().data
        return data
    }
}