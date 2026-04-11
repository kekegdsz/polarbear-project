package com.undersky.androidim.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.undersky.androidim.R
import com.undersky.androidim.databinding.FragmentLoginBinding
import com.undersky.core.common.applyWindowInsetsPadding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyWindowInsetsPadding(padTop = true, padBottom = true)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { ev ->
                    when (ev) {
                        is LoginViewModel.LoginEvent.NavigateMain -> {
                            binding.progress.visibility = View.GONE
                            findNavController().navigate(R.id.action_login_to_main)
                        }
                        is LoginViewModel.LoginEvent.ShowError -> {
                            binding.progress.visibility = View.GONE
                            binding.textError.visibility = View.VISIBLE
                            binding.textError.text = ev.message
                        }
                    }
                }
            }
        }

        binding.buttonLogin.setOnClickListener {
            binding.textError.visibility = View.GONE
            binding.progress.visibility = View.VISIBLE
            viewModel.login(
                binding.editUsername.text?.toString().orEmpty(),
                binding.editPassword.text?.toString().orEmpty()
            )
        }

        binding.buttonRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
