package com.xvlaze.clover.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.xvlaze.clover.BuildConfig
import com.xvlaze.clover.R
import com.xvlaze.clover.databinding.DialogConsumeBinding
import com.xvlaze.clover.model.AlarmPool
import com.xvlaze.clover.model.Treatment
import com.xvlaze.clover.util.Constants
import com.xvlaze.clover.util.MyApplication
import kotlin.math.floor
import kotlin.random.Random

class ConsumeDialog : AppCompatActivity() {
    private lateinit var viewModel: ConsumeViewModel
    private lateinit var binding: DialogConsumeBinding
    private lateinit var d: Treatment
    private var alarmId = 0
    private var nombreTratamiento: String? = null
    private var notificationManager = MyApplication.getApp()!!.notificationManager
    private var context: Context? = null
    private var isButtonPressedTwice = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogConsumeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        setFinishOnTouchOutside(false)

        viewModel = ViewModelProvider(
            this,
            ConsumeViewModel.MyViewModelFactory(application)
        )[ConsumeViewModel::class.java]

        context = this@ConsumeDialog

        /*AdsProvider.instance
            .loadInterstitialAdCallback(R.string.i_take, object : FullScreenContentCallback() {
                fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    consumeRoutine()
                }
            })*/
        alarmId = intent.getIntExtra("alarmId", -1)
        nombreTratamiento = intent.getStringExtra("nombreTratamiento")

        viewModel.findTreatment(nombreTratamiento)
        viewModel.foundTreatment.observe(this) {
            d = it
            val nombre: TextView = binding.nombreTratamiento
            nombre.text = nombreTratamiento

            // TODO: Implementar tipo de tratamiento: jarabe...
            val cantidadTreatment: Int = d.mPastillasPorTreatment
            val cantidad: TextView = binding.cantidadTratamiento
            if (cantidadTreatment.toDouble() == floor(cantidadTreatment.toDouble()) &&
                !java.lang.Double.isInfinite(cantidadTreatment.toDouble())
            ) {
                cantidad.text = (context as ConsumeDialog).resources.getQuantityString(
                    R.plurals.qty,
                    cantidadTreatment,
                    cantidadTreatment
                )
            } else {
                cantidad.text = (context as ConsumeDialog).resources.getQuantityString(
                    R.plurals.qty,
                    cantidadTreatment,
                    cantidadTreatment
                )
            }

            val tomar: Button = binding.tomarBtn
            tomar.setOnTouchListener(object : View.OnTouchListener {
                private val gestureDetector: GestureDetector =
                    GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                        override fun onDoubleTap(e: MotionEvent): Boolean {
                            isButtonPressedTwice = true
                            /*if (!getPackageName().endsWith(".pro")) {
                                if (AdsProvider.instance
                                        .showInterstitialAd(R.string.i_take, this@ConsumeDialog)
                                ) {
                                    consumeRoutine()
                                }
                            } else {

                            }*/
                            consumeRoutine()
                            return super.onDoubleTap(e)
                        }

                        override fun onSingleTapUp(e: MotionEvent): Boolean {
                            if (BuildConfig.DEBUG) {
                                Toast.makeText(context, "SINGLE TAP", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            // TODO: ¿Cambiar color?
                            return super.onSingleTapUp(e)
                        }
                    })

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    gestureDetector.onTouchEvent(event)
                    return false
                }
            })
        }
    }

    private fun consumeRoutine() {
        if (BuildConfig.DEBUG) {
            Toast.makeText(context, "DOUBLE TAP", Toast.LENGTH_SHORT).show()
        }
        viewModel.consume(d)
        viewModel.remainingIntakes.observe(this) { remainingIntakes ->
            if (remainingIntakes <= 3) {
                // TODO: Falta el binding aquí.
                val dialog = Dialog(this@ConsumeDialog, R.style.CustomAlertDialog)
                dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog.setCancelable(false)
                dialog.setContentView(R.layout.dialog_dosis)

                val alertHead: TextView = dialog.findViewById(R.id.alert_head)
                alertHead.text = when (remainingIntakes) {
                    0 -> getString(
                        R.string.ran_out,
                        d.mNombre
                    )
                    else -> getString(R.string.nearly_ran_out, d.mNombre)
                }

                val btnReponer = dialog.findViewById<Button>(R.id.btnReponer)
                btnReponer.setOnClickListener {
                    dialog.dismiss()
                    val i = Intent(context, FarmaciaDialog::class.java)
                    i.putExtra("nombreTratamiento", nombreTratamiento)
                    i.putExtra("cantidadPaquete", intent.getIntExtra("cantidadPaquete", 0))
                    i.putExtra("alarmId", alarmId)
                    startActivity(i)
                    finish()
                }
                val btnOmitir = dialog.findViewById<Button>(R.id.btnOmitir)
                btnOmitir.setOnClickListener {
                    dialog.dismiss()
                    val dialogOmitir = Dialog(this@ConsumeDialog, R.style.CustomAlertDialog)
                    dialogOmitir.window?.requestFeature(Window.FEATURE_NO_TITLE) // if you have blue line on top of your dialog, you need use this code
                    dialogOmitir.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    dialogOmitir.setCancelable(false)
                    dialogOmitir.setContentView(R.layout.dialog_omitir_dosis)

                    val btnOmitirReponer = dialogOmitir.findViewById<Button>(R.id.btnReponer)
                    btnOmitirReponer.setOnClickListener {
                        dialogOmitir.dismiss()
                        dialog.show() // TODO: ¿Es esta la mejor experiencia de usuario? Igual mejor lanzar el intent a RestockDialog...
                    }
                    val btnOmitirOmitir = dialogOmitir.findViewById<Button>(R.id.btnOmitir)
                    btnOmitirOmitir.setOnClickListener {
                        val i = Intent(applicationContext, MainActivity::class.java)
                        startActivity(i)
                        finish()
                    }
                    dialogOmitir.show()
                }
                dialog.show()
                // TODO: Ahora, haremos un for por las dosis (ver checkStock() y preguntar uno por uno si queremos rehacer el tratamiento.)
            } else {
                d.let { notificationManager.cancel(it.mId) } // Elimina notificaciones de stock.
                notificationManager.cancel(alarmId)
                try {
                    // Sacamos las notificaciones de stock y otras que hayan podido quedar de la pool para que no se vuelven a llamar.
                    nombreTratamiento?.let { AlarmPool.instance.popNotification(it) }
                } catch (ignored: NullPointerException) {
                }

                finish()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && !isButtonPressedTwice) throwReminder()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        throwReminder()
    }

    private fun throwReminder() {
        d.mId.let { notificationManager.cancel(it) }
        val remoteViews = RemoteViews(context!!.packageName, R.layout.not_time)
        remoteViews.setTextViewText(R.id.title, getString(R.string.dont_forget))
        remoteViews.setTextViewText(R.id.subtitle, getString(R.string.dont_forget_sub))
        val notifyIntent = Intent(context, ConsumeDialog::class.java)
        notifyIntent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK
                or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        notifyIntent.putExtra("alarmId", alarmId)
        notifyIntent.putExtra("nombreTratamiento", nombreTratamiento)
        notifyIntent.putExtra("cantidadTreatment", intent.getIntExtra("cantidadTreatment", -1))
        notifyIntent.putExtra("cantidadPaquete", intent.getIntExtra("cantidadPaquete", -1))
        val notifyPendingIntent: PendingIntent =
            PendingIntent.getActivity(context, Random.nextInt(), notifyIntent,
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    }
                    else -> PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
        val builder: Notification.Builder =
            Notification.Builder(this@ConsumeDialog, Constants.PRIMARY_CHANNEL_ID)
                .setSmallIcon(R.drawable.idle_1)
                .setCustomContentView(remoteViews)
                .setContentIntent(notifyPendingIntent)
                .setOngoing(true)
        notificationManager.notify(alarmId, builder.build())
    }
}