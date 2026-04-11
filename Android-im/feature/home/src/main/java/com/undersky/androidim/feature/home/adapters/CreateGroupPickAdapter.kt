package com.undersky.androidim.feature.home.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.undersky.androidim.feature.home.databinding.ItemCreateGroupPickBinding

data class CreateGroupPickRow(val userId: Long, val displayName: String)

class CreateGroupPickAdapter(
    private val rows: List<CreateGroupPickRow>,
    private val onSelectionChanged: (count: Int) -> Unit
) : RecyclerView.Adapter<CreateGroupPickAdapter.Vh>() {

    private val selectedIds = linkedSetOf<Long>()

    fun selectedIds(): List<Long> = selectedIds.toList()

    override fun getItemCount(): Int = rows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val binding = ItemCreateGroupPickBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Vh(binding)
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        holder.bind(rows[position], position)
    }

    inner class Vh(private val binding: ItemCreateGroupPickBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: CreateGroupPickRow, position: Int) {
            binding.textName.text = row.displayName
            binding.avatarLetter.text = row.displayName.take(1)
            binding.checkbox.isChecked = selectedIds.contains(row.userId)
            binding.root.setOnClickListener {
                val id = row.userId
                if (selectedIds.contains(id)) {
                    selectedIds.remove(id)
                } else {
                    selectedIds.add(id)
                }
                notifyItemChanged(position)
                onSelectionChanged(selectedIds.size)
            }
        }
    }
}
