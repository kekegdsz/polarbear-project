package com.undersky.androidim.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.undersky.business.user.DirectoryUserDto
import com.undersky.androidim.databinding.ItemContactBinding

class ContactAdapter(
    private val onClick: (DirectoryUserDto) -> Unit
) : ListAdapter<DirectoryUserDto, ContactAdapter.Vh>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Vh(binding, onClick)
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        holder.bind(getItem(position))
    }

    class Vh(
        private val binding: ItemContactBinding,
        private val onClick: (DirectoryUserDto) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(u: DirectoryUserDto) {
            val title = u.username?.takeIf { it.isNotBlank() } ?: "用户 ${u.id}"
            val sub = buildString {
                append("ID ${u.id}")
                u.mobile?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
            }
            binding.avatarLetter.text = title.take(1)
            binding.textTitle.text = title
            binding.textSub.text = sub
            binding.root.setOnClickListener { onClick(u) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<DirectoryUserDto>() {
        override fun areItemsTheSame(old: DirectoryUserDto, new: DirectoryUserDto) = old.id == new.id
        override fun areContentsTheSame(old: DirectoryUserDto, new: DirectoryUserDto) = old == new
    }
}
