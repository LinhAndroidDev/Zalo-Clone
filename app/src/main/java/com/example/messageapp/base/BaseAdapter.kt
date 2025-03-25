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

    fun addItems(list: ArrayList<T>) {
        val startPosition = items.size
        items.addAll(list)
        notifyItemRangeChanged(startPosition, list.size)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateDiffList(newList: List<T>, compareItem: (T, T) -> Boolean, compareContent: (T, T) -> Boolean) {
        val diffResult = DiffUtil.calculateDiff(BaseDiffUtil(items, newList, compareItem, compareContent))
        items.clear()
        items.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    class BaseDiffUtil<T>(
        private val oldList: List<T>,
        private val newList: List<T>,
        private val areItemsTheSame: (T, T) -> Boolean,
        private val areContentsTheSame: (T, T) -> Boolean
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
            return areItemsTheSame(oldList[oldItem], newList[newItem])
        }

        override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
            return areContentsTheSame(oldList[oldItem], newList[newItem])
        }
    }

}