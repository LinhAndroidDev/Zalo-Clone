package com.example.messageapp.base

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

abstract class BaseAdapter<T : Any, VB : ViewDataBinding> :
    RecyclerView.Adapter<BaseAdapter.BaseViewHolder<VB>>() {
    var items = mutableListOf<T>()

    class BaseViewHolder<VB : ViewDataBinding>(val v: VB) : RecyclerView.ViewHolder(v.root)

    abstract fun getLayout(): Int

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        BaseViewHolder<VB>(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                getLayout(),
                parent,
                false
            )
        )

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun resetList(list: ArrayList<T>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun addItems(list: ArrayList<T>) {
        val startPosition = items.size
        items.addAll(list)
        notifyItemRangeChanged(startPosition, list.size)
    }

    class BaseDiffCallback<T>(private val oldList: List<T>, private val newList: List<T>) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}