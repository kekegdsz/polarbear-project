package com.undersky.androidim.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
            val nick = session.nickname?.takeIf { it.isNotBlank() }
                ?: session.username?.takeIf { it.isNotBlank() }
                ?: "用户"
            binding.textUsername.text = nick
            binding.avatarLetter.text =
                nick.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            val sub = buildString {
                session.username?.takeIf { it.isNotBlank() }?.let { append("登录账号：$it\n") }
                append("ID：${session.userId}")
            }
            binding.textUserId.text = sub
        }

        binding.buttonEditProfile.setOnClickListener {
            val session = sessionViewModel.session.value ?: return@setOnClickListener
            val services = (requireActivity().application as BootstrapApplication).services
            val density = resources.displayMetrics.density
            val padH = (20 * density).toInt()
            val padV = (12 * density).toInt()
            val edit = EditText(requireContext()).apply {
                setPadding(padH, padV, padH, padV)
                hint = "1～32 个字符"
                setText(
                    session.nickname?.takeIf { it.isNotBlank() }
                        ?: session.username.orEmpty()
                )
            }
            val wrap = FrameLayout(requireContext()).apply {
                setPadding(padH, padV, padH, 0)
                addView(
                    edit,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                )
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("修改昵称")
                .setView(wrap)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("保存") { _, _ ->
                    val raw = edit.text?.toString()?.trim().orEmpty()
                    if (raw.isEmpty()) {
                        Toast.makeText(requireContext(), "昵称不能为空", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    if (raw.length > 32) {
                        Toast.makeText(requireContext(), "昵称最多 32 个字符", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    viewLifecycleOwner.lifecycleScope.launch {
                        services.authRepository.updateProfile(raw)
                            .onSuccess { data ->
                                runCatching { services.sessionStore.applyProfileUpdate(data) }
                                    .onFailure { e ->
                                        Toast.makeText(
                                            requireContext(),
                                            e.message ?: "保存本地失败",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@launch
                                    }
                                Toast.makeText(requireContext(), "已保存", Toast.LENGTH_SHORT).show()
                            }
                            .onFailure { e ->
                                Toast.makeText(
                                    requireContext(),
                                    e.message ?: "保存失败",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
                .show()
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
