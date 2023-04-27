package com.spin.secure.main.core

import com.spin.secure.R
import com.spin.secure.replaceBlanks

val MConnection.countryIconResId: Int
    get() {
        return when (country.lowercase().replaceBlanks()) {
            "japan" -> R.mipmap.japan
            "canada" -> R.mipmap.canada
            "unitedstates" -> R.mipmap.unitedstates
            "unitedkingdom" -> R.mipmap.unitedkingdom
            "germany" -> R.mipmap.germany
            "australia" -> R.mipmap.australia
            "france" -> R.mipmap.france
            "singapore" -> R.mipmap.singapore
            else -> R.mipmap.fast
        }
    }

val MConnection.name: String
    get()
    = if (smart) "Super Fast Servers" else "$country - $city"

val SmartConnection by lazy { MConnection(smart = true) }

fun MConnection.contentEqual(other: MConnection): Boolean {
    return (other.host == host && other.port == port)
            || (smart && other.smart)
}
