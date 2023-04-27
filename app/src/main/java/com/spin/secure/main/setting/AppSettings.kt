package com.spin.secure.main.setting

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.blankj.utilcode.util.ToastUtils

object AppSettings {
    private const val CONTACT_EMAIL = "xxxx@gmail.com"
    private const val PRIVACY_AGREEMENT = "https://www.baidu.com"
    private const val GOOGLE_PLAY = "https://play.google.com/store/apps/details?id="

    @JvmStatic
    fun openEmail(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_SENDTO)
                    .apply {
                        data = "mailto:".toUri()
                        putExtra(Intent.EXTRA_EMAIL, CONTACT_EMAIL)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
        }.onFailure {
            ToastUtils.showShort("Contact us with email: $CONTACT_EMAIL")
        }
    }

    @JvmStatic
    fun openPrivacy(context: Context) {
        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW, PRIVACY_AGREEMENT.toUri()
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
        }
    }

    @JvmStatic
    fun shareApp(context: Context) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_SEND)
                .apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, GOOGLE_PLAY)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
        }
    }

    @JvmStatic
    fun updateApp(context: Context) {
        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW, GOOGLE_PLAY.toUri()
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
        }
    }
}