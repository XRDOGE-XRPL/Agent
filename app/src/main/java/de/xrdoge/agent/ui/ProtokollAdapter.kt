package de.xrdoge.agent.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.xrdoge.agent.R

class ProtokollAdapter : RecyclerView.Adapter<ProtokollAdapter.Halter>() {
    private val eintraege = mutableListOf<String>()

    fun hinzufuegen(zeile: String) {
        eintraege.add(zeile)
        notifyItemInserted(eintraege.size - 1)
    }

    fun leeren() {
        val alt = eintraege.size
        eintraege.clear()
        notifyItemRangeRemoved(0, alt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Halter {
        val sicht = LayoutInflater.from(parent.context)
            .inflate(R.layout.eintrag_protokoll, parent, false)
        return Halter(sicht)
    }

    override fun onBindViewHolder(holder: Halter, position: Int) {
        holder.text.text = eintraege[position]
    }

    override fun getItemCount(): Int = eintraege.size

    class Halter(sicht: View) : RecyclerView.ViewHolder(sicht) {
        val text: TextView = sicht.findViewById(R.id.protokollText)
    }
}
