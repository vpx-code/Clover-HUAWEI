package com.xvlaze.clover.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.xvlaze.clover.R
import com.xvlaze.clover.databinding.DialogFarmaciaBinding

class FarmaciaDialog : AppCompatActivity() {
    private lateinit var binding: DialogFarmaciaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogFarmaciaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        // FIXME: Ojo que no va
        val btnFarmacia = findViewById<Button>(R.id.btn_buscar)
        btnFarmacia.setOnClickListener {
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
                            i.putExtra(
                                "nombreTratamiento",
                                intent.getStringExtra("nombreTratamiento")
                            )
                            i.putExtra(
                                "cantidadDosis",
                                intent.getIntExtra("cantidadDosis", 0)
                            )
                            i.putExtra(
                                "cantidadPaquete",
                                intent.getIntExtra("cantidadPaquete", 0)
                            )
                            startActivity(i)
                            finish()
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            showSettingsDialog()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<com.karumi.dexter.listener.PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        token?.continuePermissionRequest()
                    }
                }).check()
        }

        val btnMasTarde = findViewById<Button>(R.id.btn_later)
        btnMasTarde.setOnClickListener {
            val i = Intent(applicationContext, RestockDialog::class.java)
            i.putExtra("nombreTratamiento", intent.getStringExtra("nombreTratamiento"))
            i.putExtra("alarmId", intent.getIntExtra("alarmId", -1))
            startActivity(i)
            finish()
        }
    }

    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        val viewGroup = findViewById<ViewGroup>(R.id.content)
        val dialogView: View =
            LayoutInflater.from(this).inflate(R.layout.dialog_permissions, viewGroup, false)
        builder.setView(dialogView)
        val dialogConfirm = builder.create()
        val permissionReq = dialogView.findViewById<TextView>(R.id.permission_req)
        permissionReq.setText(R.string.location_permission)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            dialogConfirm.cancel()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivityForResult(intent, 101) // FIXME
        }
        val btnDismiss = dialogView.findViewById<Button>(R.id.btnDismiss)
        btnDismiss.setOnClickListener { dialogConfirm.cancel() }
        dialogConfirm.show()
    }
}