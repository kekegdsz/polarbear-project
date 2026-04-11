package com.undersky.androidim.feature.chat.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.undersky.androidim.feature.chat.databinding.ItemChatMessageMineBinding
import com.undersky.androidim.feature.chat.databinding.ItemChatMessageOtherBinding
import com.undersky.im.core.api.ChatMessage

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_MINE) {
            MineVh(ItemChatMessageMineBinding.inflate(inflater, parent, false))
        } else {
            OtherVh(ItemChatMessageOtherBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val m = getItem(position)
        when (holder) {
            is MineVh -> holder.binding.textBody.text = m.body
            is OtherVh -> holder.binding.textBody.text = m.body
        }
    }

    class MineVh(val binding: ItemChatMessageMineBinding) : RecyclerView.ViewHolder(binding.root)
    class OtherVh(val binding: ItemChatMessageOtherBinding) : RecyclerView.ViewHolder(binding.root)

    private object Diff : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(old: ChatMessage, new: ChatMessage): Boolean = old.msgId == new.msgId
        override fun areContentsTheSame(old: ChatMessage, new: ChatMessage): Boolean = old == new
    }
}
