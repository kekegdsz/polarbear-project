package com.undersky.androidim.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.undersky.androidim.feature.home.adapters.CreateGroupPickAdapter
import com.undersky.androidim.feature.home.adapters.CreateGroupPickRow
import com.undersky.androidim.feature.home.databinding.DialogBottomSheetCreateGroupBinding
import com.undersky.androidim.shared.ui.R as UiR
import com.undersky.business.user.DirectoryUserDto

/**
 * 微信式：底部弹层 + 列表勾选联系人，避免 AlertDialog 多选与说明文字冲突导致无法选人。
 */
class CreateGroupPickBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "CreateGroupPickBottomSheet"
        const val REQUEST_KEY = "create_group_member_pick"
        const val RESULT_MEMBER_IDS = "member_ids"
        private const val ARG_IDS = "ids"
        private const val ARG_NAMES = "names"

        fun newInstance(users: List<DirectoryUserDto>): CreateGroupPickBottomSheet {
            val ids = users.map { it.id }.toLongArray()
            val names = users.map { u ->
                u.nickname?.takeIf { it.isNotBlank() }
                    ?: u.username?.takeIf { it.isNotBlank() }
                    ?: "用户 ${u.id}"
            }.toTypedArray()
            return CreateGroupPickBottomSheet().apply {
                arguments = Bundle().apply {
                    putLongArray(ARG_IDS, ids)
                    putStringArray(ARG_NAMES, names)
                }
            }
        }

        fun show(manager: FragmentManager, users: List<DirectoryUserDto>) {
            newInstance(users).show(manager, TAG)
        }
    }

    private var _binding: DialogBottomSheetCreateGroupBinding? = null
    private val binding get() = _binding!!

    private var adapter: CreateGroupPickAdapter? = null

    override fun getTheme(): Int = R.style.ThemeOverlay_Home_BottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogBottomSheetCreateGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ids = requireArguments().getLongArray(ARG_IDS) ?: longArrayOf()
        val names = requireArguments().getStringArray(ARG_NAMES) ?: emptyArray()
        val rows = ids.mapIndexed { i, id ->
            CreateGroupPickRow(id, names.getOrElse(i) { "用户 $id" })
        }
        val ad = CreateGroupPickAdapter(rows) { count ->
            binding.buttonDone.isEnabled = count > 0
            binding.buttonDone.text = if (count <= 0) "完成" else "完成($count)"
        }
        adapter = ad
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                setDrawable(requireNotNull(ContextCompat.getDrawable(requireContext(), UiR.drawable.list_divider)))
            }
        )
        binding.recycler.adapter = ad

        binding.buttonClose.setOnClickListener { dismiss() }
        binding.buttonDone.setOnClickListener {
            val picked = ad.selectedIds()
            if (picked.isEmpty()) return@setOnClickListener
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(RESULT_MEMBER_IDS to picked.toLongArray())
            )
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
            sheet.layoutParams.height = (resources.displayMetrics.heightPixels * 0.78f).toInt()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        _binding = null
    }
}
