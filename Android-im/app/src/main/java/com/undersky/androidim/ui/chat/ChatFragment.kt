package com.undersky.androidim.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.undersky.androidim.databinding.FragmentChatBinding
import com.undersky.androidim.ui.adapters.ChatMessageAdapter
import com.undersky.androidim.ui.applyWindowInsetsPadding
import com.undersky.androidim.ui.session.SessionViewModel

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    private val sessionViewModel: SessionViewModel by activityViewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    private var adapter: ChatMessageAdapter? = null
    private var didBindChat = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyWindowInsetsPadding(padTop = true, padBottom = true)
        val peer = arguments?.getLong("peerUserId") ?: -1L
        val group = arguments?.getLong("groupId") ?: -1L
        val titleFb = arguments?.getString("titleFallback").orEmpty()

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.recycler.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }

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

        viewModel.messages.observe(viewLifecycleOwner) { list ->
            adapter?.submitList(list) {
                if (list.isNotEmpty()) {
                    binding.recycler.scrollToPosition(list.lastIndex)
                }
            }
        }

        binding.buttonSend.setOnClickListener {
            viewModel.send(binding.editMessage.text?.toString().orEmpty())
            binding.editMessage.text?.clear()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        _binding = null
    }
}
