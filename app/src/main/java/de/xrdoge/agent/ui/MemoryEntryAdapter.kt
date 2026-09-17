package de.xrdoge.agent.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.xrdoge.agent.R

data class MemoryEntry(val key: String, val value: String)

class MemoryEntryAdapter(
    private val entries: MutableList<MemoryEntry>,
    private val onDelete: (MemoryEntry) -> Unit
) : RecyclerView.Adapter<MemoryEntryAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val keyText: TextView = view.findViewById(R.id.memoryItemKey)
        val valueText: TextView = view.findViewById(R.id.memoryItemValue)
        val deleteButton: TextView = view.findViewById(R.id.memoryItemDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_memory_entry, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = entries[position]
        holder.keyText.text = entry.key
        holder.valueText.text = entry.value
        holder.deleteButton.setOnClickListener { onDelete(entry) }
    }

    override fun getItemCount(): Int = entries.size

    fun replace(newEntries: List<MemoryEntry>) {
        entries.clear()
        entries.addAll(newEntries)
        notifyDataSetChanged()
    }
}
