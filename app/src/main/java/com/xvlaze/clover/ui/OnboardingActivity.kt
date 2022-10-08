package com.xvlaze.clover.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.lifecycle.ViewModelProvider
import com.xvlaze.clover.R
import com.xvlaze.clover.databinding.ActivityOnboardingBinding
import com.xvlaze.clover.util.TypeWriter
import spencerstudios.com.bungeelib.Bungee

class OnboardingActivity : ThemedActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var viewModel: OnboardingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(
            this,
            OnboardingViewModel.MyViewModelFactory(application)
        )[OnboardingViewModel::class.java]

        // Carga el sprite.
        val cloverSprite: ImageView = findViewById(R.id.imageView)
        cloverSprite.setBackgroundResource(R.drawable.anim_walk)

        // Carga el cuadro de diálogo.
        val welcomeDialog = resources.getStringArray(R.array.welcome_dialog)
        val tw: TypeWriter = findViewById(R.id.textView)
        tw.text = ""
        tw.setCharacterDelay(35)
        val i = intArrayOf(0) // Itera sobre las frases.

        tw.animateText(welcomeDialog[i[0]])
        i[0]++

        val nombre = arrayOf("")

        val et_nombre: EditText = findViewById(R.id.editTextTextPersonName)
        et_nombre.visibility = View.INVISIBLE

        val btn: Button = findViewById(R.id.next_btn)
        btn.setOnClickListener { view: View? ->
            btn.visibility = View.INVISIBLE
            // Si aún no hemos recogido el nombre.
            if (et_nombre.text.toString().isEmpty()) {
                tw.text = ""
                tw.animateText(welcomeDialog[i[0]])
                i[0]++
                if (i[0] == welcomeDialog.size - 2) {
                    val handler = Handler()
                    handler.postDelayed({
                        tw.visibility = View.INVISIBLE
                        et_nombre.visibility = View.VISIBLE
                        et_nombre.requestFocus();
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(binding.editTextTextPersonName, InputMethodManager.SHOW_IMPLICIT)
                    }, 5000)
                } else {
                    btn.visibility = View.VISIBLE
                }
            } else {
                // Iniciamos la secuencia para salir de la actividad y enseñar al usuario el menú principal en la siguiente.
                nombre[0] = et_nombre.text.toString()
                et_nombre.visibility = View.INVISIBLE
                tw.visibility = View.VISIBLE
                tw.animateText(
                    welcomeDialog[i[0]] + nombre[0] + welcomeDialog[i[0] + 1]
                )
                val handler = Handler()
                handler.postDelayed({
                    viewModel.saveUsername(nombre[0])
                    val displayMetrics = DisplayMetrics()
                    windowManager.defaultDisplay.getMetrics(displayMetrics)
                    cloverSprite.animate()
                        .setDuration(3000)
                        .translationX((displayMetrics.widthPixels / 1.25).toFloat())
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                super.onAnimationEnd(animation)
                                viewModel.setCompleteFlag()
                                viewModel.isComplete.observe(this@OnboardingActivity) {
                                    if (it) {
                                        val i1 = Intent(applicationContext, MainActivity::class.java)
                                        startActivity(i1)
                                        finish()
                                        Bungee.fade(this@OnboardingActivity)
                                    }
                                }
                            }
                        })
                }, 3500)
            }
        }

        et_nombre.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString().trim { it <= ' ' }.isEmpty()) {
                    btn.visibility = View.INVISIBLE
                } else {
                    btn.visibility = View.VISIBLE
                }
            }

            override fun afterTextChanged(editable: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        })

        // Get the background, which has been compiled to an AnimationDrawable object.

        // Get the background, which has been compiled to an AnimationDrawable object.
        val frameAnimation = cloverSprite.background as AnimationDrawable

        // Start the animation (looped playback by default).

        // Start the animation (looped playback by default).
        frameAnimation.start()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}