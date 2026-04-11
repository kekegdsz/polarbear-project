package com.undersky.androidim.feature.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.undersky.androidim.bootstrap.session.SessionViewModel
import com.undersky.androidim.feature.chat.adapters.ChatMessageAdapter
import com.undersky.androidim.feature.chat.toChatListItems
import com.undersky.androidim.feature.chat.databinding.FragmentChatBinding
import com.undersky.androidim.feature.home.MainTabsViewModel
import com.undersky.core.common.applyWindowInsetsPadding
import com.undersky.im.core.api.ChatMessage

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

    private var adapter: ChatMessageAdapter? = null
    private var didBindChat = false
    private var peerUserId: Long = -1L
    private var groupId: Long = -1L
    private var lastMessages: List<ChatMessage> = emptyList()
    private var lastDisplayNames: Map<Long, String> = emptyMap()

    /** 上一帧 IME 底边 inset，用于检测键盘从关闭到打开 */
    private var lastImeBottomPx: Int = 0

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

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        if (group > 0) {
            binding.toolbar.inflateMenu(R.menu.menu_chat_group)
            binding.toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_group_info -> {
                        showGroupInfoDialog()
                        true
                    }
                    else -> false
                }
            }
        }

        val layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.layoutManager = layoutManager

        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy >= 0) return
                val first = layoutManager.findFirstVisibleItemPosition()
                if (first <= LOAD_MORE_THRESHOLD) {
                    viewModel.loadOlderMessages()
                }
            }
        })

        sessionViewModel.session.observe(viewLifecycleOwner) { session ->
            if (session == null) return@observe
            if (adapter == null) {
                adapter = ChatMessageAdapter(session.userId)
                binding.recycler.adapter = adapter
                adapter?.setOnlineByUserId(viewModel.userOnline.value.orEmpty())
            } else {
                adapter?.updateSelfUserId(session.userId)
            }
            if (!didBindChat) {
                viewModel.bind(session, peer, group, titleFb)
                didBindChat = true
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
        }

        viewModel.messagesState.observe(viewLifecycleOwner) { state ->
            lastMessages = state.messages
            submitChatList(state.scroll)
        }

        binding.buttonSend.setOnClickListener {
            viewModel.send(binding.editMessage.text?.toString().orEmpty())
            binding.editMessage.text?.clear()
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
        mainTabsViewModel.clearOpenChat()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
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
                        binding.recycler.scrollToPosition(items.lastIndex)
                    }
                }
                ChatScroll.KeepScroll -> {
                    val newRange = binding.recycler.computeVerticalScrollRange()
                    binding.recycler.scrollBy(0, newRange - oldScrollRange)
                }
                ChatScroll.NoScroll -> Unit
            }
        }
    }

    companion object {
        private const val LOAD_MORE_THRESHOLD = 2
    }
}
