package com.spin.secure.main.core

enum class ConnectState(val text: String) {
    Connecting("Connecting..."),
    Connected("Connected"),
    Stopping("Disconnecting..."),
    Stopped("Connect")
}