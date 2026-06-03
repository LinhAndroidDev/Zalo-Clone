package com.example.messageapp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.R
import com.example.messageapp.databinding.ItemCreateGroupMemberBinding
import com.example.messageapp.model.Friend
import com.example.messageapp.utils.FileUtils.loadImg

class CreateGroupMemberAdapter(
    private val onSelectionChanged: () -> Unit = {},
) : RecyclerView.Adapter<CreateGroupMemberAdapter.VH>() {

    private val items = ArrayList<Friend>()
    private val selected = mutableSetOf<String>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(friends: List<Friend>) {
        items.clear()
        items.addAll(friends)
        selected.clear()
        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun selectedFriendIds(): List<String> = selected.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCreateGroupMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(private val binding: ItemCreateGroupMemberBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(friend: Friend) {
            val id = friend.keyAuth
            binding.tvName.text = friend.name
            binding.root.context.loadImg(
                friend.avatar,
                binding.avatar,
                R.drawable.bg_grey_equal,
            )
            binding.checkSelect.setOnCheckedChangeListener(null)
            binding.checkSelect.isChecked = selected.contains(id)
            binding.checkSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selected.add(id) else selected.remove(id)
                onSelectionChanged()
            }
            binding.root.setOnClickListener {
                binding.checkSelect.isChecked = !binding.checkSelect.isChecked
            }
        }
    }
}
