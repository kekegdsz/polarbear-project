package com.undersky.androidim.feature.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
