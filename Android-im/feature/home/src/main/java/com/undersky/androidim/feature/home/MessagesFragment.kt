package com.undersky.androidim.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.undersky.androidim.bootstrap.BootstrapApplication
import com.undersky.androidim.bootstrap.session.SessionViewModel
import com.undersky.androidim.feature.home.adapters.ConversationAdapter
import com.undersky.androidim.feature.home.databinding.FragmentMessagesBinding
import com.undersky.androidim.shared.ui.R as UiR
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MessagesFragment : Fragment() {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    private var directoryCollectJob: Job? = null

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
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        val divider = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
            setDrawable(requireNotNull(ContextCompat.getDrawable(requireContext(), UiR.drawable.list_divider)))
        }
        binding.recycler.addItemDecoration(divider)

        val navigator = parentFragment as? MainChatNavigator
        val services = (requireActivity().application as BootstrapApplication).services

        sessionViewModel.session.observe(viewLifecycleOwner) { session ->
            if (session == null) return@observe
            directoryCollectJob?.cancel()
            directoryCollectJob = viewLifecycleOwner.lifecycleScope.launch {
                services.userDirectoryCacheStore.directoryFlow(session.userId).collect { users ->
                    adapter?.updateDirectory(users)
                }
            }
            if (adapter == null) {
                adapter = ConversationAdapter(session) { item ->
                    when (item.convType) {
                        "P2P" -> item.peerUserId?.let { navigator?.openChatP2P(it, "用户 $it") }
                        "GROUP" -> item.groupId?.let { navigator?.openChatGroup(it) }
                    }
                }
                binding.recycler.adapter = adapter
            } else {
                adapter?.updateSession(session)
            }
            tabsViewModel.conversations.value?.let { adapter?.submitList(it) }
        }

        tabsViewModel.conversations.observe(viewLifecycleOwner) { list ->
            adapter?.submitList(list)
        }

        binding.buttonRefresh.setOnClickListener {
            tabsViewModel.refreshConversations()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        directoryCollectJob?.cancel()
        directoryCollectJob = null
        adapter = null
        _binding = null
    }
}
