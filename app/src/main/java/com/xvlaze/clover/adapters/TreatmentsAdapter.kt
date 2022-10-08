package com.xvlaze.clover.adapters

import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xvlaze.clover.R
import com.xvlaze.clover.databinding.ItemListTreatmentBinding
import com.xvlaze.clover.model.AlarmPool
import com.xvlaze.clover.model.Treatment

class TreatmentsAdapter(var treatmentsList: MutableList<Treatment>): RecyclerView.Adapter<TreatmentsAdapter.TreatmentsViewHolder>() {
    private lateinit var listener: IOnItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreatmentsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemListTreatmentBinding.inflate(layoutInflater, parent, false)
        return TreatmentsViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: TreatmentsViewHolder, position: Int) {
        val t = treatmentsList[position]
        holder.binding.nombreTratamiento.text = t.mNombre
        val nextIntake: String = holder.binding.root.context.getString(
            R.string.next_intake,
            AlarmPool.instance.findTreatmentsUpNext(t.mNombre).second
        )
        val styledNextIntake = Html.fromHtml(nextIntake, Html.FROM_HTML_MODE_LEGACY)
        holder.binding.proxima.text = styledNextIntake
        holder.binding.restantes.text = holder.binding.root.context.getString(R.string.remaining,
            t.mTreatmentRestantes.toString()
        )
    }

    override fun getItemCount(): Int = treatmentsList.size

    fun setOnItemClickListener(listener: IOnItemClickListener) {
        this.listener = listener
    }

    class TreatmentsViewHolder (val binding: ItemListTreatmentBinding, listener: IOnItemClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.moreTratamiento.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }
    }
}