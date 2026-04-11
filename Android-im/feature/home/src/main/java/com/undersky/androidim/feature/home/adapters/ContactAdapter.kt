package com.undersky.androidim.feature.home.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.undersky.androidim.feature.home.databinding.ItemContactBinding
import com.undersky.business.user.DirectoryUserDto

class ContactAdapter(
    private val onOpenChat: (DirectoryUserDto) -> Unit
) : ListAdapter<DirectoryUserDto, ContactAdapter.Vh>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Vh(binding, onOpenChat)
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        holder.bind(getItem(position))
    }

    class Vh(
        private val binding: ItemContactBinding,
        private val onOpenChat: (DirectoryUserDto) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(u: DirectoryUserDto) {
            val name = u.nickname?.takeIf { it.isNotBlank() }
                ?: u.username?.takeIf { it.isNotBlank() }
                ?: "用户 ${u.id}"
            binding.textTitle.text = name
            binding.textSub.text = u.mobile?.takeIf { it.isNotBlank() } ?: "ID: ${u.id}"
            binding.root.setOnClickListener { onOpenChat(u) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<DirectoryUserDto>() {
        override fun areItemsTheSame(old: DirectoryUserDto, new: DirectoryUserDto): Boolean = old.id == new.id
        override fun areContentsTheSame(old: DirectoryUserDto, new: DirectoryUserDto): Boolean = old == new
    }
}
