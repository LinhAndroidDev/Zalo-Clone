package com.example.messageapp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.R
import com.example.messageapp.databinding.ItemGroupMemberRemoveBinding
import com.example.messageapp.model.User
import com.example.messageapp.utils.FileUtils.loadImg

class GroupMemberRemoveAdapter(
    private val onRemove: (User) -> Unit,
) : RecyclerView.Adapter<GroupMemberRemoveAdapter.VH>() {

    private val items = ArrayList<User>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(members: List<User>) {
        items.clear()
        items.addAll(members)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemGroupMemberRemoveBinding.inflate(
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

    inner class VH(private val binding: ItemGroupMemberRemoveBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.tvName.text = user.name.orEmpty()
            binding.root.context.loadImg(
                user.avatar.orEmpty(),
                binding.avatar,
                R.drawable.bg_grey_equal,
            )
            binding.btnRemove.setOnClickListener { onRemove(user) }
        }
    }
}
