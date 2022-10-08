package com.xvlaze.clover.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.github.amlcurran.showcaseview.ShowcaseView
import com.github.amlcurran.showcaseview.targets.ViewTarget
import com.xvlaze.clover.R
import com.xvlaze.clover.databinding.ActivityDataFormBinding
import com.xvlaze.clover.util.Extensions.disable
import com.xvlaze.clover.util.Extensions.enable

class DataFormActivity : ThemedActivity() {

    private lateinit var binding: ActivityDataFormBinding
    private lateinit var viewModel: DataFormViewModel
    private lateinit var scv: ShowcaseView
    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            if (uriContent != null) {
                viewModel.scanTreatment(uriContent)
            }
        } else {
            Toast.makeText(this@DataFormActivity, R.string.error_reconocimiento, Toast.LENGTH_LONG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(
            this,
            DataFormViewModel.MyViewModelFactory(application)
        )[DataFormViewModel::class.java]

        binding.btnConfirm.disable()

        binding.diasLayout.visibility = View.GONE

        binding.datePicker.minDate = System.currentTimeMillis() - 1000
        val dayPicker = binding.ipickerNum
        dayPicker.minValue = 1
        dayPicker.maxValue = 30

        val dwmPicker = binding.ipickerDwm
        dwmPicker.minValue = 0
        dwmPicker.maxValue = 1
        dwmPicker.displayedValues = arrayOf(getString(R.string.days), getString(R.string.weeks))

        binding.etBlister.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                viewModel.onBlisterQuantityChanged(charSequence.toString())
            }

            override fun afterTextChanged(editable: Editable) {
                viewModel.checkIntegrity()
            }
        })

        binding.etDosis.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                viewModel.onIntakeQuantityChanged(charSequence.toString())
            }

            override fun afterTextChanged(editable: Editable) {
                viewModel.checkIntegrity()
            }
        })

        val lunes = binding.cbLunes
        val martes = binding.cbMartes
        val miercoles = binding.cbMiercoles
        val jueves = binding.cbJueves
        val viernes = binding.cbViernes
        val sabado = binding.cbSabado
        val domingo = binding.cbDomingo

        // TODO: Cuando editamos, pasamos por aquí aunque estemos dentro del Observer¿
        lunes.setOnCheckedChangeListener { compoundButton, b ->
            viewModel.onDayCheckedChange(compoundButton.id, b)
        }
        martes.setOnCheckedChangeListener { compoundButton, b ->
            viewModel.onDayCheckedChange(compoundButton.id, b)
        }
        miercoles.setOnCheckedChangeListener { compoundButton, b ->
            viewModel.onDayCheckedChange(compoundButton.id, b)
        }
        jueves.setOnCheckedChangeListener { compoundButton, b ->
            viewModel.onDayCheckedChange(compoundButton.id, b)
        }
        viernes.setOnCheckedChangeListener { compoundButton, b ->
            viewModel.onDayCheckedChange(compoundButton.id, b)
        }
        sabado.setOnCheckedChangeListener { compoundButton, b ->
            viewModel.onDayCheckedChange(compoundButton.id, b)
        }
        domingo.setOnCheckedChangeListener { compoundButton, b ->
            viewModel.onDayCheckedChange(compoundButton.id, b)
        }

        binding.diasLayout.visibility = View.GONE
        binding.intervalPicker.visibility = View.GONE

        // TODO: scrollTo
        binding.optionA.setOnClickListener {
            binding.diasLayout.visibility = View.GONE
            binding.intervalPicker.visibility = View.GONE
            viewModel.onRadioButtonChecked(binding.optionA.id)
        }
        binding.optionB.setOnClickListener {
            binding.diasLayout.visibility = View.VISIBLE
            binding.intervalPicker.visibility = View.GONE
            viewModel.onRadioButtonChecked(binding.optionB.id)
        }
        binding.optionC.setOnClickListener {
            binding.diasLayout.visibility = View.GONE
            binding.intervalPicker.visibility = View.VISIBLE
            viewModel.onRadioButtonChecked(binding.optionC.id)
        }

        viewModel.scannedTreatment.observe(this) { scannedName ->
            binding.etNombre.setText(scannedName)
        }

        val editedTreatmentName = intent.getStringExtra("nombreTratamiento")
        viewModel.areWeEditing(editedTreatmentName)
        viewModel.isEditMode.observe(this) { inEditMode ->
            if (inEditMode) {
                viewModel.findTreatmentByName(editedTreatmentName)
                viewModel.editedTreatment.observe(this) { t ->
                    binding.etNombre.setText(t.mNombre)
                    binding.etNombre.isEnabled = false
                    viewModel.onTextChanged(t.mNombre)
                    binding.etBlister.setText(t.mBlister.toString())
                    viewModel.onBlisterQuantityChanged(t.mBlister.toString())
                    binding.etDosis.setText(t.mPastillasPorTreatment.toString())
                    viewModel.onIntakeQuantityChanged(t.mPastillasPorTreatment.toString())
                    when {
                        t.mFreqDias.all { true } -> {
                            binding.optionA.isChecked = true
                            viewModel.onRadioButtonChecked(binding.optionA.id)
                            viewModel.checkIntegrity()
                        }
                        t.mFreqDias.all { false } -> {
                            binding.optionC.isChecked = true
                            binding.ipickerNum.value = t.mFreqNum
                            binding.ipickerDwm.value = t.mFreqTime
                            viewModel.onRadioButtonChecked(binding.optionC.id)
                            viewModel.checkIntegrity()
                        }
                        else -> {
                            binding.optionB.isChecked = true
                            domingo.isChecked = t.mFreqDias[0]
                            lunes.isChecked = t.mFreqDias[1]
                            martes.isChecked = t.mFreqDias[2]
                            miercoles.isChecked = t.mFreqDias[3]
                            jueves.isChecked = t.mFreqDias[4]
                            viernes.isChecked = t.mFreqDias[5]
                            sabado.isChecked = t.mFreqDias[6]
                            viewModel.onRadioButtonChecked(binding.optionB.id)
                            viewModel.checkIntegrity()
                        }
                    }
                }
            } else {
                binding.etNombre.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                    override fun onTextChanged(
                        charSequence: CharSequence,
                        i: Int,
                        i1: Int,
                        i2: Int
                    ) {
                        viewModel.onTextChanged(charSequence.toString())
                    }

                    override fun afterTextChanged(editable: Editable) {
                        viewModel.checkIntegrity()
                    }
                })
            }

            binding.textPack.setOnClickListener {
                launchIndividualTutorial(it)
            }

            binding.textDosis.setOnClickListener {
                launchIndividualTutorial(it)
            }

            binding.textFecha.setOnClickListener {
                launchIndividualTutorial(it)
            }

            binding.textFreq.setOnClickListener {
                launchIndividualTutorial(it)
            }
        }

        viewModel.correctTreatment.observe(this) {
            if (it) binding.btnConfirm.enable() else binding.btnConfirm.disable()
        }

        binding.btnCamera.setOnClickListener {
            viewModel.askForPermissions(this)
        }

        binding.btnConfirm.setOnClickListener {
            Intent(applicationContext, SetTimeActivity::class.java).apply {
                putExtra("name", binding.etNombre.text.toString())
                putExtra("blister", binding.etBlister.text.toString().toInt())
                putExtra("intake", binding.etDosis.text.toString().toInt())
                putExtra("day", binding.datePicker.dayOfMonth)
                putExtra("month", binding.datePicker.month + 1)
                putExtra("year", binding.datePicker.year)
                putExtra("specificDays", viewModel.treatmentDays)
                putExtra("int_dias", binding.ipickerNum.value)
                putExtra("int_tiempo", binding.ipickerDwm.value)
                putExtra("alarmId", intent.getIntExtra("alarmId", -1))
                startActivity(this)
                finish()
                overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
            }
        }

        viewModel.arePermissionsGranted.observe(this) { areGranted ->
            if (areGranted) showImagePickerOptions() else showSettingsDialog()
        }

        if (intent.getStringExtra("from") != null) tutorialSequence()
    }

    // FIXME: Con un tema diferente a Noon, se carga el tema de showcase Noon.
    private fun launchIndividualTutorial(it: View?) {
        when (it) {
            binding.textPack -> {
                scv = ShowcaseView.Builder(this)
                    .setTarget(ViewTarget(it))
                    .setContentTitle(getString(R.string.tutorial_title_blister))
                    .setContentText(getString(R.string.tutorial_subtitle_blister))
                    .build()
            }
            binding.textDosis -> {
                scv = ShowcaseView.Builder(this)
                    .setTarget(ViewTarget(it))
                    .setContentTitle(getString(R.string.tutorial_title_dosis))
                    .setContentText(getString(R.string.tutorial_subtitle_dosis))
                    .build()
            }
            binding.textFecha -> {
                scv = ShowcaseView.Builder(this)
                    .setTarget(ViewTarget(it))
                    .setContentTitle(getString(R.string.tutorial_title_fecha))
                    .setContentText(getString(R.string.tutorial_subtitle_fecha))
                    .build()
            }
            binding.textFreq -> {
                scv = ShowcaseView.Builder(this)
                    .setTarget(ViewTarget(it))
                    .setContentTitle(getString(R.string.tutorial_title_freq))
                    .setContentText(getString(R.string.tutorial_subtitle_freq))
                    .build()
            }
        }

        // FIXME: Revisar porque fuera de Noon no personaliza. ¿Cómo lo hago?
        viewModel.getTheme()
        viewModel.theme.observe(this) { theme ->
            when (theme) {
                R.style.Night -> scv.setStyle(R.style.IndividualNightShowcaseTheme)
                R.style.Dawn -> scv.setStyle(R.style.IndividualMorningShowcaseTheme)
                R.style.Dusk -> scv.setStyle(R.style.IndividualDuskShowcaseTheme)
                else -> scv.setStyle(R.style.IndividualNoonShowcaseTheme)
            }
        }
    }

    private fun showImagePickerOptions() {
        cropImage.launch(
            options {
                setGuidelines(CropImageView.Guidelines.ON)
            }
        )
    }

    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        val viewGroup = findViewById<ViewGroup>(R.id.content) // FIXME
        val dialogView: View =
            LayoutInflater.from(this).inflate(R.layout.dialog_permissions, viewGroup, false)
        builder.setView(dialogView)
        val dialogConfirm = builder.create()
        val permissionReq = dialogView.findViewById<TextView>(R.id.permission_req)
        permissionReq.setText(R.string.camera_permission)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            dialogConfirm.cancel()
            openSettings()
        }
        val btnDismiss = dialogView.findViewById<Button>(R.id.btnDismiss)
        btnDismiss.setOnClickListener { dialogConfirm.cancel() }
        dialogConfirm.show()
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun tutorialSequence() {
        // No queremos pasar de pantalla accidentalmente en mitad del tutorial.
        binding.btnConfirm.isEnabled = false
        val etNombre = binding.etNombre
        etNombre.isEnabled = false
        etNombre.inputType = InputType.TYPE_NULL
        val scrollView: ScrollView = findViewById(R.id.scrollView2)
        // Siempre comenzaremos el tutorial desde arriba de la pantalla.
        scrollView.post { scrollView.smoothScrollTo(0, 0) }
        binding.optionA.isChecked = true
        scv = ShowcaseView.Builder(this)
            .setTarget(ViewTarget(findViewById(R.id.text_name)))
            .setContentTitle(getString(R.string.tutorial_title_insertar))
            .setContentText(getString(R.string.tutorial_subt_insertar))
            .build()

        viewModel.getTheme()
        viewModel.theme.observe(this) { theme ->
            when (theme) {
                0 -> scv.setStyle(R.style.NightShowcaseTheme)
                1 -> scv.setStyle(R.style.MorningShowcaseTheme)
                3 -> scv.setStyle(R.style.DuskShowcaseTheme)
                else -> scv.setStyle(R.style.NoonShowcaseTheme)
            }
        }

        scv.overrideButtonClick {
            scrollView.smoothScrollTo(etNombre.scrollX, etNombre.scrollY)
            scv.setTarget(ViewTarget(findViewById(R.id.text_pack)))
            scv.setContentTitle(getString(R.string.tutorial_title_blister))
            scv.setContentText(getString(R.string.tutorial_subtitle_blister))

            scv.overrideButtonClick {
                scrollView.smoothScrollTo(binding.etBlister.scrollX, binding.etBlister.scrollY)
                scv.setTarget(ViewTarget(findViewById(R.id.text_dosis)))
                scv.setContentTitle(getString(R.string.tutorial_title_dosis))
                scv.setContentText(getString(R.string.tutorial_subtitle_dosis))

                scv.overrideButtonClick {
                    scrollView.post {
                        scrollView.fullScroll(
                            View.FOCUS_DOWN
                        )
                    }
                    scv.setTarget(ViewTarget(findViewById(R.id.text_fecha)))
                    scv.setContentTitle(getString(R.string.tutorial_title_fecha))
                    scv.setContentText(getString(R.string.tutorial_subtitle_fecha))

                    scv.overrideButtonClick {
                        scrollView.post {
                            scrollView.fullScroll(
                                View.FOCUS_DOWN
                            )
                        }
                        scv.setTarget(
                            ViewTarget(
                                binding.optionA
                            )
                        )
                        scv.setContentTitle(getString(R.string.tutorial_title_freq))
                        scv.setContentText(getString(R.string.tutorial_subtitle_freq))

                        scv.overrideButtonClick {
                            binding.optionB.isChecked = true
                            scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
                            scv.setTarget(
                                ViewTarget(
                                    binding.optionB
                                )
                            )
                            scv.setContentTitle(getString(R.string.tutorial_title_dias))
                            scv.setContentText(getString(R.string.tutorial_subtitle_dias))

                            scv.overrideButtonClick {
                                binding.optionC.isChecked = true
                                scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
                                scv.setTarget(
                                    ViewTarget(
                                        binding.optionC
                                    )
                                )
                                scv.setContentTitle(getString(R.string.tutorial_title_interval))
                                scv.setContentText(getString(R.string.tutorial_subtitle_interval))

                                scv.overrideButtonClick {
                                    val btnConfirm: Button = binding.btnConfirm
                                    btnConfirm.enable()
                                    btnConfirm.isEnabled = false
                                    scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
                                    scv.setTarget(
                                        ViewTarget(
                                            btnConfirm
                                        )
                                    )
                                    scv.setContentTitle(getString(R.string.tutorial_title_hours))
                                    scv.setContentText(getString(R.string.tutorial_subtitle_hours))

                                    scv.overrideButtonClick {
                                        Intent(
                                            applicationContext,
                                            MainActivity::class.java
                                        ).apply {
                                            putExtra("tutorial", true)
                                            viewModel.completeTutorialPart()
                                            startActivity(this)
                                        }
                                        finish()
                                        overridePendingTransition(
                                            R.anim.slide_from_right,
                                            R.anim.slide_to_left
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}