package com.undersky.androidim.feature.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.undersky.androidim.bootstrap.BootstrapApplication
import com.undersky.androidim.bootstrap.ImHostActivity
import com.undersky.androidim.feature.splash.databinding.FragmentSplashBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            val services = (requireActivity().application as BootstrapApplication).services
            val session = services.sessionStore.sessionFlow.first()
            (requireActivity() as ImHostActivity).endSplashHold()
            val host = requireActivity() as ImHostActivity
            if (session != null) {
                host.navigateSplashToMain()
            } else {
                host.navigateSplashToLogin()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
