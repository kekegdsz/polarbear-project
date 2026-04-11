package com.undersky.androidim.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.undersky.androidim.data.ConversationItem
import com.undersky.androidim.data.UserSession
import com.undersky.androidim.databinding.ItemConversationBinding

class ConversationAdapter(
    private var session: UserSession,
    private val onOpen: (ConversationItem) -> Unit
) : ListAdapter<ConversationItem, ConversationAdapter.Vh>(Diff) {

    fun updateSession(s: UserSession) {
        session = s
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Vh(binding)
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        holder.bind(getItem(position), session, onOpen)
    }

    class Vh(private val binding: ItemConversationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ConversationItem, session: UserSession, onOpen: (ConversationItem) -> Unit) {
            val title = when (item.convType) {
                "P2P" -> {
                    val peer = item.peerUserId ?: 0L
                    if (peer == session.userId) "我" else "用户 $peer"
                }
                "GROUP" -> "群聊 ${item.groupId ?: ""}"
                else -> "会话"
            }
            binding.avatarLetter.text = title.take(1)
            binding.textTitle.text = title
            binding.textPreview.text = item.lastMessage.body
            binding.textTime.text = item.lastMessage.createdAt?.takeLast(8).orEmpty()
            binding.root.setOnClickListener { onOpen(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<ConversationItem>() {
        override fun areItemsTheSame(old: ConversationItem, new: ConversationItem): Boolean {
            val oldKey = when (old.convType) {
                "P2P" -> "p2p-${old.peerUserId}"
                "GROUP" -> "g-${old.groupId}"
                else -> old.lastMessage.msgId.toString()
            }
            val newKey = when (new.convType) {
                "P2P" -> "p2p-${new.peerUserId}"
                "GROUP" -> "g-${new.groupId}"
                else -> new.lastMessage.msgId.toString()
            }
            return oldKey == newKey
        }

        override fun areContentsTheSame(old: ConversationItem, new: ConversationItem): Boolean =
            old == new
    }
}
