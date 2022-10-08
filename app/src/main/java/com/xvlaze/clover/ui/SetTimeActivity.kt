package com.xvlaze.clover.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.TimePicker
import androidx.lifecycle.ViewModelProvider
import com.xvlaze.clover.adapters.IOnItemClickListener
import com.xvlaze.clover.adapters.TimesAdapter
import com.xvlaze.clover.databinding.ActivitySetTimeBinding
import com.xvlaze.clover.model.Treatment
import com.xvlaze.clover.util.Extensions.trimMinute
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

class SetTimeActivity : ThemedActivity() {
    private lateinit var adapter: TimesAdapter
    private lateinit var binding: ActivitySetTimeBinding
    private lateinit var viewModel: SetTimeViewModel
    private var day = 0
    private var month = 0
    private var year = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetTimeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(
            this,
            SetTimeViewModel.MyViewModelFactory(application)
        )[SetTimeViewModel::class.java]

        // Todo: Analytics & Ads

        binding.addTime.setOnClickListener {
            // TODO: DatePicker
            val mcurrentTime = Calendar.getInstance()
            val hour = mcurrentTime[Calendar.HOUR_OF_DAY]
            val minute = mcurrentTime[Calendar.MINUTE]

            // TODO: Solo quiero intervalos de 5 minutos.
            TimePickerDialog(
                this@SetTimeActivity,
                { _: TimePicker?, selectedHour: Int, selectedMinute: Int ->
                    val hora = StringBuilder()
                    if (selectedHour < 10) hora.append("0")
                    hora.append(selectedHour).append(":")
                    if (selectedMinute < 10) hora.append("0")
                    hora.append(selectedMinute)
                    val selectedTime = LocalTime.parse(hora).toString()

                    val timesList = viewModel.timesList.value
                    if (timesList != null) {
                        if (!timesList.contains(selectedTime)) {
                            viewModel.addTime(selectedTime)
                        }
                    }
                }, hour, minute, true
            ).apply {
                setTitle("Select Time")
                show()
            }
        }

        val editedTreatmentName = intent.getStringExtra("name") ?: ""
        viewModel.areWeEditing(editedTreatmentName)
        viewModel.isEditMode.observe(this) { inEditMode ->
            viewModel.timesList.observe(this) {
                handleEmptyTimes(it.isEmpty())
                adapter = TimesAdapter(it)

                binding.recyclerHoras.adapter = adapter
                adapter.setOnItemClickListener(object : IOnItemClickListener {
                    override fun onItemClick(position: Int) {
                        viewModel.deleteTime(it[position])
                        adapter.notifyItemChanged(position)
                        handleEmptyTimes(it.isEmpty())
                    }
                })
                adapter.notifyDataSetChanged() // TODO: Hace falta?
            }

            if (inEditMode) {
                viewModel.restoreTimes(editedTreatmentName)
                // TODO: Rutina para reemplazar el tratamiento y no crear 2 con el mismo nombre.
            }
        }

        binding.setTimesBtn.setOnClickListener {
            val name = intent.getStringExtra("name")

            val blister = intent.getIntExtra("blister", 0)
            val intake = intent.getIntExtra("intake", 0)


            val day = intent.getIntExtra("day", 0)
            val month = intent.getIntExtra("month", 0)
            val year = intent.getIntExtra("year", 0)

            //  Array de días de la semana seleccionados por el usuario.
            val specificDays = intent.getBooleanArrayExtra("specificDays") // TODO: Estp es correcto?

            // Para el intervalo de tiempo (Opción 3).
            val every = intent.getIntExtra("int_dias", 0)
            val timeUnit = intent.getIntExtra("int_tiempo", 0)

            // TODO: Para respetar la independencia entre ViewModels, pasar esto como extra a la MainActivity y guardarlo desde ahí.
            val t = Treatment(
                name!!,
                blister,
                // Acabo de convertir esto a toMinutes para no ser tan precisos.
                OffsetDateTime.of(year, month, day, 0, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli().trimMinute(),
                adapter.timesList as ArrayList<String>,
                intake,
                every,
                timeUnit,
                specificDays!!,
                blister / intake, // Al parecer, esto tiene que hacerse aquí porque, de lo contrario, no se pasa al JSON la primera vez.
                "01/01/1970"
            )
            viewModel.saveTreatment(t)
            // Aquí guardaba 2 veces el tratamiento y no sé por qué...
            finish()
        }
    }

    fun handleEmptyTimes(isTimesListEmpty: Boolean) {
        binding.setTimesBtn.visibility = if (isTimesListEmpty) View.INVISIBLE else View.VISIBLE
        binding.emptyTime.visibility = if (isTimesListEmpty) View.VISIBLE else View.INVISIBLE
    }
}