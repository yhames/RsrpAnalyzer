package com.example.rsrpanalyzer.view.signal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rsrpanalyzer.data.model.SignalRecord
import com.example.rsrpanalyzer.databinding.ItemSignalRecordBinding

class SignalRecordAdapter : RecyclerView.Adapter<SignalRecordAdapter.SignalRecordViewHolder>() {

    private val records = mutableListOf<SignalRecord>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SignalRecordViewHolder {
        val binding = ItemSignalRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SignalRecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SignalRecordViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    fun addRecord(record: SignalRecord) {
        records.add(0, record) // 맨 위에 추가
        notifyItemInserted(0)
    }

    fun clearRecords() {
        records.clear()
        notifyDataSetChanged()
    }

    class SignalRecordViewHolder(private val binding: ItemSignalRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: SignalRecord) {
            binding.itemLatitude.text = String.format("%.6f", record.latitude)
            binding.itemLongitude.text = String.format("%.6f", record.longitude)
            binding.itemRsrp.text = record.rsrp.toString()
            binding.itemRsrq.text = record.rsrq.toString()
        }
    }
}