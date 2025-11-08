package com.example.rsrpanalyzer.view.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rsrpanalyzer.R
import com.example.rsrpanalyzer.databinding.ItemSessionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionHistoryAdapter(
    private val onSessionClick: (SessionItem) -> Unit
) : ListAdapter<SessionItem, SessionHistoryAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding, onSessionClick)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SessionViewHolder(
        private val binding: ItemSessionBinding,
        private val onSessionClick: (SessionItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        fun bind(item: SessionItem) {
            binding.tvSessionName.text = item.sessionName
            binding.tvSessionDate.text = dateFormat.format(Date(item.createdAt))
            binding.tvRecordCount.text = binding.root.context.getString(
                R.string.session_record_count,
                item.recordCount
            )

            binding.root.setOnClickListener {
                onSessionClick(item)
            }
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<SessionItem>() {
        override fun areItemsTheSame(oldItem: SessionItem, newItem: SessionItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SessionItem, newItem: SessionItem): Boolean {
            return oldItem == newItem
        }
    }
}
