package com.spin.secure.connection.list

import androidx.databinding.BaseObservable
import androidx.lifecycle.LifecycleOwner
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseDataBindingHolder
import com.spin.secure.BR
import com.spin.secure.R
import com.spin.secure.click
import com.spin.secure.databinding.ItemConnectionBinding
import com.spin.secure.main.core.MConnection
import com.spin.secure.main.core.countryIconResId
import com.spin.secure.main.core.name

class ConnectionListAdapter(
    private val owner: LifecycleOwner,
    private val onConnectionSelect: (c: MConnection) -> Unit
) :
    BaseQuickAdapter<ConnectionListAdapter.Item, BaseDataBindingHolder<ItemConnectionBinding>>(R.layout.item_connection) {
    class Item(
        val connection: MConnection,
        val checked: Boolean
    ) : BaseObservable() {
        val countryIconResId: Int
            get() {
                return connection.countryIconResId
            }
        val name: String
            get() {
                return connection.name
            }
    }

    override fun convert(holder: BaseDataBindingHolder<ItemConnectionBinding>, item: Item) {
        with(holder.dataBinding!!) {
            lifecycleOwner = owner
            setVariable(BR.m, item)
            root.click { onConnectionSelect(item.connection) }
        }
    }
}