package com.undersky.androidim.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.undersky.androidim.bootstrap.BootstrapApplication
import com.undersky.androidim.bootstrap.ImHostActivity
import com.undersky.androidim.bootstrap.session.SessionViewModel
import com.undersky.androidim.feature.home.databinding.FragmentMeBinding
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
                val services = (requireActivity().application as BootstrapApplication).services
                services.sessionStore.clear()
                (requireActivity() as ImHostActivity).navigateLogoutToLogin()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
