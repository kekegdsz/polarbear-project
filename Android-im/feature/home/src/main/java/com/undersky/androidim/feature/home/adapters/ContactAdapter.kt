package com.undersky.androidim.feature.home.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.undersky.androidim.feature.home.databinding.ItemContactBinding
import com.undersky.business.user.DirectoryUserDto
import com.undersky.androidim.shared.ui.bindPresenceLabel

class ContactAdapter(
    private val onOpenChat: (DirectoryUserDto) -> Unit
) : ListAdapter<DirectoryUserDto, ContactAdapter.Vh>(Diff) {

    private var imPresence: Map<Long, Boolean> = emptyMap()

    fun updateImPresence(map: Map<Long, Boolean>) {
        imPresence = map
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Vh(binding, onOpenChat)
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        holder.bind(getItem(position), imPresence)
    }

    class Vh(
        private val binding: ItemContactBinding,
        private val onOpenChat: (DirectoryUserDto) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(u: DirectoryUserDto, imPresence: Map<Long, Boolean>) {
            val name = u.nickname?.takeIf { it.isNotBlank() }
                ?: u.username?.takeIf { it.isNotBlank() }
                ?: "用户 ${u.id}"
            binding.textTitle.text = name
            val letter = name.trim().take(1).ifEmpty { "?" }
            binding.avatarLetter.text = letter
            binding.textSub.text = u.mobile?.takeIf { it.isNotBlank() } ?: "ID: ${u.id}"
            val online = imPresence[u.id] ?: u.online
            binding.textPresence.bindPresenceLabel(online, show = true)
            binding.root.setOnClickListener { onOpenChat(u) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<DirectoryUserDto>() {
        override fun areItemsTheSame(old: DirectoryUserDto, new: DirectoryUserDto): Boolean = old.id == new.id
        override fun areContentsTheSame(old: DirectoryUserDto, new: DirectoryUserDto): Boolean = old == new
    }
}
