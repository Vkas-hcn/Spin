package com.spin.secure.base

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.blankj.utilcode.util.BarUtils
import com.spin.secure.BR

abstract class BaseActivity<B : ViewDataBinding, M : BaseViewModel> : AppCompatActivity() {
    protected lateinit var binding: B
    protected abstract val model: M
    protected abstract val implLayoutResId: Int

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adaptScreen()
        initSystemUi()
        initDataBinding()
    }

    private fun initSystemUi() {
        BarUtils.transparentStatusBar(this)
        BarUtils.setStatusBarLightMode(this, true)
    }

    private fun initDataBinding() {
        val that = this
        binding = DataBindingUtil.setContentView<B>(this, implLayoutResId)
            .apply {
                lifecycleOwner = that
                setVariable(BR.m, model)
            }
    }

    private fun adaptScreen() {
        with(resources.displayMetrics) {
            density = heightPixels / 780.0F
            densityDpi = (160 * density).toInt()
        }
    }
}