package com.undersky.androidim.feature.chat.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.undersky.androidim.feature.chat.ChatListItem
import com.undersky.androidim.feature.chat.databinding.ItemChatMessageMineBinding
import com.undersky.androidim.feature.chat.databinding.ItemChatMessageOtherBinding
import com.undersky.androidim.feature.chat.databinding.ItemChatTimeHeaderBinding
import com.undersky.androidim.feature.chat.avatarLetter
import com.undersky.androidim.feature.chat.formatChatListTime

class ChatMessageAdapter(
    private var selfUserId: Long
) : ListAdapter<ChatListItem, RecyclerView.ViewHolder>(Diff) {

    companion object {
        private const val TYPE_TIME = 0
        private const val TYPE_MINE = 1
        private const val TYPE_OTHER = 2
    }

    fun updateSelfUserId(id: Long) {
        selfUserId = id
    }

    override fun getItemViewType(position: Int): Int =
        when (val item = getItem(position)) {
            is ChatListItem.TimeHeader -> TYPE_TIME
            is ChatListItem.MessageRow ->
                if (item.message.fromUserId == selfUserId) TYPE_MINE else TYPE_OTHER
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TIME -> TimeVh(ItemChatTimeHeaderBinding.inflate(inflater, parent, false))
            TYPE_MINE -> MineVh(ItemChatMessageMineBinding.inflate(inflater, parent, false))
            else -> OtherVh(ItemChatMessageOtherBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatListItem.TimeHeader ->
                (holder as TimeVh).binding.textTime.text = formatChatListTime(item.epochMillis)
            is ChatListItem.MessageRow -> when (holder) {
                is MineVh -> {
                    holder.binding.textNickname.text = item.displayName
                    holder.binding.avatarLetter.text = avatarLetter(item.displayName)
                    holder.binding.textBody.text = item.message.body
                }
                is OtherVh -> {
                    holder.binding.textNickname.text = item.displayName
                    holder.binding.avatarLetter.text = avatarLetter(item.displayName)
                    holder.binding.textBody.text = item.message.body
                }
            }
        }
    }

    class TimeVh(val binding: ItemChatTimeHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class MineVh(val binding: ItemChatMessageMineBinding) : RecyclerView.ViewHolder(binding.root)
    class OtherVh(val binding: ItemChatMessageOtherBinding) : RecyclerView.ViewHolder(binding.root)

    private object Diff : DiffUtil.ItemCallback<ChatListItem>() {
        override fun areItemsTheSame(old: ChatListItem, new: ChatListItem): Boolean = when {
            old is ChatListItem.TimeHeader && new is ChatListItem.TimeHeader ->
                old.anchorMsgId == new.anchorMsgId
            old is ChatListItem.MessageRow && new is ChatListItem.MessageRow ->
                old.message.msgId == new.message.msgId
            else -> false
        }

        override fun areContentsTheSame(old: ChatListItem, new: ChatListItem): Boolean = old == new
    }
}
