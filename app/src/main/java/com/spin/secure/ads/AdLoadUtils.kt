package com.spin.secure.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ResourceUtils
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.spin.secure.*
import com.spin.secure.R
import com.spin.secure.key.Constant
import com.spin.secure.key.Constant.logTagSpin
import com.spin.secure.splash.SplashActivity
import com.spin.secure.utils.KLog
import com.spin.secure.utils.SpinOkHttpUtils
import com.spin.secure.utils.SpinUtils
import com.spin.secure.utils.SpinUtils.afterLoadLinkSettingsBa
import com.spin.secure.utils.SpinUtils.beforeLoadLinkSettingsBa
import com.spin.secure.utils.SpinUtils.isBlockScreenAds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@MainThread
object AdLoadUtils {
    var myUnit_O: MAd = MAd()
    var myUnit_H: MAd = MAd()
    var myUnit_C: MAd = MAd()
    var myUnit_R: MAd = MAd()
    var myUnit_B: MAd = MAd()

    fun init(context: Application) {
        GoogleAds.init(context) {
            preloadAds()
        }
        Hot.registerAppLifeCallback(context)
    }

    fun loadOf(where: String) {
        Load.of(where)?.load()
    }

    fun resultOf(where: String): Any? {
        return Load.of(where)?.res
    }

    fun showFullScreenOf(
        where: String,
        context: AppCompatActivity,
        res: Any,
        preload: Boolean = false,
        onShowCompleted: () -> Unit
    ) {
        Show.of(where)
            .showFullScreen(
                context = context,
                res = res,
                callback = {
                    Load.of(where)?.let { load ->
                        load.clearCache()
                        if (preload) {
                            load.load()
                        }
                    }
                    onShowCompleted()
                }
            )
    }

    fun showNativeOf(
        where: String,
        nativeRoot: View,
        res: Any,
        preload: Boolean = false,
        onShowCompleted: (() -> Unit)? = null
    ) {
        Show.of(where)
            .showNativeOf(
                nativeRoot = nativeRoot,
                res = res,
                callback = {
                    Load.of(where)?.let { load ->
                        load.clearCache()
//                        if (preload) {
//                            load.load()
//                        }
                    }
                    onShowCompleted?.invoke()
                }
            )
    }

    fun registerTask(runnable: Runnable) {
        Hot.registerTask(runnable)
    }

    fun unregisterTask(runnable: Runnable) {
        Hot.unregisterTask(runnable)
    }

    private fun preloadAds() {
        runCatching {
            Load.of(AdsCons.POS_OPEN)?.load()
            Load.of(AdsCons.POS_HOME)?.load()
            Load.of(AdsCons.POS_CONNECT)?.load()
            Load.of(AdsCons.POS_RESULT)?.load()
        }
    }

    private object Hot {
        private var startedActivities = 0
        private var backgroundJob: Job? = null
        private var needExecBackgroundTask = false
        private val backgroundTasks = mutableSetOf<Runnable>()

        @MainThread
        fun registerTask(runnable: Runnable) {
            backgroundTasks.add(runnable)
        }

        @MainThread
        fun unregisterTask(runnable: Runnable) {
            backgroundTasks.remove(runnable)
        }

        @MainThread
        fun registerAppLifeCallback(app: Application) {
            app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

                }

                override fun onActivityStarted(activity: Activity) {
                    startedActivities++
                    backgroundJob?.cancel()
                    backgroundJob = null
                    if (needExecBackgroundTask) {
                        onHotForeground()
                    }
                }

                override fun onActivityResumed(activity: Activity) {

                }

                override fun onActivityPaused(activity: Activity) {

                }

                override fun onActivityStopped(activity: Activity) {
                    startedActivities--
                    if (startedActivities <= 0) {
                        onHotBackground()
                    }
                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

                }

                override fun onActivityDestroyed(activity: Activity) {

                }

            })
        }

        private fun onHotForeground() {
            if (ActivityUtils.getActivityList().isNotEmpty()) {
                ActivityUtils.getTopActivity()?.let {
                    it.startActivity(Intent(it, SplashActivity::class.java))
                }
            }
            val it = backgroundTasks.iterator()
            while (it.hasNext()) {
                it.next().run()
            }
            needExecBackgroundTask = false
        }

        private fun onHotBackground() {
            backgroundJob = AppScope.launch {
                delay(3000L)
                needExecBackgroundTask = true
                ActivityUtils.finishActivity(SplashActivity::class.java)
                Show.finishAdActivity()
            }
        }
    }

    private class Load private constructor(private val where: String) {
        companion object {
            private val open by lazy { Load(AdsCons.POS_OPEN) }
            private val home by lazy { Load(AdsCons.POS_HOME) }
            private val connect by lazy { Load(AdsCons.POS_CONNECT) }
            private val back by lazy { Load(AdsCons.POS_BACK) }
            private val result by lazy { Load(AdsCons.POS_RESULT) }

            fun of(where: String): Load? {
                return when (where) {
                    AdsCons.POS_OPEN -> open
                    AdsCons.POS_HOME -> home
                    AdsCons.POS_CONNECT -> connect
                    AdsCons.POS_BACK -> back
                    AdsCons.POS_RESULT -> result
                    else -> null
                }
            }
        }

        private var isLoading = false
        private val localAdJson by lazy {
            runCatching {
                ResourceUtils.readAssets2String("ads.json")
            }.getOrElse { "" }
        }
        private val firebaseAdJson: String
            get() {
                return "si_ads".asSpKeyAndExtract()
            }
        private val inst: MAds
            get() {
                return firebaseAdJson.ifBlank { localAdJson }
                    .toModelOrDefault(MAds::class.java) { EmptyAds }
            }
        private var createdTime = 0L
        var res: Any? = null
            private set

        private fun printLog(content: String) {
            content.printAdLog(where)
        }

        fun load(
            context: Context = SpinApp.self,
            requestCount: Int = 1,
            inst: MAds = this.inst
        ) {
            if (isLoading) {
                printLog("is requesting")
                return
            }

            val cache = res
            val cacheTime = createdTime
            if (cache != null) {
                if (cacheTime > 0L
                    && ((System.currentTimeMillis() - cacheTime) > (1000L * 60L * 60L))
                ) {
                    printLog("cache is expired")
                    clearCache()
                } else {
                    return
                }
            }
            isLoading = true
            printLog("load started")
            doRequest(
                context, when (where) {
                    AdsCons.POS_OPEN -> inst.open
                    AdsCons.POS_CONNECT -> inst.connect
                    AdsCons.POS_HOME -> inst.home
                    AdsCons.POS_RESULT -> inst.result
                    AdsCons.POS_BACK -> inst.back
                    else -> emptyList()
                }.sortedByDescending { it.importance }
            ) {
                val isSuccessful = it != null
                printLog("load complete, result=$isSuccessful")
                if (isSuccessful) {
                    res = it
                    createdTime = System.currentTimeMillis()
                }
                isLoading = false
                if (!isSuccessful && where == AdsCons.POS_OPEN && requestCount < 2) {
                    load(context, requestCount + 1, inst)
                }
            }
        }

        private fun doRequest(
            context: Context,
            units: List<MAd>,
            startIndex: Int = 0,
            callback: ((result: Any?) -> Unit)
        ) {
            val unit = units.getOrNull(startIndex)
            if (unit == null) {
                callback(null)
                return
            }
            printLog("on request: $unit")
            GoogleAds(where).load(context, unit) {
                if (it == null)
                    doRequest(context, units, startIndex + 1, callback)
                else
                    callback(it)
            }
        }

        fun clearCache() {
            res = null
            createdTime = 0L
        }
    }

    private class Show private constructor(private val where: String) {
        companion object {
            private var isShowingFullScreen = false

            fun of(where: String): Show {
                return Show(where)
            }

            fun finishAdActivity() {
                GoogleAds.finishAdActivity()
            }
        }

        fun showFullScreen(
            context: AppCompatActivity,
            res: Any,
            callback: () -> Unit
        ) {
            if (isShowingFullScreen || !context.isVisible()) {
                callback()
                return
            }
            isShowingFullScreen = true
            GoogleAds(where)
                .showFullScreen(
                    context = context,
                    res = res,
                    callback = {
                        isShowingFullScreen = false
                        callback()
                    }
                )
        }

        fun showNativeOf(
            nativeRoot: View,
            res: Any,
            callback: () -> Unit
        ) {
            GoogleAds(where)
                .showNativeOf(
                    nativeRoot = nativeRoot,
                    res = res,
                    callback = callback
                )
        }
    }

    private class GoogleAds(private val where: String) {
        private class GoogleFullScreenCallback(
            private val where: String,
            private val callback: () -> Unit
        ) : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                "dismissed".printAdLog(where)
                onAdComplete()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                "fail to show, message=${p0.message}]".printAdLog(where)
                onAdComplete()
            }

            private fun onAdComplete() {
                callback()
            }

            override fun onAdShowedFullScreenContent() {
                "showed".printAdLog(where)
            }
        }

        companion object {
            fun init(context: Context, onInitialized: () -> Unit) {
                MobileAds.initialize(context) {
                    onInitialized()
                }
            }

            fun finishAdActivity() {
                ActivityUtils.finishActivity(AdActivity::class.java)
            }
        }

        fun load(
            context: Context,
            unit: MAd,
            callback: ((result: Any?) -> Unit)
        ) {
            val requestContext = context.applicationContext
            when (unit.way) {
                AdsCons.WAY_OPEN -> {
                    AppOpenAd.load(
                        requestContext,
                        unit.dataId,
                        AdRequest.Builder().build(),
                        AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                        object :
                            AppOpenAd.AppOpenAdLoadCallback() {
                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                "request fail: ${loadAdError.message}".printAdLog(where)
                                callback(null)
                            }

                            override fun onAdLoaded(appOpenAd: AppOpenAd) {
                                callback(appOpenAd)
                                myUnit_O = beforeLoadLinkSettingsBa(unit)
                                appOpenAd.setOnPaidEventListener { adValue ->
                                    KLog.e("TBA", "开屏页--开屏广告开始上报${where}")
                                    SpinOkHttpUtils.postAdEvent(
                                        adValue,
                                        appOpenAd.responseInfo, myUnit_O, unit.way, where
                                    )
                                }
                            }
                        })
                }
                AdsCons.WAY_INTER -> {
                    InterstitialAd.load(
                        requestContext,
                        unit.dataId,
                        AdRequest.Builder().build(),
                        object : InterstitialAdLoadCallback() {
                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                "request fail: ${loadAdError.message}".printAdLog(where)
                                callback(null)
                            }

                            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                                callback(interstitialAd)
                                if (where == "si_c") {
                                    myUnit_C = beforeLoadLinkSettingsBa(unit)
                                } else {
                                    myUnit_B = beforeLoadLinkSettingsBa(unit)
                                }
                                interstitialAd.setOnPaidEventListener { adValue ->
                                    KLog.e("TBA", "插屏页--插屏广告开始上报${where}")
                                    if (where == "si_c") {
                                        SpinOkHttpUtils.postAdEvent(
                                            adValue,
                                            interstitialAd.responseInfo, myUnit_C, unit.way, where
                                        )
                                    } else {
                                        SpinOkHttpUtils.postAdEvent(
                                            adValue,
                                            interstitialAd.responseInfo, myUnit_B, unit.way, where
                                        )
                                    }

                                }
                            }
                        }
                    )
                }
                AdsCons.WAY_NATIVE -> {
                    AdLoader.Builder(requestContext, unit.dataId)
                        .forNativeAd {
                            callback(it)
                            it.setOnPaidEventListener { adValue ->
                                KLog.e("TBA", "原生广告-----开始上报=${where}")
                                it.responseInfo?.let { it1 ->
                                    val data = if (where == "si_h") {
                                        myUnit_H
                                    } else {
                                        myUnit_R
                                    }
                                    SpinOkHttpUtils.postAdEvent(
                                        adValue,
                                        it1, data, unit.way, where
                                    )
                                }
                                loadOf(where)
                            }
                        }
                        .withAdListener(object : AdListener() {
                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                "request fail: ${loadAdError.message}".printAdLog(where)
                                callback(null)
                            }

                            override fun onAdLoaded() {
                                super.onAdLoaded()
                                KLog.e("TBA", "原生广告加载成功")
                                if (where == "si_h") {
                                    myUnit_H=beforeLoadLinkSettingsBa(unit)
                                } else {
                                    myUnit_R=beforeLoadLinkSettingsBa(unit)
                                }
                            }
                        })
                        .withNativeAdOptions(
                            NativeAdOptions.Builder()
                                .setAdChoicesPlacement(
                                    when (where) {
                                        AdsCons.POS_HOME -> NativeAdOptions.ADCHOICES_TOP_RIGHT
                                        AdsCons.POS_RESULT -> NativeAdOptions.ADCHOICES_TOP_LEFT
                                        else -> NativeAdOptions.ADCHOICES_BOTTOM_LEFT
                                    }
                                )
                                .build()
                        )
                        .build()
                        .loadAd(AdRequest.Builder().build())
                }
                else -> {
                    callback(null)
                }
            }
        }

        fun showFullScreen(
            context: AppCompatActivity,
            res: Any,
            callback: () -> Unit
        ) {
            when (res) {
                is AppOpenAd -> {
                    myUnit_O = afterLoadLinkSettingsBa(myUnit_O)
                    res.fullScreenContentCallback = GoogleFullScreenCallback(where, callback)
                    res.show(context)
                }
                is InterstitialAd -> {
                    val localVpnBootData = SpinUtils.getScenarioConfiguration()
                    val blacklistUser = getAppMmkv().decodeBool(Constant.BLACKLIST_USER_SPIN, false)
                    KLog.d(logTagSpin, "bubble_cloak---${localVpnBootData.spin_lock}。。。")
                    KLog.d(logTagSpin, "blacklist_user---${blacklistUser}。。。")
                    if (blacklistUser && localVpnBootData.spin_lock == "1") {
                        KLog.d(logTagSpin, "根据黑名单屏蔽插屏广告。。。")
                        callback.invoke()
                        return
                    }
                    if (!isBlockScreenAds(localVpnBootData.spin_show)) {
                        KLog.d(logTagSpin, "根据买量屏蔽插屏广告。。。")
                        callback.invoke()
                        return
                    }
                    if (where == "si_c") {
                        myUnit_C =afterLoadLinkSettingsBa(myUnit_C)
                    } else {
                        myUnit_B = afterLoadLinkSettingsBa(myUnit_C)
                    }
                    res.fullScreenContentCallback = GoogleFullScreenCallback(where, callback)
                    res.show(context)

                }
                else -> callback()
            }
        }

        fun showNativeOf(
            nativeRoot: View,
            res: Any,
            callback: () -> Unit
        ) {
            val nativeAd = res as? NativeAd ?: return
            nativeRoot.findViewById<View>(R.id.ad_cover)?.visibility = View.GONE
            val nativeAdView =
                nativeRoot.findViewById<NativeAdView>(R.id.ad_view) ?: return
            nativeAdView.visibility = View.VISIBLE
            nativeRoot.findViewById<MediaView>(R.id.ad_media)?.let { mediaView ->
                nativeAdView.mediaView = mediaView
                nativeAd.mediaContent?.let { mediaContent ->
                    mediaView.setMediaContent(mediaContent)
                    mediaView.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
                }
            }

            nativeRoot.findViewById<TextView>(R.id.ad_title)?.let { titleView ->
                nativeAdView.headlineView = titleView
                titleView.text = nativeAd.headline
            }

            nativeRoot.findViewById<TextView>(R.id.ad_body)?.let { bodyView ->
                nativeAdView.bodyView = bodyView
                bodyView.text = nativeAd.body
            }

            nativeRoot.findViewById<TextView>(R.id.ad_action)?.let { actionView ->
                nativeAdView.callToActionView = actionView
                actionView.text = nativeAd.callToAction
            }

            nativeRoot.findViewById<ImageView>(R.id.ad_icon)?.let { iconView ->
                nativeAdView.iconView = iconView
                iconView.setImageDrawable(nativeAd.icon?.drawable)
            }
            KLog.e("TBA", "原生广告开始展示")
            if (where == "si_h") {
                myUnit_H =afterLoadLinkSettingsBa(myUnit_H)
            } else {
                myUnit_R = afterLoadLinkSettingsBa(myUnit_R)
            }
            nativeAdView.setNativeAd(nativeAd)
            "showed".printAdLog(where)
            callback()
        }
    }

}