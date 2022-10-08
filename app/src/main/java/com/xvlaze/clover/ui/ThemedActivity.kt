package com.xvlaze.clover.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.xvlaze.clover.R
import java.time.LocalTime

abstract class ThemedActivity : AppCompatActivity() {
    private lateinit var viewModel: ThemedActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(
            this,
            ThemedActivityViewModel.MyViewModelFactory(application)
        )[ThemedActivityViewModel::class.java]
        calculateWeather()
    }

    override fun onResume() {
        super.onResume()
        calculateWeather()
    }

    // No se puede hacer en el ViewModel. Por alguna razón que desconozco no puedo establecer un tema desde un observe.
    private fun calculateWeather() {
        val horaActual = LocalTime.now().hour
        val themeNumber: Int
        val theme: Int
        when {
            horaActual > 20 || horaActual < 7 -> {
                // 0 - Night
                themeNumber = 0
                theme = R.style.Night
            }
            horaActual < 10 -> {
                // 1 - Dawn
                themeNumber = 1
                theme = R.style.Dawn
            }
            horaActual < 18 -> {
                // 2 - Noon
                themeNumber = 2
                theme = R.style.Noon
            }
            else -> {
                // 3 - Dusk
                themeNumber = 3
                theme = R.style.Dusk
            }
        }
        setTheme(theme)
        viewModel.setTheme(themeNumber)
    }
}