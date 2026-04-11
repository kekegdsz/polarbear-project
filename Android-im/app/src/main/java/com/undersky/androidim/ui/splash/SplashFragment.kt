package com.undersky.androidim.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.undersky.androidim.ImApp
import com.undersky.androidim.MainActivity
import com.undersky.androidim.R
import com.undersky.androidim.databinding.FragmentSplashBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

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
            val app = requireActivity().application as ImApp
            val session = app.sessionStore.sessionFlow.first()
            (requireActivity() as MainActivity).endSplashHold()
            val nav = findNavController()
            val opts = NavOptions.Builder().setPopUpTo(R.id.splashFragment, true).build()
            if (session != null) {
                nav.navigate(R.id.mainFragment, null, opts)
            } else {
                nav.navigate(R.id.loginFragment, null, opts)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
