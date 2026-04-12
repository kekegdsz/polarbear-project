package com.undersky.androidim.feature.chat

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import com.undersky.androidim.feature.chat.R
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.undersky.androidim.bootstrap.BootstrapApplication
import com.undersky.androidim.bootstrap.session.SessionViewModel
import com.undersky.androidim.feature.chat.adapters.ChatMessageAdapter
import com.undersky.androidim.feature.chat.adapters.ChatSwipeReplyItemTouchHelper
import com.undersky.androidim.feature.chat.media.ChatMediaViewerActivity
import com.undersky.androidim.feature.chat.media.buildChatVisualMediaPages
import com.undersky.androidim.feature.chat.toChatListItems
import com.undersky.androidim.feature.chat.databinding.FragmentChatBinding
import com.undersky.androidim.feature.home.MainTabsViewModel
import com.undersky.core.common.applyWindowInsetsPadding
import com.undersky.androidim.shared.ui.R as SharedR
import com.undersky.im.core.api.ChatMessage
import com.undersky.im.core.api.ImConnectionState
import com.undersky.im.core.api.chatMessagePreviewLabel
import com.undersky.im.core.api.resolveImAttachmentUrl
import com.undersky.im.core.local.ChatConvKeys
import kotlinx.coroutines.launch
import java.io.File

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    private val sessionViewModel: SessionViewModel by activityViewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    private val mainTabsViewModel: MainTabsViewModel by activityViewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadImageFromUri(it) }
    }
    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadVideoFromUri(it) }
    }
    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadFileFromUri(it) }
    }
    private var cameraUri: Uri? = null
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let { viewModel.uploadImageFromUri(it) }
    }
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCameraInternal() else Toast.makeText(requireContext(), "需要相机权限", Toast.LENGTH_SHORT).show()
        }
    private val requestAudioPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startVoiceRecordDialog() else Toast.makeText(requireContext(), "需要麦克风权限", Toast.LENGTH_SHORT).show()
        }

    private var voiceRecorder: MediaRecorder? = null
    private var voiceFile: File? = null
    private var voiceStartMs: Long = 0L

    private var adapter: ChatMessageAdapter? = null
    private var swipeReplyTouch: ItemTouchHelper? = null
    private var didBindChat = false
    private var draftRestored = false
    private var peerUserId: Long = -1L
    private var groupId: Long = -1L
    private var lastMessages: List<ChatMessage> = emptyList()
    private var lastDisplayNames: Map<Long, String> = emptyMap()

    /** 聊天记录查找：列表中的 adapter position */
    private var chatSearchHits: List<Int> = emptyList()
    private var chatSearchHitIdx: Int = 0

    /** 上一帧 IME 底边 inset，用于检测键盘从关闭到打开 */
    private var lastImeBottomPx: Int = 0

    private var subtitleOnlineMap: Map<Long, Boolean> = emptyMap()
    private var subtitleGroupDetail: GroupDetailUi? = null

    private fun services() = (requireActivity().application as BootstrapApplication).services

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyWindowInsetsPadding(padTop = true, padBottom = true) { insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val keyboardOpened = imeBottom > 0 && lastImeBottomPx == 0
            lastImeBottomPx = imeBottom
            if (keyboardOpened) {
                scrollListToBottomAfterIme()
            }
        }
        val peer = arguments?.getLong("peerUserId") ?: -1L
        val group = arguments?.getLong("groupId") ?: -1L
        peerUserId = peer
        groupId = group
        val titleFb = arguments?.getString("titleFallback").orEmpty()
        if (group > 0) {
            binding.toolbar.subtitle = "正在加载群资料…"
        }

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.toolbar.inflateMenu(R.menu.menu_chat_toolbar)
        binding.toolbar.menu.findItem(R.id.action_group_info).isVisible = group > 0
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_chat_search -> {
                    showSearchInChatDialog()
                    true
                }
                R.id.action_chat_clear_local -> {
                    confirmClearLocalHistory()
                    true
                }
                R.id.action_group_info -> {
                    showGroupInfoDialog()
                    true
                }
                else -> false
            }
        }

        val layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.layoutManager = layoutManager

        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateScrollFabVisibility()
                if (dy >= 0) return
                val first = layoutManager.findFirstVisibleItemPosition()
                if (first <= LOAD_MORE_THRESHOLD) {
                    viewModel.loadOlderMessages()
                }
            }
        })

        binding.fabScrollBottom.setOnClickListener {
            val c = adapter?.itemCount ?: 0
            if (c > 0) {
                binding.recycler.smoothScrollToPosition(c - 1)
            }
        }

        sessionViewModel.session.observe(viewLifecycleOwner) { session ->
            if (session == null) return@observe
            if (adapter == null) {
                val apiBase = (requireActivity().application as BootstrapApplication).services.apiBaseUrl
                adapter = ChatMessageAdapter(
                    session.userId,
                    apiBaseUrl = apiBase,
                    onVisualMediaOpen = { msgId -> openChatVisualMediaBrowser(msgId, apiBase) },
                    onPlayVoice = { viewModel.playVoiceFromUrl(it) },
                    onBubbleLongPress = { msg -> showMessageActionsDialog(msg, apiBase) },
                    onReplyRefClick = { msgId ->
                        val items = lastMessages.toChatListItems(lastDisplayNames)
                        val idx = items.indexOfFirst {
                            it is ChatListItem.MessageRow && it.message.msgId == msgId
                        }
                        if (idx >= 0) {
                            binding.recycler.post {
                                binding.recycler.smoothScrollToPosition(idx)
                                viewModel.flashHighlightMessage(msgId)
                            }
                        } else {
                            Toast.makeText(requireContext(), "未找到该条消息", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onVoiceSpeedCycle = { viewModel.cycleVoicePlaybackSpeed() }
                )
                binding.recycler.adapter = adapter
                adapter?.setOnlineByUserId(viewModel.userOnline.value.orEmpty())
                adapter?.setVoiceSpeedForLabel(viewModel.voicePlaybackSpeed.value ?: 1f)
                adapter?.updateHighlightMessageId(viewModel.highlightMessageId.value)
                if (swipeReplyTouch == null) {
                    swipeReplyTouch = ItemTouchHelper(
                        ChatSwipeReplyItemTouchHelper(adapter!!) { viewModel.setReplyDraft(it) }
                    ).also { it.attachToRecyclerView(binding.recycler) }
                }
            } else {
                adapter?.updateSelfUserId(session.userId)
            }
            if (!didBindChat) {
                viewModel.bind(session, peer, group, titleFb)
                didBindChat = true
            }
            if (!draftRestored) {
                draftRestored = true
                val ck = convKeyForDraft(session.userId, peer, group)
                val draft = services().chatDraftStore.get(session.userId, ck)
                if (draft.isNotBlank()) {
                    binding.editMessage.setText(draft)
                    binding.editMessage.setSelection(draft.length)
                }
            }
        }

        viewModel.title.observe(viewLifecycleOwner) { t ->
            binding.toolbar.title = t
        }

        viewModel.displayNames.observe(viewLifecycleOwner) { map ->
            lastDisplayNames = map
            submitChatList(ChatScroll.NoScroll)
        }

        viewModel.userOnline.observe(viewLifecycleOwner) { map ->
            adapter?.setOnlineByUserId(map)
            subtitleOnlineMap = map
            refreshToolbarSubtitle()
        }

        viewModel.groupDetail.observe(viewLifecycleOwner) { detail ->
            subtitleGroupDetail = detail
            refreshToolbarSubtitle()
        }

        viewModel.playingVoiceUrl.observe(viewLifecycleOwner) { url ->
            adapter?.setPlayingVoiceUrl(url)
        }

        viewModel.highlightMessageId.observe(viewLifecycleOwner) { id ->
            adapter?.updateHighlightMessageId(id)
        }

        viewModel.voicePlaybackSpeed.observe(viewLifecycleOwner) { s ->
            adapter?.setVoiceSpeedForLabel(s ?: 1f)
        }

        viewModel.composerSending.observe(viewLifecycleOwner) { busy ->
            val on = busy == true
            binding.progressSend.isVisible = on
            binding.buttonSend.isInvisible = on
            binding.buttonSend.isEnabled = !on
        }

        viewModel.messagesState.observe(viewLifecycleOwner) { state ->
            lastMessages = state.messages
            submitChatList(state.scroll)
        }

        viewModel.replyDraft.observe(viewLifecycleOwner) { ref ->
            if (ref == null) {
                binding.replyPreviewBar.visibility = View.GONE
            } else {
                val names = viewModel.displayNames.value.orEmpty()
                val who = names[ref.fromUserId]?.takeIf { it.isNotBlank() } ?: "用户 ${ref.fromUserId}"
                val prev = chatMessagePreviewLabel(ref.body).take(80)
                binding.replyPreviewText.text = "回复 $who：$prev"
                binding.replyPreviewBar.visibility = View.VISIBLE
            }
        }
        binding.replyPreviewClose.setOnClickListener { viewModel.setReplyDraft(null) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                services().imClient.connectionState.collect { state ->
                    val b = binding.bannerConnection
                    val ctx = requireContext()
                    when (state) {
                        ImConnectionState.Connected -> b.visibility = View.GONE
                        ImConnectionState.Connecting -> {
                            b.visibility = View.VISIBLE
                            b.setBackgroundColor(ContextCompat.getColor(ctx, SharedR.color.wx_banner_connecting_bg))
                            b.setTextColor(ContextCompat.getColor(ctx, SharedR.color.wx_banner_connecting_text))
                            b.text = "正在连接…"
                        }
                        ImConnectionState.Disconnected -> {
                            b.visibility = View.VISIBLE
                            b.setBackgroundColor(ContextCompat.getColor(ctx, SharedR.color.wx_banner_offline_bg))
                            b.setTextColor(ContextCompat.getColor(ctx, SharedR.color.wx_banner_offline_text))
                            b.text = "未连接，将自动重试"
                        }
                    }
                }
            }
        }

        binding.buttonSend.setOnClickListener {
            val raw = binding.editMessage.text?.toString().orEmpty()
            if (raw.isBlank()) return@setOnClickListener
            binding.buttonSend.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            val session = sessionViewModel.session.value
            if (session != null) {
                services().chatDraftStore.put(session.userId, convKeyForDraft(session.userId, peerUserId, groupId), "")
            }
            viewModel.send(raw)
            binding.editMessage.text?.clear()
        }

        binding.buttonAttach.setOnClickListener { showAttachMenu() }
    }

    private fun openChatVisualMediaBrowser(anchorMsgId: Long, apiBaseUrl: String) {
        val pages = buildChatVisualMediaPages(lastMessages, apiBaseUrl)
        if (pages.isEmpty()) return
        val idx = pages.indexOfFirst { it.msgId == anchorMsgId }.takeIf { it >= 0 } ?: 0
        ChatMediaViewerActivity.start(requireContext(), pages, idx)
    }

    private fun refreshToolbarSubtitle() {
        when {
            peerUserId > 0 -> {
                binding.toolbar.subtitle = when (subtitleOnlineMap[peerUserId]) {
                    true -> "在线"
                    false -> "离线"
                    else -> null
                }
            }
            groupId > 0 -> {
                val d = subtitleGroupDetail?.takeIf { it.groupId == groupId }
                binding.toolbar.subtitle = when {
                    d != null -> "${d.members.size} 位成员"
                    else -> "正在加载群资料…"
                }
            }
            else -> binding.toolbar.subtitle = null
        }
    }

    private fun showAttachMenu() {
        val sheet = BottomSheetDialog(requireContext())
        val v: View = layoutInflater.inflate(R.layout.bottom_sheet_chat_attach, binding.root, false)
        fun dismissAnd(block: () -> Unit) {
            sheet.dismiss()
            block()
        }
        v.findViewById<View>(R.id.row_camera).setOnClickListener {
            dismissAnd { requestCameraAndTakePicture() }
        }
        v.findViewById<View>(R.id.row_gallery).setOnClickListener {
            dismissAnd { pickImage.launch("image/*") }
        }
        v.findViewById<View>(R.id.row_video).setOnClickListener {
            dismissAnd { pickVideo.launch("video/*") }
        }
        v.findViewById<View>(R.id.row_file).setOnClickListener {
            dismissAnd { pickFile.launch("*/*") }
        }
        v.findViewById<View>(R.id.row_voice).setOnClickListener {
            dismissAnd { requestAudioAndRecord() }
        }
        sheet.setContentView(v)
        sheet.show()
    }

    private fun requestCameraAndTakePicture() {
        val ctx = requireContext()
        when {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ->
                launchCameraInternal()
            else -> requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCameraInternal() {
        val ctx = requireContext()
        val dir = File(ctx.cacheDir, "im_camera").apply { mkdirs() }
        val f = File(dir, "cap_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
        cameraUri = uri
        takePicture.launch(uri)
    }

    private fun requestAudioAndRecord() {
        val ctx = requireContext()
        when {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ->
                startVoiceRecordDialog()
            else -> requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceRecordDialog() {
        val ctx = requireContext()
        val dir = File(ctx.cacheDir, "im_voice").apply { mkdirs() }
        val f = File(dir, "v_${System.currentTimeMillis()}.m4a")
        voiceFile = f
        try {
            voiceRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(f.absolutePath)
                prepare()
                start()
            }
            voiceStartMs = SystemClock.elapsedRealtime()
        } catch (_: Exception) {
            Toast.makeText(ctx, "无法开始录音", Toast.LENGTH_SHORT).show()
            voiceRecorder = null
            voiceFile = null
            return
        }
        MaterialAlertDialogBuilder(ctx)
            .setTitle("正在录音")
            .setMessage("发送后对方可点击播放")
            .setCancelable(false)
            .setNegativeButton("取消") { _, _ -> stopVoiceRecord(send = false) }
            .setPositiveButton("发送") { _, _ -> stopVoiceRecord(send = true) }
            .show()
    }

    private fun stopVoiceRecord(send: Boolean) {
        val rec = voiceRecorder
        val f = voiceFile
        voiceRecorder = null
        voiceFile = null
        if (rec != null) {
            try {
                rec.stop()
            } catch (_: Exception) {
            }
            try {
                rec.release()
            } catch (_: Exception) {
            }
        }
        if (send && f != null && f.exists()) {
            val dur = SystemClock.elapsedRealtime() - voiceStartMs
            viewModel.uploadVoiceFile(f, dur)
        } else {
            f?.delete()
        }
    }

    override fun onResume() {
        super.onResume()
        mainTabsViewModel.setOpenChat(peerUserId, groupId)
        if (peerUserId > 0) {
            mainTabsViewModel.clearUnreadP2P(peerUserId)
        }
        if (groupId > 0) {
            mainTabsViewModel.clearUnreadGroup(groupId)
        }
    }

    override fun onPause() {
        val session = sessionViewModel.session.value
        if (session != null) {
            services().chatDraftStore.put(
                session.userId,
                convKeyForDraft(session.userId, peerUserId, groupId),
                binding.editMessage.text?.toString().orEmpty()
            )
        }
        mainTabsViewModel.clearOpenChat()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        swipeReplyTouch?.attachToRecyclerView(null)
        swipeReplyTouch = null
        adapter = null
        draftRestored = false
        _binding = null
    }

    /** 键盘顶起布局后再滚一次，避免首次 inset 时 RecyclerView 高度未更新 */
    private fun scrollListToBottomAfterIme() {
        binding.recycler.post {
            val cnt = adapter?.itemCount ?: 0
            if (cnt > 0) {
                binding.recycler.scrollToPosition(cnt - 1)
            }
            binding.recycler.post {
                val c = adapter?.itemCount ?: 0
                if (c > 0) binding.recycler.scrollToPosition(c - 1)
            }
        }
    }

    private fun showGroupInfoDialog() {
        val detail = viewModel.groupDetail.value
        if (detail == null) {
            viewModel.requestGroupInfoRefresh()
            Toast.makeText(requireContext(), "正在加载群资料…", Toast.LENGTH_SHORT).show()
            return
        }
        val session = sessionViewModel.session.value ?: return
        val names = viewModel.displayNames.value.orEmpty()
        fun label(uid: Long): String = when (uid) {
            session.userId -> session.nickname?.takeIf { it.isNotBlank() }
                ?: session.username?.takeIf { it.isNotBlank() }
                ?: "我"
            else -> names[uid] ?: "用户 $uid"
        }
        fun roleZh(r: String) = when (r) {
            "OWNER" -> "群主"
            "ADMIN" -> "管理员"
            else -> "成员"
        }
        val body = detail.members.joinToString("\n") { m ->
            "${label(m.userId)} — ${roleZh(m.role)}"
        }
        val b = MaterialAlertDialogBuilder(requireContext())
            .setTitle(detail.name)
            .setMessage(body)
            .setPositiveButton("确定", null)
        if (detail.myRole == "OWNER" || detail.myRole == "ADMIN") {
            b.setNegativeButton("修改群名") { _, _ -> showRenameGroupDialog() }
        }
        if (detail.myRole == "OWNER") {
            b.setNeutralButton("管理员") { _, _ -> showAdminManageDialog(detail, ::label) }
        }
        b.show()
    }

    private fun showRenameGroupDialog() {
        val ctx = requireContext()
        val input = EditText(ctx).apply {
            setText(viewModel.groupDetail.value?.name.orEmpty())
        }
        val pad = (16 * resources.displayMetrics.density).toInt()
        input.setPadding(pad, pad / 2, pad, pad / 2)
        MaterialAlertDialogBuilder(ctx)
            .setTitle("修改群名")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                viewModel.renameGroupTo(input.text?.toString().orEmpty())
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAdminManageDialog(detail: GroupDetailUi, label: (Long) -> String) {
        data class Act(val text: String, val targetUserId: Long, val remove: Boolean)
        val acts = buildList {
            detail.members
                .filter { it.userId != detail.ownerUserId && it.role == "MEMBER" }
                .forEach { add(Act("设为管理员 · ${label(it.userId)}", it.userId, false)) }
            detail.members
                .filter { it.role == "ADMIN" }
                .forEach { add(Act("取消管理员 · ${label(it.userId)}", it.userId, true)) }
        }
        if (acts.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage("暂无可操作的成员")
                .setPositiveButton("确定", null)
                .show()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("管理员")
            .setItems(acts.map { it.text }.toTypedArray()) { _, which ->
                val a = acts[which]
                if (a.remove) viewModel.removeGroupAdminFor(a.targetUserId)
                else viewModel.setGroupAdminFor(a.targetUserId)
            }
            .show()
    }

    private fun submitChatList(scroll: ChatScroll) {
        val items = lastMessages.toChatListItems(lastDisplayNames)
        val oldScrollRange = binding.recycler.computeVerticalScrollRange()
        adapter?.submitList(items) {
            when (scroll) {
                ChatScroll.ToBottom -> {
                    if (items.isNotEmpty()) {
                        val last = items.lastIndex
                        binding.recycler.post {
                            binding.recycler.scrollToPosition(last)
                            binding.recycler.post { binding.recycler.scrollToPosition(last) }
                        }
                    }
                }
                ChatScroll.KeepScroll -> {
                    val newRange = binding.recycler.computeVerticalScrollRange()
                    binding.recycler.scrollBy(0, newRange - oldScrollRange)
                }
                ChatScroll.NoScroll -> Unit
            }
            binding.recycler.post { updateScrollFabVisibility() }
        }
    }

    private fun updateScrollFabVisibility() {
        val show = binding.recycler.canScrollVertically(1)
        binding.fabScrollBottom.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun convKeyForDraft(selfId: Long, peer: Long, group: Long): String =
        if (peer > 0) ChatConvKeys.p2p(selfId, peer) else ChatConvKeys.group(group)

    private fun copyToClipboard(label: String, text: String) {
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(requireContext(), "已复制", Toast.LENGTH_SHORT).show()
    }

    private fun showMessageActionsDialog(message: ChatMessage, apiBase: String) {
        val options = mutableListOf<Pair<String, () -> Unit>>()
        options.add("回复" to { viewModel.setReplyDraft(message) })
        when (val c = parseBubbleBody(message.body)) {
            is BubbleContent.PlainText ->
                options.add("复制文字" to { copyToClipboard("消息", c.text) })
            is BubbleContent.FileMsg -> {
                val u = resolveImAttachmentUrl(c.url, apiBase)
                options.add("复制文件链接" to { copyToClipboard("链接", u) })
            }
            is BubbleContent.ImageMsg -> {
                val u = resolveImAttachmentUrl(c.url, apiBase)
                options.add("复制图片链接" to { copyToClipboard("链接", u) })
            }
            is BubbleContent.VideoMsg -> {
                val u = resolveImAttachmentUrl(c.url, apiBase)
                options.add("复制视频链接" to { copyToClipboard("链接", u) })
            }
            is BubbleContent.VoiceMsg -> {
                val u = resolveImAttachmentUrl(c.url, apiBase)
                options.add("复制语音链接" to { copyToClipboard("链接", u) })
            }
        }
        if (options.isEmpty()) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("消息操作")
            .setMessage("左滑自己消息 / 右滑对方消息可快速回复；语音条长按切换倍速。")
            .setItems(options.map { it.first }.toTypedArray()) { _, which -> options[which].second() }
            .show()
    }

    private fun computeChatSearchHits(query: String): List<Int> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val ql = q.lowercase()
        val items = lastMessages.toChatListItems(lastDisplayNames)
        return items.mapIndexedNotNull { idx, item ->
            if (item !is ChatListItem.MessageRow) return@mapIndexedNotNull null
            val hay = chatMessagePreviewLabel(item.message.body).lowercase()
            if (hay.contains(ql)) idx else null
        }
    }

    private fun scrollToCurrentSearchHit() {
        if (chatSearchHits.isEmpty()) return
        val pos = chatSearchHits[chatSearchHitIdx % chatSearchHits.size]
        val items = lastMessages.toChatListItems(lastDisplayNames)
        val row = items.getOrNull(pos) as? ChatListItem.MessageRow
        binding.recycler.post {
            binding.recycler.smoothScrollToPosition(pos)
            row?.let { viewModel.flashHighlightMessage(it.message.msgId) }
        }
    }

    private fun confirmClearLocalHistory() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("清空本地聊天记录")
            .setMessage("将删除本机已缓存的该会话消息，并重新从服务器拉取。不影响对方设备。")
            .setNegativeButton("取消", null)
            .setPositiveButton("清空") { _, _ ->
                viewModel.clearLocalHistory()
                Toast.makeText(requireContext(), "已清空本地记录，正在同步…", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showSearchInChatDialog() {
        val ctx = requireContext()
        val view = layoutInflater.inflate(R.layout.dialog_chat_search, binding.root, false)
        val edit = view.findViewById<EditText>(R.id.edit_query)
        val dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle("查找聊天内容")
            .setView(view)
            .setNegativeButton("关闭", null)
            .create()
        view.findViewById<View>(R.id.button_find).setOnClickListener {
            chatSearchHits = computeChatSearchHits(edit.text?.toString().orEmpty())
            chatSearchHitIdx = 0
            if (chatSearchHits.isEmpty()) {
                Toast.makeText(ctx, "未找到相关内容", Toast.LENGTH_SHORT).show()
            } else {
                scrollToCurrentSearchHit()
                Toast.makeText(ctx, "第 1/${chatSearchHits.size} 处", Toast.LENGTH_SHORT).show()
            }
        }
        view.findViewById<View>(R.id.button_next_hit).setOnClickListener {
            if (chatSearchHits.isEmpty()) {
                Toast.makeText(ctx, "请先点击「查找」", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            chatSearchHitIdx = (chatSearchHitIdx + 1) % chatSearchHits.size
            scrollToCurrentSearchHit()
            Toast.makeText(ctx, "第 ${chatSearchHitIdx + 1}/${chatSearchHits.size} 处", Toast.LENGTH_SHORT).show()
        }
        dialog.show()
    }

    companion object {
        private const val LOAD_MORE_THRESHOLD = 2
    }
}
