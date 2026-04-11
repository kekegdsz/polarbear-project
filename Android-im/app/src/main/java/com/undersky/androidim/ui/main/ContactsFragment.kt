package com.undersky.androidim.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.undersky.androidim.R
import com.undersky.androidim.databinding.FragmentContactsBinding
import com.undersky.androidim.ui.adapters.ContactAdapter
import com.undersky.androidim.ui.session.SessionViewModel

class ContactsFragment : Fragment() {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!

    private val contactsViewModel: ContactsViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    private val sessionViewModel: SessionViewModel by activityViewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    private var adapter: ContactAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                setDrawable(requireNotNull(ContextCompat.getDrawable(requireContext(), R.drawable.list_divider)))
            }
        )

        val navigator = parentFragment as? MainChatNavigator
        adapter = ContactAdapter { u -> navigator?.openChatP2P(u.id) }
        binding.recycler.adapter = adapter

        contactsViewModel.users.observe(viewLifecycleOwner) { users ->
            adapter?.submitList(users)
            updateStates(users, contactsViewModel.loading.value == true, contactsViewModel.error.value)
        }

        contactsViewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progress.visibility = if (loading && adapter?.currentList.isNullOrEmpty()) View.VISIBLE else View.GONE
            updateStates(contactsViewModel.users.value.orEmpty(), loading, contactsViewModel.error.value)
        }

        contactsViewModel.error.observe(viewLifecycleOwner) { err ->
            updateStates(contactsViewModel.users.value.orEmpty(), contactsViewModel.loading.value == true, err)
        }

        binding.buttonRefresh.setOnClickListener {
            sessionViewModel.session.value?.let { contactsViewModel.refresh(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        sessionViewModel.session.value?.let { contactsViewModel.start(it) }
    }

    private fun updateStates(users: List<*>, loading: Boolean, error: String?) {
        val empty = users.isEmpty()
        when {
            loading && empty -> {
                binding.recycler.visibility = View.GONE
                binding.emptyState.visibility = View.GONE
            }
            error != null && empty -> {
                binding.recycler.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
                binding.emptyTitle.text = error
                binding.emptySub.text = "点击右上角刷新重试"
            }
            empty && !loading && error == null -> {
                binding.recycler.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
                binding.emptyTitle.text = "暂无其他用户"
                binding.emptySub.text = "服务器上只有您一个账号时，列表会为空（已自动隐藏自己）"
            }
            else -> {
                binding.recycler.visibility = View.VISIBLE
                binding.emptyState.visibility = View.GONE
            }
        }
    }

    override fun onPause() {
        super.onPause()
        contactsViewModel.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        _binding = null
    }
}
