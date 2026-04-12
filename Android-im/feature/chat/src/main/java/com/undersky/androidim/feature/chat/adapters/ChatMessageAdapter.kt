package com.undersky.androidim.feature.chat.adapters

import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.decode.VideoFrameDecoder
import coil.load
import coil.request.CachePolicy
import coil.request.videoFrameMillis
import coil.transform.RoundedCornersTransformation
import com.undersky.androidim.feature.chat.BubbleContent
import com.undersky.androidim.feature.chat.ChatListItem
import com.undersky.androidim.feature.chat.R
import com.undersky.androidim.feature.chat.databinding.ItemChatMessageMineBinding
import com.undersky.androidim.feature.chat.databinding.ItemChatMessageOtherBinding
import com.undersky.androidim.feature.chat.databinding.ItemChatTimeHeaderBinding
import com.undersky.androidim.feature.chat.parseBubble
import com.undersky.androidim.feature.chat.avatarLetter
import com.undersky.im.core.api.ChatMessage
import com.undersky.im.core.api.resolveImAttachmentUrl
import com.undersky.androidim.feature.chat.formatChatListTime
import com.undersky.androidim.shared.ui.bindPresenceLabel
import android.content.Intent
import android.net.Uri
import java.io.File

class ChatMessageAdapter(
    private var selfUserId: Long,
    private val apiBaseUrl: String,
    /** 点击图片 / 视频气泡：传入消息 id，由界面打开可左右滑浏览的媒体查看器 */
    private val onVisualMediaOpen: (msgId: Long) -> Unit,
    private val onPlayVoice: (String) -> Unit,
    /** 长按气泡：复制等操作（对标微信 / Telegram） */
    private val onBubbleLongPress: ((ChatMessage) -> Unit)? = null,
    /** 点击引用条：跳转到被引用消息（列表内滚动） */
    private val onReplyRefClick: ((Long) -> Unit)? = null,
    /** 长按语音条：循环播放倍速（微信无此快捷方式） */
    private val onVoiceSpeedCycle: (() -> Unit)? = null
) : ListAdapter<ChatListItem, RecyclerView.ViewHolder>(Diff) {

    private fun resolveMedia(raw: String): String = resolveImAttachmentUrl(raw, apiBaseUrl)

    private var onlineByUserId: Map<Long, Boolean> = emptyMap()
    private var playingVoiceUrl: String? = null
    private var highlightMsgId: Long? = null
    private var voiceSpeedLabel: Float = 1f

    companion object {
        private const val TYPE_TIME = 0
        private const val TYPE_MINE = 1
        private const val TYPE_OTHER = 2

    }

    fun updateSelfUserId(id: Long) {
        selfUserId = id
    }

    fun setOnlineByUserId(map: Map<Long, Boolean>) {
        onlineByUserId = map
        notifyDataSetChanged()
    }

    fun setPlayingVoiceUrl(url: String?) {
        if (playingVoiceUrl == url) return
        playingVoiceUrl = url
        notifyDataSetChanged()
    }

    fun updateHighlightMessageId(newId: Long?) {
        val old = highlightMsgId
        highlightMsgId = newId
        fun indexOf(mid: Long?): Int {
            if (mid == null) return RecyclerView.NO_POSITION
            return currentList.indexOfFirst {
                it is ChatListItem.MessageRow && it.message.msgId == mid
            }
        }
        val io = indexOf(old)
        val ni = indexOf(newId)
        if (io != RecyclerView.NO_POSITION) notifyItemChanged(io)
        if (ni != RecyclerView.NO_POSITION && ni != io) notifyItemChanged(ni)
    }

    fun setVoiceSpeedForLabel(speed: Float) {
        val s = speed.coerceAtLeast(0.5f)
        if (voiceSpeedLabel == s) return
        voiceSpeedLabel = s
        notifyDataSetChanged()
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
                is MineVh -> bindMine(holder, item)
                is OtherVh -> bindOther(holder, item)
            }
        }
    }

    private fun bindMine(holder: MineVh, item: ChatListItem.MessageRow) {
        val b = holder.binding
        val showMeta = item.showSenderMeta
        b.avatarWrap.isVisible = showMeta
        b.textNickname.isVisible = showMeta
        b.textNickname.text = item.displayName
        b.avatarLetter.text = avatarLetter(item.displayName)
        val selfOn = onlineByUserId[selfUserId] ?: true
        b.textPresence.bindPresenceLabel(selfOn, show = showMeta)
        bindRich(
            b.replyStrip,
            b.textBody,
            b.panelVisual,
            b.imageVisual,
            b.iconVideoPlay,
            b.textVideoDuration,
            b.panelVoice,
            b.iconVoiceState,
            b.textVoiceDuration,
            b.textFile,
            item.message
        )
        wireBubbleLongPress(b.bubbleInner, item.message)
        applyRowHighlight(holder.itemView, item.message.msgId)
    }

    private fun bindOther(holder: OtherVh, item: ChatListItem.MessageRow) {
        val b = holder.binding
        val showMeta = item.showSenderMeta
        b.avatarWrap.isVisible = showMeta
        b.textNickname.isVisible = showMeta
        b.textNickname.text = item.displayName
        b.avatarLetter.text = avatarLetter(item.displayName)
        val uid = item.message.fromUserId
        val online = onlineByUserId[uid]
        b.textPresence.bindPresenceLabel(online, show = showMeta)
        bindRich(
            b.replyStrip,
            b.textBody,
            b.panelVisual,
            b.imageVisual,
            b.iconVideoPlay,
            b.textVideoDuration,
            b.panelVoice,
            b.iconVoiceState,
            b.textVoiceDuration,
            b.textFile,
            item.message
        )
        wireBubbleLongPress(b.bubbleInner, item.message)
        applyRowHighlight(holder.itemView, item.message.msgId)
    }

    private fun applyRowHighlight(row: View, msgId: Long) {
        row.setBackgroundColor(
            if (msgId == highlightMsgId) 0x280F9D74 else Color.TRANSPARENT
        )
    }

    private fun wireBubbleLongPress(bubbleInner: View, message: ChatMessage) {
        val cb = onBubbleLongPress
        if (cb == null) {
            bubbleInner.setOnLongClickListener(null)
            bubbleInner.isLongClickable = false
            return
        }
        bubbleInner.isLongClickable = true
        bubbleInner.setOnLongClickListener {
            cb(message)
            true
        }
    }

    /** 自己发送且缓存文件仍在时优先用本地文件给 Coil；点击/预览仍走网络 URL 便于分享与兼容。 */
    private fun visualCoilData(message: ChatMessage, remoteUrl: String): Pair<Any, String> {
        val remote = resolveMedia(remoteUrl)
        val mine = message.fromUserId == selfUserId
        val p = message.localMediaPath
        if (mine && !p.isNullOrBlank()) {
            val f = File(p)
            if (f.exists() && f.canRead()) return f to remote
        }
        return remote to remote
    }

    private fun bindRich(
        replyStrip: TextView,
        textBody: TextView,
        panelVisual: FrameLayout,
        imageVisual: ImageView,
        iconVideoPlay: ImageView,
        textVideoDuration: TextView,
        panelVoice: LinearLayout,
        iconVoiceState: TextView,
        textVoiceDuration: TextView,
        textFile: TextView,
        message: ChatMessage
    ) {
        val body = message.body
        val parsed = parseBubble(body)
        val ref = parsed.reply
        if (ref != null && ref.refMsgId > 0L) {
            replyStrip.isVisible = true
            val who = ref.fromLabel?.takeIf { it.isNotBlank() } ?: "消息"
            val pv = ref.preview.takeIf { it.isNotBlank() } ?: "…"
            replyStrip.text = "▎ $who：$pv"
            val jump = onReplyRefClick
            if (jump != null) {
                replyStrip.setOnClickListener { jump(ref.refMsgId) }
            } else {
                replyStrip.setOnClickListener(null)
            }
        } else {
            replyStrip.isVisible = false
            replyStrip.setOnClickListener(null)
        }

        val ctx = textBody.context
        val cornerPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            10f,
            ctx.resources.displayMetrics
        )

        textBody.isVisible = false
        panelVisual.isVisible = false
        panelVoice.isVisible = false
        textFile.isVisible = false
        panelVisual.setOnClickListener(null)
        panelVoice.setOnClickListener(null)
        textFile.setOnClickListener(null)

        when (val c = parsed.content) {
            is BubbleContent.PlainText -> {
                textBody.isVisible = true
                textBody.text = c.text
            }
            is BubbleContent.ImageMsg -> {
                panelVisual.isVisible = true
                iconVideoPlay.isVisible = false
                textVideoDuration.isVisible = false
                val (coilData, openUrl) = visualCoilData(message, c.url)
                imageVisual.load(coilData) {
                    allowHardware(false)
                    diskCachePolicy(CachePolicy.ENABLED)
                    memoryCachePolicy(CachePolicy.ENABLED)
                    crossfade(coilData !is File)
                    placeholder(R.drawable.bg_media_placeholder)
                    error(R.drawable.bg_media_placeholder)
                    transformations(RoundedCornersTransformation(cornerPx))
                }
                panelVisual.setOnClickListener { onVisualMediaOpen(message.msgId) }
            }
            is BubbleContent.VideoMsg -> {
                panelVisual.isVisible = true
                iconVideoPlay.isVisible = true
                textVideoDuration.isVisible = true
                textVideoDuration.text = formatVideoDurationLabel(c.durationMs)
                val (coilData, openUrl) = visualCoilData(message, c.url)
                imageVisual.load(coilData) {
                    allowHardware(false)
                    diskCachePolicy(CachePolicy.ENABLED)
                    memoryCachePolicy(CachePolicy.ENABLED)
                    decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
                    videoFrameMillis(1L)
                    crossfade(coilData !is File)
                    placeholder(R.drawable.bg_media_placeholder)
                    error(R.drawable.bg_media_placeholder)
                    transformations(RoundedCornersTransformation(cornerPx))
                }
                panelVisual.setOnClickListener { onVisualMediaOpen(message.msgId) }
            }
            is BubbleContent.VoiceMsg -> {
                panelVoice.isVisible = true
                val voiceUrl = resolveMedia(c.url)
                val sec = ((c.durationMs + 500) / 1000).toInt().coerceAtLeast(1)
                val widthDp = (108 + sec * 5).coerceIn(120, 260)
                panelVoice.layoutParams = panelVoice.layoutParams.apply {
                    width = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        widthDp.toFloat(),
                        ctx.resources.displayMetrics
                    ).toInt()
                }
                val playing = voiceUrl == playingVoiceUrl
                iconVoiceState.text = if (playing) "❚❚" else "▶"
                val speedHint = if (voiceSpeedLabel != 1f) {
                    " · ${trimSpeedLabel(voiceSpeedLabel)}x"
                } else {
                    ""
                }
                textVoiceDuration.text = if (playing) {
                    "${sec}s · 播放中$speedHint"
                } else {
                    "${sec}s · 点击播放$speedHint"
                }
                panelVoice.alpha = if (playing) 0.92f else 1f
                panelVoice.setOnClickListener { onPlayVoice(voiceUrl) }
                val speedCb = onVoiceSpeedCycle
                if (speedCb != null) {
                    panelVoice.isLongClickable = true
                    panelVoice.setOnLongClickListener {
                        speedCb()
                        true
                    }
                } else {
                    panelVoice.setOnLongClickListener(null)
                    panelVoice.isLongClickable = false
                }
            }
            is BubbleContent.FileMsg -> {
                textFile.isVisible = true
                textFile.text = "📎 ${c.name}"
                val fileUrl = resolveMedia(c.url)
                textFile.setOnClickListener {
                    textFile.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl)))
                }
            }
        }
    }

    private fun trimSpeedLabel(s: Float): String {
        val t = String.format(java.util.Locale.US, "%.1f", s).trimEnd('0').trimEnd('.')
        return if (t.isEmpty()) "1" else t
    }

    private fun formatVideoDurationLabel(ms: Long): String {
        if (ms <= 0L) return ""
        val t = (ms + 500) / 1000
        val m = t / 60
        val s = t % 60
        return if (m > 0) String.format("%d:%02d", m, s) else String.format("0:%02d", t.coerceAtMost(59))
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
