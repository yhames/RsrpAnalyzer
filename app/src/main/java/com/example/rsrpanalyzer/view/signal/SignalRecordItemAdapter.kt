package com.example.rsrpanalyzer.view.signal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rsrpanalyzer.databinding.ItemSignalRecordBinding
import java.util.Locale

class SignalRecordItemAdapter :
    RecyclerView.Adapter<SignalRecordItemAdapter.SignalRecordViewHolder>() {

    private val records = mutableListOf<SignalRecordItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SignalRecordViewHolder {
        val binding =
            ItemSignalRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SignalRecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SignalRecordViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    fun addRecordItem(record: SignalRecordItem) {
        records.add(0, record) // 맨 위에 추가
        notifyItemInserted(0)
    }

    fun clearRecordItems() {
        val size = records.size
        records.clear()
        notifyItemRangeRemoved(0, size)
    }

    class SignalRecordViewHolder(private val binding: ItemSignalRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(record: SignalRecordItem) {
            binding.itemLatitude.text = String.format(Locale.US, "%.6f", record.latitude)
            binding.itemLongitude.text = String.format(Locale.US, "%.6f", record.longitude)
            binding.itemRsrp.text = record.rsrp.toString()
            binding.itemRsrq.text = record.rsrq.toString()
        }
    }
}
