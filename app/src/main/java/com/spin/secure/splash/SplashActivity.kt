package com.spin.secure.splash

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.BarUtils
import com.spin.secure.R
import com.spin.secure.ads.AdLoadUtils
import com.spin.secure.ads.AdsCons
import com.spin.secure.base.BaseActivity
import com.spin.secure.databinding.ActivitySplashBinding
import com.spin.secure.dismiss
import com.spin.secure.main.SpinActivity
import com.spin.secure.utils.SpinOkHttpUtils
import com.spin.secure.utils.SpinTbaUtils
import com.spin.secure.utils.SpinUtils
import com.xuexiang.xutil.net.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding, SplashViewModel>() {
    override val implLayoutResId = R.layout.activity_splash
    override val model by viewModels<SplashViewModel>()

    private lateinit var progressValueAnimator: ValueAnimator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdLoadUtils.loadOf(
            where = AdsCons.POS_OPEN
        )
        BarUtils.addMarginTopEqualStatusBarHeight(binding.logo)
        val that = this
        with(model) {
            gotoNext.observe(that) {
                that.gotoNext()
            }
            showOpen.observe(that) {
                AdLoadUtils.showFullScreenOf(
                    where = AdsCons.POS_OPEN,
                    context = that,
                    res = it,
                    onShowCompleted = {
                        lifecycleScope.launch(Dispatchers.Main) {
                            model.gotoNext.value = true
                        }
                    }
                )
            }
        }
        start()
        lifecycleScope.launch(Dispatchers.IO) {
            if(!NetworkUtils.isNetworkAvailable()){
                return@launch
            }
            SpinUtils.referrer(this@SplashActivity)
            runBlocking {
                SpinOkHttpUtils.getDeliverData()
                SpinTbaUtils.obtainGoogleAdvertisingId(this@SplashActivity)
                SpinTbaUtils.obtainIpAddress()
            }
            SpinOkHttpUtils.postSessionEvent()
            SpinOkHttpUtils.getBlacklistData()
        }
    }

    private fun start() {
        with(
            ValueAnimator.ofInt(0, 100)
                .apply {
                    duration = 10000L
                }) {
            progressValueAnimator = this
            addUpdateListener {
                with(it.animatedValue as Int) {
                    model.progress.value = this
                    if (this in 20..99) {
                        AdLoadUtils.resultOf(AdsCons.POS_OPEN)?.let { res ->
                            progressValueAnimator.dismiss()
                            model.showOpen.value = res
                        }
                    } else if (this >= 100) {
                        model.gotoNext.value = true
                    }
                }
            }
            start()
        }
    }

    private fun gotoNext() {
        if (!ActivityUtils.isActivityExistsInStack(SpinActivity::class.java)) {
            ActivityUtils.startActivity(SpinActivity::class.java)
        }
        finish()
    }

    override fun onBackPressed() {
    }

    override fun onPause() {
        super.onPause()
        progressValueAnimator.pause()
    }

    override fun onPostResume() {
        super.onPostResume()
        progressValueAnimator.resume()
    }

    override fun onResume() {
        super.onResume()
        SpinUtils.toBuriedPointSpin("spi_zag")
    }

    override fun onDestroy() {
        super.onDestroy()
        progressValueAnimator.dismiss()
    }
}