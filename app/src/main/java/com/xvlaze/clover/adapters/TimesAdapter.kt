package com.xvlaze.clover.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xvlaze.clover.databinding.ItemListTimeBinding

class TimesAdapter(val timesList: MutableList<String>): RecyclerView.Adapter<TimesAdapter.TimesViewHolder>() {
    private lateinit var listener: IOnItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimesViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemListTimeBinding.inflate(layoutInflater, parent, false)
        return TimesViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: TimesViewHolder, position: Int) {
        val time = timesList[position]
        holder.binding.nombreHora.text = time
    }

    override fun getItemCount(): Int = timesList.size

    fun setOnItemClickListener(listener: IOnItemClickListener) {
        this.listener = listener
    }

    class TimesViewHolder(
        val binding: ItemListTimeBinding,
        listener: IOnItemClickListener
    ): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.deleteHora.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }
    }
}