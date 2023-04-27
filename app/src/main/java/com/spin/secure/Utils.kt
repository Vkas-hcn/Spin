package com.spin.secure

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.lifecycle.Lifecycle
import com.blankj.utilcode.util.ClickUtils
import com.blankj.utilcode.util.ProcessUtils
import com.google.gson.reflect.TypeToken
import com.spin.secure.bean.SpinRemoteBean
import com.spin.secure.key.Constant
import com.squareup.moshi.Moshi
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope

fun ValueAnimator.dismiss() {
    removeAllUpdateListeners()
    cancel()
}

fun String.replaceBlanks(): String = replace(" ", "")

object DataBindingAdapter {

    @BindingAdapter("img_res_id")
    @JvmStatic
    fun bindImageResId(imageView: ImageView, resId: Int) {
        imageView.setImageResource(resId)
    }

    @BindingAdapter("background_res_id")
    @JvmStatic
    fun bindBackgroundResId(view: View, resId: Int) {
        view.setBackgroundResource(resId)
    }

    @BindingAdapter("view_enable")
    @JvmStatic
    fun bindEnable(view: View, enable: Boolean) {
        view.isEnabled = enable
    }

    @BindingAdapter("view_select")
    @JvmStatic
    fun bindSelected(view: View, selected: Boolean) {
        view.isSelected = selected
    }

    @BindingAdapter("str_text_color")
    @JvmStatic
    fun bindStrTextColor(tv: TextView, colorStr: String) {
        runCatching {
            tv.setTextColor(Color.parseColor(colorStr))
        }
    }
}

fun runOnMainProgress(action: () -> Unit) {
    if (ProcessUtils.isMainProcess()) {
        action()
    }
}

val AppScope: CoroutineScope
    get() {
        return GlobalScope
    }

fun AppCompatActivity.isOnResumeByLife(): Boolean {
    return lifecycle.currentState == Lifecycle.State.RESUMED
}

fun <T> Iterator<T>.forEachRemain(accept: (e: T) -> Unit) {
    while (hasNext()) {
        accept(next())
    }
}

fun Long.toTimeUnitStr(): String {
    return "${getHour().fixTimeUnitLength()}:${
        getMin().fixTimeUnitLength()
    }:${getSec().fixTimeUnitLength()}"
}

fun Long.fixTimeUnitLength(): String {
    return toString().fixTimeUnitLength()
}

fun String.fixTimeUnitLength(): String {
    return if (this.length == 1) "0${this}" else this
}

fun Long.getHour(): Long {
    return (this % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
}

fun Long.getMin(): Long {
    return (this % (1000 * 60 * 60)) / (1000 * 60)
}

fun Long.getSec(): Long {
    return (this % (1000 * 60)) / 1000
}

fun <T : Activity> Activity.startActivity(des: Class<T>) {
    startActivity(Intent(this, des))
}

fun <T : Activity> Activity.startActivityForResult(des: Class<T>, reqCode: Int) {
    if (reqCode == -1) throw IllegalStateException("reqCode can not be -1.")
    startActivityForResult(Intent(this, des), reqCode)
}

fun View.click(onClick: () -> Unit) {
    ClickUtils.applySingleDebouncing(this) { onClick() }
}

private val mmkv by lazy {
    MMKV.mmkvWithID("Spin", MMKV.MULTI_PROCESS_MODE)
}

fun getAppMmkv(): MMKV {
    return mmkv
}

fun <T> String.toModelOrNull(clazz: Class<T>): T? {
    return runCatching {
        Moshi.Builder()
            .build()
            .adapter(clazz)
            .fromJson(this)
    }.getOrNull()
}

fun <T> String.toModelOrDefault(clazz: Class<T>, creator: () -> T): T {
    return toModelOrNull(clazz) ?: creator()
}

fun AppCompatActivity.isVisible(): Boolean {
    return lifecycle.currentState == Lifecycle.State.RESUMED
}

fun String.asSpKeyAndExtract(): String {
    return getAppMmkv().decodeString(this) ?: ""
}

