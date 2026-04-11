package com.undersky.androidim.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationBarView
import com.undersky.androidim.bootstrap.BootstrapApplication
import com.undersky.androidim.bootstrap.ImHostActivity
import com.undersky.androidim.bootstrap.session.SessionViewModel
import com.undersky.androidim.feature.home.databinding.FragmentMainBinding
import com.undersky.core.common.applyNavigationBarBottomInset
import com.undersky.core.common.applyStatusBarTopInset

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
        (requireActivity() as ImHostActivity).requestPostNotificationsIfNeeded()
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
        val services = (requireActivity().application as BootstrapApplication).services
        val pending = services.pendingChatNavigation ?: return
        services.pendingChatNavigation = null
        (requireActivity() as ImHostActivity).navigateMainToChat(
            pending.peerUserId,
            pending.groupId,
            pending.titleFallback
        )
    }

    override fun openChatP2P(peerUserId: Long) {
        (requireActivity() as ImHostActivity).navigateMainToChat(peerUserId, -1L, "用户 $peerUserId")
    }

    override fun openChatGroup(groupId: Long) {
        (requireActivity() as ImHostActivity).navigateMainToChat(-1L, groupId, "群聊 $groupId")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
