package com.undersky.androidim.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.undersky.androidim.ImApp
import com.undersky.androidim.R
import com.undersky.androidim.databinding.FragmentMeBinding
import com.undersky.androidim.ui.session.SessionViewModel
import kotlinx.coroutines.launch

class MeFragment : Fragment() {

    private var _binding: FragmentMeBinding? = null
    private val binding get() = _binding!!

    private val sessionViewModel: SessionViewModel by activityViewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionViewModel.session.observe(viewLifecycleOwner) { session ->
            if (session == null) return@observe
            binding.textUsername.text = session.username ?: "用户"
            binding.textUserId.text = "ID：${session.userId}"
        }

        binding.buttonLogout.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val app = requireActivity().application as ImApp
                app.sessionStore.clear()
                app.imSocket.disconnect(clearUser = true)
                val nav = findNavController()
                val opts = NavOptions.Builder()
                    .setPopUpTo(R.id.mainFragment, true)
                    .build()
                nav.navigate(R.id.loginFragment, null, opts)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
