package com.spin.secure.main

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.blankj.utilcode.util.BarUtils
import com.spin.secure.*
import com.spin.secure.ads.AdLoadUtils
import com.spin.secure.ads.AdsCons
import com.spin.secure.base.BaseActivity
import com.spin.secure.bean.SpinRemoteBean
import com.spin.secure.connection.list.ConnectionListActivity
import com.spin.secure.connection.result.ConnectionResultActivity
import com.spin.secure.databinding.ActivitySpinBinding
import com.spin.secure.key.Constant.logTagSpin
import com.spin.secure.main.core.IntentCons
import com.spin.secure.main.core.connector.ss.ShadowsockConnector
import com.spin.secure.main.setting.AppSettings
import com.spin.secure.utils.KLog
import com.spin.secure.utils.SpinUtils
import com.xuexiang.xutil.net.JsonUtil.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SpinActivity : BaseActivity<ActivitySpinBinding, SpinViewModel>() {
    override val model by viewModels<SpinViewModel>()
    override val implLayoutResId = R.layout.activity_spin

    private var homeAdJob: Job? = null

    //是否执行A方案
    private var whetherToImplementPlanA = false
    val bubbleConfig: SpinRemoteBean = SpinUtils.getScenarioConfiguration()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = model.dialogDunUser(this)
        if(data){return}
        with(binding) {
            BarUtils.addMarginTopEqualStatusBarHeight(topLayout)
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            btnSetting.click { openSettingPanel() }
            btnList.click {
                lifecycleScope.launch {
                    if (SpinUtils.deliverServerTransitions()) {
                        startActivityForResult(
                            ConnectionListActivity::class.java,
                            IntentCons.REQ_CODE_CONNECT_SERVERS
                        )
                        proList.visibility = View.GONE
                    } else {
                        proList.visibility = View.VISIBLE
                        delay(2000)
                        proList.visibility = View.GONE
                    }
                }
            }
            btnToggle.click {
                lifecycleScope.launch {
                    SpinUtils.toBuriedPointSpin("spin_ccl")
                    SpinUtils.getIpInformation()
                    val data = model.dialogDunUser(this@SpinActivity)
                    if(data){return@launch}
                    if (SpinUtils.deliverServerTransitions()) {
                        model.toggleConnection()
                        proList.visibility = View.GONE
                    } else {
                        proList.visibility = View.VISIBLE
                        delay(2000)
                        proList.visibility = View.GONE
                    }
                }
            }
            initSetting()
        }

        val that = this
        with(model) {
            startResultActivity.observe(that) {
                lifecycleScope.launch {
                    delay(300L)
                    if (that.isOnResumeByLife()) {
                        startActivity(Intent(that, ConnectionResultActivity::class.java).apply {
                            putExtra(IntentCons.KEY_CONNECT_RESULT_IS_CONNECTED, it)
                        })
                    }
                    startConnectingAnimation.value = false
                    clickable.value = true
                }
            }
            doToggleConnection.observe(that) {
                doToggleConnection(it)
            }
            startConnectingAnimation.observe(that) {
                if (it)
                    binding.lottie.playAnimation()
                else
                    binding.lottie.cancelAnimation()
            }
            showNetNotAccessDialog.observe(that) {
                showNetNotAccessDialog()
            }
            showConnectAd.observe(that) {
                showConnectAd(it)
            }
            showHomeNativeAd.observe(that) {
                showHomeNativeAd(it)
            }
            initConnection {
                ShadowsockConnector(that)
            }
        }
        KLog.d(logTagSpin, "bubbleConfig---->${toJson(bubbleConfig)}")
        if (bubbleConfig.spin_start == "1") {
            getVpnPlan()
        }
    }

    private fun showConnectAd(param: SpinViewModel.ConnectAdParam) {
        AdLoadUtils.showFullScreenOf(
            where = AdsCons.POS_CONNECT,
            context = this,
            res = param.res,
            onShowCompleted = {
                param.next(param.forConnect)
            },
            preload = true
        )
    }

    @SuppressLint("CheckResult")
    private fun initSetting() {
        val that = this
        with(binding.layoutSetting) {
            BarUtils.addMarginTopEqualStatusBarHeight(logo)
            tvContactUs.click { AppSettings.openEmail(that) }
            tvPrivacyPolicy.click { AppSettings.openPrivacy(that) }
            tvUpdateApp.click { AppSettings.updateApp(that) }
            tvShareApp.click { AppSettings.shareApp(that) }
//            tvDebug.click {
//                MaterialDialog(that)
//                    .show {
//                        input(
//                            prefill = getAppMmkv().decodeString(
//                                "brand_strategy",
//                                """{
//   "strategy":0,
//   "packages":[
//   ]
//}"""
//                            )
//                        ) { _, text ->
//                            getAppMmkv().encode("brand_strategy", text.toString())
//                        }
//                        positiveButton(text = "Save")
//                    }
//            }
        }
    }

    private fun isSettingPanelOpen(): Boolean {
        return binding.drawerLayout.isDrawerOpen(binding.navigationView)
    }

    override fun onBackPressed() {
        if (isSettingPanelOpen()) {
            closeSettingPanel()
            return
        }
        super.onBackPressed()
    }

    private fun openSettingPanel() {
        if (isSettingPanelOpen()) return
        binding.drawerLayout.openDrawer(binding.navigationView)
    }

    private fun closeSettingPanel() {
        binding.drawerLayout.closeDrawer(binding.navigationView)
    }

    private fun showNetNotAccessDialog() {
        val that = this
        MaterialDialog(this)
            .show {
                message(text = "Network is not accessible!")
                negativeButton(text = "Cancel")
                lifecycleOwner(that)
            }
    }

    override fun onPause() {
        super.onPause()
        model.onPause()
    }

    override fun onPostResume() {
        super.onPostResume()
        model.onResume()
        startHomeNativeAdJob()
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        model.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        model.onDestroy()
        SpinApp.nativeAdRefreshBa =true
    }

    private fun startHomeNativeAdJob() {
        cancelHomeNativeAdJob()
        AdLoadUtils.loadOf(
            where = AdsCons.POS_HOME
        )
        homeAdJob = lifecycleScope.launch(Dispatchers.Main) {
            delay(300L)
            if (SpinApp.nativeAdRefreshBa && isVisible()) {
                if (bubbleConfig.spin_start == "2") {
                    getVpnPlan()
                }
                var res = resultOfHomeAds()
                while (res == null) {
                    delay(1000L)
                    res = resultOfHomeAds()
                }
                model.showHomeNativeAd.value = res
                SpinApp.nativeAdRefreshBa = false
            }
        }
    }

    private fun showHomeNativeAd(res: Any) {
        AdLoadUtils.showNativeOf(
            where = AdsCons.POS_HOME,
            nativeRoot = binding.adRoot,
            res = res,
            preload = true
        )
    }

    private fun resultOfHomeAds(): Any? {
        return AdLoadUtils.resultOf(
            where = AdsCons.POS_HOME
        )
    }

    private fun cancelHomeNativeAdJob() {
        homeAdJob?.cancel()
        homeAdJob = null
    }

    /**
     * 获取Vpn方案
     */
    private fun getVpnPlan() {
        if (!model.isItABuyingUser()) {
            //非买量用户直接走A方案
            whetherToImplementPlanA = true
            return
        }
        val data = bubbleConfig.spin_may
        if ((data).isEmpty()) {
            KLog.d(logTagSpin, "判断Vpn方案---默认")
            vpnCPlan("50")
        } else {
            //C
            whetherToImplementPlanA = false
            vpnCPlan(data)
        }
    }

    /**
     * vpn B 方案
     */
    private fun vpnBPlan() {
        lifecycleScope.launch {
            if (SpinUtils.deliverServerTransitions()) {
                model.linkVpn()
                binding.proList.visibility = View.GONE
            } else {
                binding.proList.visibility = View.VISIBLE
                delay(2000)
                binding.proList.visibility = View.GONE
            }
        }
    }

    /**
     * vpn C 方案
     * 概率
     */
    private fun vpnCPlan(mProbability: String) {
        val mProbabilityInt = mProbability.toIntOrNull()
        if (mProbabilityInt == null) {
            whetherToImplementPlanA = true
        } else {
            val random = (0..100).shuffled().last()
            when {
                random <= mProbabilityInt -> {
                    //B
                    KLog.d(logTagSpin, "随机落在B方案")
                    vpnBPlan() //20，代表20%为B用户；80%为A用户
                }
                else -> {
                    //A
                    KLog.d(logTagSpin, "随机落在A方案")
                    whetherToImplementPlanA = true
                }
            }
        }
    }
}