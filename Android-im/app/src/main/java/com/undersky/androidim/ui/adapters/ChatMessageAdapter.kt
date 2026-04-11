package com.undersky.androidim.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.undersky.im.core.api.ChatMessage
import com.undersky.androidim.databinding.ItemChatMessageMineBinding
import com.undersky.androidim.databinding.ItemChatMessageOtherBinding

class ChatMessageAdapter(
    private var selfUserId: Long
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(Diff) {

    companion object {
        private const val TYPE_MINE = 1
        private const val TYPE_OTHER = 2
    }

    fun updateSelfUserId(id: Long) {
        selfUserId = id
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).fromUserId == selfUserId) TYPE_MINE else TYPE_OTHER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_MINE -> MineVh(
                ItemChatMessageMineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> OtherVh(
                ItemChatMessageOtherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is MineVh -> holder.bind(msg)
            is OtherVh -> holder.bind(msg)
        }
    }

    class MineVh(private val binding: ItemChatMessageMineBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: ChatMessage) {
            binding.textBody.text = msg.body
        }
    }

    class OtherVh(private val binding: ItemChatMessageOtherBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: ChatMessage) {
            binding.textBody.text = msg.body
        }
    }

    private object Diff : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(old: ChatMessage, new: ChatMessage) = old.msgId == new.msgId
        override fun areContentsTheSame(old: ChatMessage, new: ChatMessage) = old == new
    }
}
