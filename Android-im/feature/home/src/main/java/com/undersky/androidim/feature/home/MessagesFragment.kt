package com.undersky.androidim.feature.home

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
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
                        "GROUP" -> item.groupId?.let { gid ->
                            val title = item.groupName?.takeIf { n -> n.isNotBlank() } ?: "群聊 $gid"
                            navigator?.openChatGroup(gid, title)
                        }
                    }
                }
                binding.recycler.adapter = adapter
            } else {
                adapter?.updateSession(session)
            }
            tabsViewModel.conversations.value?.let { adapter?.submitList(it) }
            tabsViewModel.userPresence.value?.let { adapter?.updateImPresence(it) }
        }

        tabsViewModel.userPresence.observe(viewLifecycleOwner) { map ->
            adapter?.updateImPresence(map.orEmpty())
        }

        tabsViewModel.conversations.observe(viewLifecycleOwner) { list ->
            adapter?.submitList(list)
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

    override fun onDestroyView() {
        super.onDestroyView()
        composePopup?.dismiss()
        composePopup = null
        directoryCollectJob?.cancel()
        directoryCollectJob = null
        adapter = null
        _binding = null
    }
}
