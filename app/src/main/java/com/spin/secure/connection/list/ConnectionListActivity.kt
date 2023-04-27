package com.spin.secure.connection.list

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.blankj.utilcode.util.BarUtils
import com.spin.secure.R
import com.spin.secure.ads.AdLoadUtils
import com.spin.secure.ads.AdsCons
import com.spin.secure.base.BaseActivity
import com.spin.secure.click
import com.spin.secure.databinding.ActivityConnListBinding
import com.spin.secure.main.core.*
import com.spin.secure.main.core.connector.BaseConnector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConnectionListActivity : BaseActivity<ActivityConnListBinding, ConnectionListViewModel>() {
    override val model by viewModels<ConnectionListViewModel>()
    override val implLayoutResId = R.layout.activity_conn_list

    private val adapter by lazy {
        ConnectionListAdapter(this) {
            onNewConnection(it)
        }
    }
    private val connectionRepository = ConnectionRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdLoadUtils.loadOf(
            where = AdsCons.POS_BACK
        )
        val that = this
        with(binding) {
            BarUtils.addMarginTopEqualStatusBarHeight(topLayout)
            btnBack.click { onBackPressed() }
            with(rv) {
                adapter = that.adapter
                layoutManager = LinearLayoutManager(that)
            }
        }
        with(model) {
            showBackAd.observe(that) {
                showBackAd(it)
            }
        }
        initRvData()
    }

    private fun showBackAd(res: Any) {
        AdLoadUtils.showFullScreenOf(
            where = AdsCons.POS_BACK,
            context = this,
            res = res,
            onShowCompleted = {
                finish()
            }
        )
    }

    override fun onBackPressed() {
        AdLoadUtils.resultOf(
            where = AdsCons.POS_BACK
        )?.let {
            model.showBackAd.value = it
            return
        }
        super.onBackPressed()
    }

    private fun initRvData() {
        lifecycleScope.launch(Dispatchers.Main) {
            val cur = BaseConnector.connectData
            adapter.setNewInstance(
                withContext(Dispatchers.IO) {
                    (listOf(SmartConnection) + connectionRepository.listAll())
                        .map {
                            ConnectionListAdapter.Item(it, it.contentEqual(cur))
                        }
                        .toMutableList()
                }
            )
        }
    }

    private fun onNewConnection(connectData: MConnection) {
        val isConnected = BaseConnector.state == ConnectState.Connected
        val cur = BaseConnector.connectData
        if (isConnected) {
            if (cur != connectData) {
                val that = this
                MaterialDialog(this)
                    .show {
                        message(text = "Are you sure to disconnect current server?")
                        positiveButton(text = "Disconnect", click = {
                            finishForNewData(
                                connectData,
                                IntentCons.VALUE_SERVERS_CONNECT_ACTION_DISCONNECT
                            )
                        })
                        negativeButton(text = "Cancel")
                        lifecycleOwner(that)
                    }
            }
            return
        }
        finishForNewData(connectData, IntentCons.VALUE_SERVERS_CONNECT_ACTION_CONNECT)
    }

    private fun finishForNewData(connectData: MConnection, action: String) {
        setResult(
            IntentCons.REQ_CODE_CONNECT_SERVERS,
            Intent().apply {
                putExtra(IntentCons.KEY_SERVERS_CONNECT_ACTION, action)
                putExtra(IntentCons.KEY_SERVERS_CONNECT_DATA, connectData)
            })
        finish()
    }
}