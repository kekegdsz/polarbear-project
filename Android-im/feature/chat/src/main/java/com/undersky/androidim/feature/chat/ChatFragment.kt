package com.undersky.androidim.feature.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyWindowInsetsPadding(padTop = true, padBottom = true)
        val peer = arguments?.getLong("peerUserId") ?: -1L
        val group = arguments?.getLong("groupId") ?: -1L
        peerUserId = peer
        groupId = group
        val titleFb = arguments?.getString("titleFallback").orEmpty()

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())

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
            submitChatList()
        }

        viewModel.messages.observe(viewLifecycleOwner) { list ->
            lastMessages = list
            submitChatList()
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

    private fun submitChatList() {
        val items = lastMessages.toChatListItems(lastDisplayNames)
        adapter?.submitList(items) {
            if (items.isNotEmpty()) {
                binding.recycler.scrollToPosition(items.lastIndex)
            }
        }
    }
}
