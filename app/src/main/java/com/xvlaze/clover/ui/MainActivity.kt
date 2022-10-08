package com.xvlaze.clover.ui

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.github.amlcurran.showcaseview.ShowcaseView
import com.github.amlcurran.showcaseview.targets.ViewTarget
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.xvlaze.clover.R
import com.xvlaze.clover.adapters.IOnItemClickListener
import com.xvlaze.clover.adapters.TreatmentsAdapter
import com.xvlaze.clover.databinding.ActivityMainBinding
import com.xvlaze.clover.databinding.DialogInfoBinding
import com.xvlaze.clover.databinding.DialogOmitirDosisBinding
import com.xvlaze.clover.watch.Watch
import spencerstudios.com.bungeelib.Bungee

// TODO: Botón SEttings para informar al usuario de que confiugure el Inicio de Aplicacioines

class MainActivity : ThemedActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: TreatmentsAdapter
    private lateinit var scv: ShowcaseView
    private lateinit var scv2: ShowcaseView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: SetContentView a una pantalla del color del tema activo.
        viewModel = ViewModelProvider(
            this,
            MainViewModel.MyViewModelFactory(application)
        )[MainViewModel::class.java]

        viewModel.isFirstTime()
        viewModel.tutorialState.observe(this) { part ->
            when (part) {
                0 -> {
                    // Starts the tutorial.
                    Intent(this, OnboardingActivity::class.java).apply {
                        startActivity(this)
                    }
                    finish()
                }
                else -> {
                    binding = ActivityMainBinding.inflate(layoutInflater)
                    setContentView(binding.root)

                    binding.btnAdd.setOnClickListener {
                        Intent(this, DataFormActivity::class.java).apply {
                            startActivity(this)
                        }
                    }

                    binding.btnSettings.setOnClickListener {
                        showAutoStartDialog()
                    }

                    // FIXME! MVVM
                    binding.btnMap.setOnClickListener {
                        Dexter.withContext(this)
                            .withPermissions(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                            .withListener(object : MultiplePermissionsListener {
                                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                                    if (report.areAllPermissionsGranted()) {
                                        val i =
                                            Intent(applicationContext, PharmaActivity::class.java)
                                        startActivity(i)
                                        Bungee.slideLeft(this@MainActivity)
                                    }
                                    if (report.isAnyPermissionPermanentlyDenied) {
                                        val builder =
                                            AlertDialog.Builder(
                                                this@MainActivity,
                                                R.style.CustomAlertDialog
                                            )
                                        val viewGroup = findViewById<ViewGroup>(R.id.content)
                                        val dialogView = LayoutInflater.from(this@MainActivity)
                                            .inflate(R.layout.dialog_permissions, viewGroup, false)
                                        builder.setView(dialogView)
                                        val dialogConfirm = builder.create()
                                        val permissionReq =
                                            dialogView.findViewById<TextView>(R.id.permission_req)
                                        permissionReq.setText(R.string.location_permission)
                                        val btnConfirm =
                                            dialogView.findViewById<Button>(R.id.btnConfirm)
                                        btnConfirm.setOnClickListener {
                                            dialogConfirm.cancel()
                                            val intent =
                                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                            val uri =
                                                Uri.fromParts("package", packageName, null)
                                            intent.data = uri
                                            startActivityForResult(intent, 101) // FIXME
                                        }
                                        val btnDismiss =
                                            dialogView.findViewById<Button>(R.id.btnDismiss)
                                        btnDismiss.setOnClickListener { dialogConfirm.cancel() }
                                        dialogConfirm.show()
                                    }
                                }

                                override fun onPermissionRationaleShouldBeShown(
                                    permissions: List<PermissionRequest>,
                                    token: PermissionToken
                                ) {
                                    token.continuePermissionRequest()
                                }
                            }).check()
                    }

                    when (part) {
                        1, 2 -> {
                            tutorialSequence(part)
                        }
                        else -> {
                            loadList()
                            viewModel.checkSnackbarStatus()
                            viewModel.hasSnackbarBeenShownOnce.observe(this) {
                                if (!it) {
                                    showAutoStartDialog()

                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadList() {
        loadClover()
        adapter = TreatmentsAdapter(arrayListOf())
        adapter.setOnItemClickListener(object : IOnItemClickListener {
            override fun onItemClick(position: Int) {
                val dialog = Dialog(this@MainActivity, R.style.CustomAlertDialog)
                val binding: DialogInfoBinding = DialogInfoBinding.inflate(layoutInflater)
                dialog.setContentView(binding.root)

                val d = adapter.treatmentsList[position]
                val nameStr: String = d.mNombre
                val packStr = applicationContext.resources.getQuantityString(
                    R.plurals.blister_qty,
                    d.mBlister,
                    d.mBlister
                )
                val doseStr = applicationContext.resources.getQuantityString(
                    R.plurals.intake_qty,
                    d.mPastillasPorTreatment,
                    d.mPastillasPorTreatment
                )
                val freqStr = getString(
                    R.string.frequency,
                    d.prettifyFrequency(applicationContext)
                )

                viewModel.findTreatmentsUpNext(nameStr)
                viewModel.treatmentUpNext.observe(this@MainActivity) {
                    val upNextStr = getString(
                        R.string.next_intake,
                        it
                    )
                    val upNext: TextView = binding.upnext
                    upNext.text = Html.fromHtml(upNextStr, Html.FROM_HTML_MODE_LEGACY)
                }

                val name: TextView = binding.name
                name.text = nameStr

                val pack: TextView = binding.pack
                pack.text = Html.fromHtml(packStr, Html.FROM_HTML_MODE_LEGACY)

                val dose: TextView = binding.dosis
                dose.text = Html.fromHtml(doseStr, Html.FROM_HTML_MODE_LEGACY)

                val freq: TextView = binding.freq
                freq.text = Html.fromHtml(freqStr, Html.FROM_HTML_MODE_LEGACY)

                val btnEdit = binding.btnEditar
                btnEdit.setOnClickListener { v: View ->
                    val i = Intent(v.context, DataFormActivity::class.java)
                    i.putExtra(
                        "nombreTratamiento",
                        d.mNombre
                    )
                    v.context.startActivity(i)
                    dialog.dismiss()
                    // finish()
                }

                val btnDelete = binding.btnDelete
                btnDelete.setOnClickListener {
                    dialog.dismiss()
                    val dialogConfirm = Dialog(this@MainActivity, R.style.CustomAlertDialog)
                    val bindingConfirm: DialogOmitirDosisBinding =
                        DialogOmitirDosisBinding.inflate(layoutInflater)
                    dialogConfirm.setContentView(bindingConfirm.root)

                    dialogConfirm.setCanceledOnTouchOutside(false)
                    val btnCancel = bindingConfirm.btnReponer
                    btnCancel.setText(R.string.atras)
                    btnCancel.setOnClickListener {
                        dialogConfirm.dismiss()
                        dialog.show()
                    }
                    val btnDeleteSure = bindingConfirm.btnOmitir
                    btnDeleteSure.setText(R.string.eliminar)
                    btnDeleteSure.setOnClickListener {
                        viewModel.delete(position)
                        dialogConfirm.dismiss()
                    }
                    dialogConfirm.show()
                }
                dialog.show()
            }
        })
        binding.recyclerTratamientos.adapter = adapter

        viewModel.treatmentsList.observe(this) {
            adapter.treatmentsList = it
            adapter.notifyDataSetChanged() // No sabemos si hemos añadido o eliminado. Habrá que pensar algo o dejarlo así.
            binding.textNone.visibility = if (it.isEmpty()) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun loadClover() {
        val cloverImage = binding.cloverMain
        cloverImage.setBackgroundResource(R.drawable.anim_idle)
        val frameAnimation = cloverImage.background as AnimationDrawable
        frameAnimation.start()
        viewModel.currentDialog.observe(this) { text ->
            val cloverDialogs = binding.dialog
            cloverDialogs.setCharacterDelay(35)
            cloverDialogs.animateText(text)
        }


        val scrollingBackground = binding.scrollingBackground
        scrollingBackground.stop()
        scrollingBackground.start()

        val scrollingMid = binding.scrollingMid
        scrollingMid.stop()
        scrollingMid.start()

        val scrollingForeground = binding.scrollingForeground
        scrollingForeground.stop()
        scrollingForeground.start()
    }

    private fun showAutoStartDialog() {
        val builder =
            AlertDialog.Builder(
                this@MainActivity,
                R.style.CustomAlertDialog
            )
        val viewGroup = findViewById<ViewGroup>(R.id.content)
        val dialogView = LayoutInflater.from(this@MainActivity)
            .inflate(R.layout.dialog_permissions, viewGroup, false)
        builder.setView(dialogView)
        val dialogConfirm = builder.create()
        val permissionReq =
            dialogView.findViewById<TextView>(R.id.permission_req)
        permissionReq.setText(R.string.autostart_permission)
        val btnConfirm =
            dialogView.findViewById<Button>(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            viewModel.setAutostartDialogAlreadyShown()
            dialogConfirm.cancel()
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
        val btnDismiss = dialogView.findViewById<Button>(R.id.btnDismiss)
        btnDismiss.setOnClickListener {
            dialogConfirm.cancel()
            Snackbar.make(binding.root, getString(R.string.snackbar_quit), Snackbar.LENGTH_LONG)
                .show()
        }
        dialogConfirm.setCanceledOnTouchOutside(false)
        dialogConfirm.show()
    }

    override fun onResume() {
        super.onResume()
        // Fixes treatment list not appearing after saving a treatment post-tutorial without killing the app.
        viewModel.tutorialState.observe(this) { part ->
            if (part == 2) loadList()
        }

        viewModel.restoreTreatmentList()
        viewModel.restoreAlarms()
        viewModel.loadDialogs()
        Watch.checkDevices()
    }

    private fun tutorialSequence(part: Int) {
        // Bloqueamos la interfaz de usuario mientras dure el tutorial.
        binding.dialog.visibility = View.GONE
        binding.btnAdd.isEnabled = false
        if (part == 1) {
            viewModel.getTheme()
            viewModel.theme.observe(this) { theme ->
                when (theme) {
                    0 -> scv = ShowcaseView.Builder(this)
                        .setTarget(ViewTarget(binding.btnAdd))
                        .setContentTitle(getString(R.string.tutorial_anadir))
                        .setContentText(getString(R.string.tutorial_anadir_desc))
                        .setStyle(R.style.NightShowcaseTheme)
                        .build()
                    1 -> scv = ShowcaseView.Builder(this)
                        .setTarget(ViewTarget(binding.btnAdd))
                        .setContentTitle(getString(R.string.tutorial_anadir))
                        .setContentText(getString(R.string.tutorial_anadir_desc))
                        .setStyle(R.style.MorningShowcaseTheme)
                        .build()
                    2 -> scv = ShowcaseView.Builder(this)
                        .setTarget(ViewTarget(binding.btnAdd))
                        .setContentTitle(getString(R.string.tutorial_anadir))
                        .setContentText(getString(R.string.tutorial_anadir_desc))
                        .setStyle(R.style.NoonShowcaseTheme)
                        .build()
                    3 -> scv = ShowcaseView.Builder(this)
                        .setTarget(ViewTarget(binding.btnAdd))
                        .setContentTitle(getString(R.string.tutorial_anadir))
                        .setContentText(getString(R.string.tutorial_anadir_desc))
                        .setStyle(R.style.DuskShowcaseTheme)
                        .build()
                }

                scv.overrideButtonClick {
                    val i = Intent(applicationContext, DataFormActivity::class.java)
                    i.putExtra("from", "Tutorial")
                    startActivity(i)
                    finish()
                    overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
                }
                viewModel.completeTutorialSequence(2)
            }
        } else if (part == 2) {
            val recyclerView: RecyclerView = findViewById(R.id.recycler_tratamientos)
            viewModel.getTheme()
            viewModel.theme.observe(this) { theme ->
                when (theme) {
                    0 -> scv2 = ShowcaseView.Builder(this)
                        .setTarget(ViewTarget(recyclerView))
                        .setContentTitle(getString(R.string.tutorial_consultar_trat))
                        .setContentText(getString(R.string.tutorial_consultar_desc))
                        .setStyle(R.style.NightShowcaseTheme)
                        .build()
                    1 -> scv2 = ShowcaseView.Builder(this)
                        .setTarget(ViewTarget(recyclerView))
                        .setContentTitle(getString(R.string.tutorial_consultar_trat))
                        .setContentText(getString(R.string.tutorial_consultar_desc))
                        .setStyle(R.style.MorningShowcaseTheme)
                        .build()
                    2 -> scv2 = ShowcaseView.Builder(this)
                        .setTarget(ViewTarget(recyclerView))
                        .setContentTitle(getString(R.string.tutorial_consultar_trat))
                        .setContentText(getString(R.string.tutorial_consultar_desc))
                        .setStyle(R.style.NoonShowcaseTheme)
                        .build()
                    3 -> scv2 = ShowcaseView.Builder(this)
                        .setTarget(ViewTarget(recyclerView))
                        .setContentTitle(getString(R.string.tutorial_consultar_trat))
                        .setContentText(getString(R.string.tutorial_consultar_desc))
                        .setStyle(R.style.DuskShowcaseTheme)
                        .build()
                }
                scv2.setButtonText(getString(R.string.tutorial_entendido))
                scv2.overrideButtonClick {
                    scv2.hide()
                    binding.dialog.text = ""
                    binding.dialog.setCharacterDelay(35)
                    binding.dialog.animateText(getString(R.string.tutorial_end))

                    // Desbloqueamos la UI.
                    binding.dialog.visibility = View.VISIBLE
                    binding.btnAdd.isEnabled = true
                    recyclerView.isEnabled = true
                    loadClover()
                    showAutoStartDialog()
                }
                viewModel.completeTutorialSequence(0)
            }
        }
    }
}