package com.spin.secure.connection.result

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.BarUtils
import com.spin.secure.R
import com.spin.secure.ads.AdLoadUtils
import com.spin.secure.ads.AdsCons
import com.spin.secure.base.BaseActivity
import com.spin.secure.click
import com.spin.secure.databinding.ActivityConnResultBinding
import com.spin.secure.isVisible
import com.spin.secure.main.core.IntentCons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConnectionResultActivity :
    BaseActivity<ActivityConnResultBinding, ConnectionResultViewModel>() {
    override val model by viewModels<ConnectionResultViewModel>()
    override val implLayoutResId = R.layout.activity_conn_result

    private var resultAdJob: Job? = null
    private var hotLaunched = true
    private val adTask = Runnable { hotLaunched = true }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(binding) {
            BarUtils.addMarginTopEqualStatusBarHeight(topLayout)
            btnBack.click { onBackPressed() }
        }
        model.parseResult(intent.getBooleanExtra(IntentCons.KEY_CONNECT_RESULT_IS_CONNECTED, false))
        model.showResultNativeAd.observe(this) {
            showResultNativeAd(it)
        }
        AdLoadUtils.registerTask(adTask)
    }

    override fun onPostResume() {
        super.onPostResume()
        startResultNativeAdJob()
    }

    private fun startResultNativeAdJob() {
        if (!hotLaunched) return
        cancelResultNativeAdJob()
        AdLoadUtils.loadOf(
            where = AdsCons.POS_RESULT
        )
        resultAdJob = lifecycleScope.launch(Dispatchers.Main) {
            delay(300L)
            if (isVisible()) {
                var res = resultOfResultAds()
                while (res == null) {
                    delay(1000L)
                    res = resultOfResultAds()
                }
                model.showResultNativeAd.value = res
            }
        }
    }

    private fun showResultNativeAd(res: Any) {
        AdLoadUtils.showNativeOf(
            where = AdsCons.POS_RESULT,
            nativeRoot = binding.adRoot,
            res = res,
            preload = true,
            onShowCompleted = {
                hotLaunched = false
            }
        )
    }

    private fun resultOfResultAds(): Any? {
        return AdLoadUtils.resultOf(
            where = AdsCons.POS_RESULT
        )
    }

    private fun cancelResultNativeAdJob() {
        resultAdJob?.cancel()
        resultAdJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        AdLoadUtils.unregisterTask(adTask)
    }
}