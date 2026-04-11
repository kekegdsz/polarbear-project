package com.undersky.androidim.feature.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.undersky.androidim.feature.home.databinding.ItemConversationBinding
import com.undersky.business.user.DirectoryUserDto
import com.undersky.business.user.UserSession
import com.undersky.im.core.api.ConversationItem
import com.undersky.androidim.shared.ui.bindPresenceLabel

class ConversationAdapter(
    private var session: UserSession,
    private val onOpen: (ConversationItem) -> Unit
) : ListAdapter<ConversationItem, ConversationAdapter.Vh>(Diff) {

    private var directoryById: Map<Long, DirectoryUserDto> = emptyMap()
    /** 与 MainTabsViewModel.userPresence 同步（仅实时 IM；缺省视为离线，不用通讯录缓存 online） */
    private var imPresence: Map<Long, Boolean> = emptyMap()

    fun updateSession(s: UserSession) {
        session = s
    }

    fun updateDirectory(users: List<DirectoryUserDto>) {
        directoryById = users.associateBy { it.id }
        notifyDataSetChanged()
    }

    fun updateImPresence(map: Map<Long, Boolean>) {
        imPresence = map
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Vh(binding)
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        holder.bind(getItem(position), session, directoryById, imPresence, onOpen)
    }

    class Vh(private val binding: ItemConversationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: ConversationItem,
            session: UserSession,
            directoryById: Map<Long, DirectoryUserDto>,
            imPresence: Map<Long, Boolean>,
            onOpen: (ConversationItem) -> Unit
        ) {
            val title = when (item.convType) {
                "P2P" -> {
                    val peer = item.peerUserId ?: 0L
                    if (peer == session.userId) {
                        session.nickname?.takeIf { it.isNotBlank() }
                            ?: session.username?.takeIf { it.isNotBlank() }
                            ?: "我"
                    } else {
                        val u = directoryById[peer]
                        u?.nickname?.takeIf { it.isNotBlank() }
                            ?: u?.username?.takeIf { it.isNotBlank() }
                            ?: "用户 $peer"
                    }
                }
                "GROUP" -> item.groupName?.takeIf { it.isNotBlank() }
                    ?: "群聊 ${item.groupId ?: ""}"
                else -> "会话"
            }
            binding.avatarLetter.text = title.take(1)
            binding.textTitle.text = title
            binding.textPreview.text = item.lastMessage.body
            binding.textTime.text = item.lastMessage.createdAt?.takeLast(8).orEmpty()
            val p2pPeer = item.convType == "P2P" && (item.peerUserId ?: 0L) > 0L &&
                (item.peerUserId ?: 0L) != session.userId
            if (p2pPeer) {
                val peerId = item.peerUserId!!
                // 仅用 IM 推送的 userPresence；不要用通讯录里可能过期的 online（重连清空 map 后快照只含在线用户）
                val online = imPresence[peerId]
                binding.textPresence.bindPresenceLabel(online, show = true)
            } else {
                binding.textPresence.bindPresenceLabel(null, show = false)
            }
            val u = item.unreadCount
            if (u <= 0) {
                binding.badgeUnread.visibility = View.GONE
            } else {
                binding.badgeUnread.visibility = View.VISIBLE
                binding.badgeUnread.text = if (u > 99) "99+" else u.toString()
            }
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
