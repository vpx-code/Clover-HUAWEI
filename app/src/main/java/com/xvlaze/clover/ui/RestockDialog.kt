package com.xvlaze.clover.ui

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.xvlaze.clover.R
import com.xvlaze.clover.databinding.ActivityRestockDialogBinding

class RestockDialog : AppCompatActivity() {
    private lateinit var binding: ActivityRestockDialogBinding
    private lateinit var viewModel: RestockViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRestockDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(
            this,
            RestockViewModel.MyViewModelFactory(application)
        )[RestockViewModel::class.java]
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        val alarmId = intent.getIntExtra("alarmId", -1)
        viewModel.findTreatmentByName(intent.getStringExtra("nombreTratamiento")!!)
        viewModel.treatment.observe(this) { treatment ->
            val nameStr = treatment.mNombre
            val name: TextView = findViewById(R.id.name)
            name.text = nameStr

            val packStr = applicationContext.resources.getQuantityString(
                R.plurals.blister_qty,
                treatment.mBlister,
                treatment.mBlister
            )
            val pack: TextView = findViewById(R.id.pack)
            pack.text = Html.fromHtml(packStr, Html.FROM_HTML_MODE_LEGACY)

            val doseStr = applicationContext.resources.getQuantityString(
                R.plurals.intake_qty,
                treatment.mPastillasPorTreatment,
                treatment.mPastillasPorTreatment
            )
            val dose: TextView = findViewById(R.id.dosis)
            dose.text = Html.fromHtml(doseStr, Html.FROM_HTML_MODE_LEGACY)

            val freqStr = getString(
                R.string.frequency,
                treatment.prettifyFrequency(this)
            )
            val freq: TextView = findViewById(R.id.freq)
            freq.text = Html.fromHtml(freqStr, Html.FROM_HTML_MODE_LEGACY)

            val btnEdit = findViewById<Button>(R.id.btnEditar)
            btnEdit.setOnClickListener {
                val i = Intent(it.context, DataFormActivity::class.java)
                i.putExtra("nombreTratamiento", nameStr)
                i.putExtra("alarmId", alarmId) // FIXME Ojo que no lo pilla
                it.context.startActivity(i)
                finish()
            }

            val btnConfirmar = findViewById<Button>(R.id.btnConfirmar)
            btnConfirmar.setOnClickListener {
                viewModel.restock(treatment)
                viewModel.cancelAlarm(alarmId)
                Intent(
                    it.context,
                    MainActivity::class.java
                ).apply { it.context.startActivity(this) }
                finish()
            }
        }
    }
}