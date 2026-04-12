package com.undersky.androidim.feature.home

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.undersky.androidim.bootstrap.AppServices
import com.undersky.androidim.bootstrap.BootstrapApplication
import com.undersky.androidim.bootstrap.session.SessionViewModel
import com.undersky.androidim.feature.home.adapters.ConversationAdapter
import com.undersky.androidim.feature.home.databinding.FragmentMessagesBinding
import com.undersky.im.core.api.ConversationItem
import com.undersky.im.core.api.ImConnectionState
import com.undersky.im.core.local.ChatConvKeys
import com.undersky.androidim.shared.ui.R as UiR
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MessagesFragment : Fragment() {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    private val stopSwipeRefreshRunnable = Runnable {
        if (_binding != null) binding.swipeRefresh.isRefreshing = false
    }

    private var directoryCollectJob: Job? = null
    private var composePopup: PopupWindow? = null

    private val tabsViewModel: MainTabsViewModel by activityViewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    private val sessionViewModel: SessionViewModel by activityViewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    private var adapter: ConversationAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.swipeRefresh.setColorSchemeResources(UiR.color.wx_green)
        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(android.R.color.white)
        binding.swipeRefresh.setOnRefreshListener {
            tabsViewModel.refreshConversations()
            binding.root.removeCallbacks(stopSwipeRefreshRunnable)
            binding.root.postDelayed(stopSwipeRefreshRunnable, 2500)
        }

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        val divider = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
            setDrawable(requireNotNull(ContextCompat.getDrawable(requireContext(), UiR.drawable.list_divider)))
        }
        binding.recycler.addItemDecoration(divider)

        binding.buttonEmptyRefresh.setOnClickListener {
            binding.swipeRefresh.isRefreshing = true
            tabsViewModel.refreshConversations()
            binding.root.removeCallbacks(stopSwipeRefreshRunnable)
            binding.root.postDelayed(stopSwipeRefreshRunnable, 2500)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val svc = (requireActivity().application as BootstrapApplication).services
                svc.imClient.connectionState.collect { state ->
                    if (_binding == null) return@collect
                    val b = binding.bannerConnection
                    val ctx = requireContext()
                    when (state) {
                        ImConnectionState.Connected -> b.visibility = View.GONE
                        ImConnectionState.Connecting -> {
                            b.visibility = View.VISIBLE
                            b.setBackgroundColor(ContextCompat.getColor(ctx, UiR.color.wx_banner_connecting_bg))
                            b.setTextColor(ContextCompat.getColor(ctx, UiR.color.wx_banner_connecting_text))
                            b.text = "正在连接…"
                        }
                        ImConnectionState.Disconnected -> {
                            b.visibility = View.VISIBLE
                            b.setBackgroundColor(ContextCompat.getColor(ctx, UiR.color.wx_banner_offline_bg))
                            b.setTextColor(ContextCompat.getColor(ctx, UiR.color.wx_banner_offline_text))
                            b.text = "未连接，将自动重试"
                        }
                    }
                }
            }
        }

        val navigator = parentFragment as? MainChatNavigator
        val services = (requireActivity().application as BootstrapApplication).services

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_mark_all_read -> {
                    tabsViewModel.markAllConversationsRead()
                    Toast.makeText(requireContext(), "已全部标为已读", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_manage_hidden -> {
                    showHiddenConversationsDialog()
                    true
                }
                else -> false
            }
        }

        sessionViewModel.session.observe(viewLifecycleOwner) { session ->
            if (session == null) return@observe
            directoryCollectJob?.cancel()
            directoryCollectJob = viewLifecycleOwner.lifecycleScope.launch {
                services.userDirectoryCacheStore.directoryFlow(session.userId).collect { users ->
                    adapter?.updateDirectory(users)
                }
            }
            if (adapter == null) {
                adapter = ConversationAdapter(
                    session,
                    onOpen = { item ->
                        when (item.convType) {
                            "P2P" -> item.peerUserId?.let { navigator?.openChatP2P(it, "用户 $it") }
                            "GROUP" -> item.groupId?.let { gid ->
                                val title = item.groupName?.takeIf { n -> n.isNotBlank() } ?: "群聊 $gid"
                                navigator?.openChatGroup(gid, title)
                            }
                        }
                    },
                    onLongPress = { item -> showConversationActions(item, services) }
                )
                binding.recycler.adapter = adapter
            } else {
                adapter?.updateSession(session)
            }
            val conv = tabsViewModel.conversations.value.orEmpty()
            adapter?.submitList(conv)
            updateEmptyState(conv.isEmpty())
            tabsViewModel.userPresence.value?.let { adapter?.updateImPresence(it) }
            refreshDraftAndPinOverlay(session.userId, services)
        }

        tabsViewModel.userPresence.observe(viewLifecycleOwner) { map ->
            adapter?.updateImPresence(map.orEmpty())
        }

        tabsViewModel.conversations.observe(viewLifecycleOwner) { list ->
            binding.root.removeCallbacks(stopSwipeRefreshRunnable)
            binding.swipeRefresh.isRefreshing = false
            adapter?.submitList(list)
            updateEmptyState(list.isEmpty())
            val uid = sessionViewModel.session.value?.userId
            if (uid != null) {
                refreshDraftAndPinOverlay(uid, services)
            }
        }

        binding.buttonCompose.setOnClickListener { anchor ->
            val popupView = layoutInflater.inflate(R.layout.popup_messages_compose, binding.root, false)
            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            ).apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                elevation = 8f
                isOutsideTouchable = true
                isFocusable = true
            }
            composePopup = popupWindow
            popupWindow.setOnDismissListener { composePopup = null }

            popupView.findViewById<View>(R.id.action_create_group).setOnClickListener {
                popupWindow.dismiss()
                val session = sessionViewModel.session.value ?: return@setOnClickListener
                val app = requireActivity().application as BootstrapApplication
                lifecycleScope.launch {
                    val users = loadDirectoryUsersForPicker(app, session)
                    showCreateGroupMemberPicker(session, users)
                }
            }
            popupView.findViewById<View>(R.id.action_refresh_conversations).setOnClickListener {
                popupWindow.dismiss()
                tabsViewModel.refreshConversations()
            }
            anchor.post {
                popupView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                val popupW = popupView.measuredWidth
                popupWindow.showAsDropDown(anchor, anchor.width - popupW, 8)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val uid = sessionViewModel.session.value?.userId ?: return
        val app = requireActivity().application as BootstrapApplication
        refreshDraftAndPinOverlay(uid, app.services)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showHiddenConversationsDialog() {
        val rows = tabsViewModel.listHiddenConversationsForManage()
        if (rows.isEmpty()) {
            Toast.makeText(requireContext(), "暂无隐藏的会话", Toast.LENGTH_SHORT).show()
            return
        }
        val services = (requireActivity().application as BootstrapApplication).services
        val uid = sessionViewModel.session.value?.userId ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("已隐藏的会话")
            .setMessage("点选一项即可恢复在消息列表中显示。")
            .setItems(rows.map { it.title }.toTypedArray()) { _, which ->
                tabsViewModel.unhideConversationLocally(rows[which].convKey)
                refreshDraftAndPinOverlay(uid, services)
                Toast.makeText(requireContext(), "已恢复：${rows[which].title}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun convKeyForConversation(selfId: Long, item: ConversationItem): String? = when (item.convType) {
        "P2P" -> item.peerUserId?.let { p -> ChatConvKeys.p2p(selfId, p) }
        "GROUP" -> item.groupId?.let { g -> ChatConvKeys.group(g) }
        else -> null
    }

    private fun refreshDraftAndPinOverlay(ownerUserId: Long, services: AppServices) {
        val adapter = this.adapter ?: return
        val pins = services.pinnedConversationStore.getOrderedPins(ownerUserId).toSet()
        val drafts = mutableMapOf<String, String>()
        for (item in tabsViewModel.conversations.value.orEmpty()) {
            val k = convKeyForConversation(ownerUserId, item) ?: continue
            val d = services.chatDraftStore.get(ownerUserId, k).trim()
            if (d.isNotEmpty()) drafts[k] = d
        }
        adapter.setPinnedConvKeys(pins)
        adapter.setDraftByConvKey(drafts)
    }

    private fun showConversationActions(
        item: ConversationItem,
        services: AppServices
    ) {
        val session = sessionViewModel.session.value ?: return
        val uid = session.userId
        val key = convKeyForConversation(uid, item) ?: return
        val pinned = services.pinnedConversationStore.isPinned(uid, key)
        val hasDraft = services.chatDraftStore.get(uid, key).trim().isNotEmpty()
        val labels = buildList {
            add(if (pinned) "取消置顶" else "置顶聊天")
            if (hasDraft) add("清除草稿")
            add("从列表隐藏（仅本机）")
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("会话操作")
            .setItems(labels.toTypedArray()) { _, which ->
                when (labels[which]) {
                    "置顶聊天", "取消置顶" -> tabsViewModel.togglePinForConversation(key)
                    "清除草稿" -> {
                        services.chatDraftStore.put(uid, key, "")
                        refreshDraftAndPinOverlay(uid, services)
                    }
                    "从列表隐藏（仅本机）" -> {
                        tabsViewModel.hideConversationLocally(key)
                        Snackbar.make(binding.root, "已从消息列表隐藏", Snackbar.LENGTH_LONG)
                            .setAction("撤销") {
                                tabsViewModel.unhideConversationLocally(key)
                                refreshDraftAndPinOverlay(uid, services)
                            }
                            .show()
                    }
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.root.removeCallbacks(stopSwipeRefreshRunnable)
        composePopup?.dismiss()
        composePopup = null
        directoryCollectJob?.cancel()
        directoryCollectJob = null
        adapter = null
        _binding = null
    }
}
