package com.undersky.androidim.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationBarView
import com.undersky.androidim.MainActivity
import com.undersky.androidim.R
import com.undersky.androidim.databinding.FragmentMainBinding
import com.undersky.androidim.ImApp
import com.undersky.core.common.applyNavigationBarBottomInset
import com.undersky.core.common.applyStatusBarTopInset
import com.undersky.androidim.ui.session.SessionViewModel

class MainFragment : Fragment(), MainChatNavigator {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val tabsViewModel: MainTabsViewModel by activityViewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    private val sessionViewModel: SessionViewModel by activityViewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).requestPostNotificationsIfNeeded()
        binding.viewPager.applyStatusBarTopInset()
        binding.bottomNav.applyNavigationBarBottomInset()
        binding.viewPager.adapter = MainPagerAdapter(this)
        binding.viewPager.isUserInputEnabled = false
        binding.bottomNav.setOnItemSelectedListener(NavigationBarView.OnItemSelectedListener { item ->
            val page = when (item.itemId) {
                R.id.nav_messages -> 0
                R.id.nav_contacts -> 1
                else -> 2
            }
            binding.viewPager.setCurrentItem(page, false)
            true
        })
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val menuId = when (position) {
                    0 -> R.id.nav_messages
                    1 -> R.id.nav_contacts
                    else -> R.id.nav_me
                }
                if (binding.bottomNav.selectedItemId != menuId) {
                    binding.bottomNav.selectedItemId = menuId
                }
            }
        })
        sessionViewModel.session.observe(viewLifecycleOwner) { s ->
            if (s != null) {
                tabsViewModel.refreshConversations()
            }
        }

        tryConsumePendingChatNavigation()

        tabsViewModel.totalUnread.observe(viewLifecycleOwner) { count ->
            val badge = binding.bottomNav.getOrCreateBadge(R.id.nav_messages)
            if (count <= 0) {
                badge.isVisible = false
            } else {
                badge.isVisible = true
                badge.number = count.coerceAtMost(99)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tryConsumePendingChatNavigation()
        sessionViewModel.session.value?.let { tabsViewModel.refreshConversations() }
    }

    private fun tryConsumePendingChatNavigation() {
        val app = requireActivity().application as ImApp
        val pending = app.pendingChatNavigation ?: return
        app.pendingChatNavigation = null
        findNavController().navigate(
            R.id.action_main_to_chat,
            bundleOf(
                "peerUserId" to pending.peerUserId,
                "groupId" to pending.groupId,
                "titleFallback" to pending.titleFallback
            )
        )
    }

    override fun openChatP2P(peerUserId: Long) {
        findNavController().navigate(
            R.id.action_main_to_chat,
            bundleOf(
                "peerUserId" to peerUserId,
                "groupId" to -1L,
                "titleFallback" to "用户 $peerUserId"
            )
        )
    }

    override fun openChatGroup(groupId: Long) {
        findNavController().navigate(
            R.id.action_main_to_chat,
            bundleOf(
                "peerUserId" to -1L,
                "groupId" to groupId,
                "titleFallback" to "群聊 $groupId"
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
