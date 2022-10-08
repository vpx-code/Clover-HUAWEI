package com.xvlaze.clover.model

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.xvlaze.clover.R

class SharedPrefsModel(c: Context) {
    private val sharedPrefs = c.getSharedPreferences(
        c.getString(R.string.sharedprefs),
        FragmentActivity.MODE_PRIVATE
    )
    private val editor = sharedPrefs.edit()

    fun setTheme(theme: Int) {
        editor.putInt("theme", theme)
        editor.apply()
    }

    fun getTheme(): Int {
        return sharedPrefs.getInt("theme", 2)
    }

    fun saveUsername(name: String) {
        editor.putString("name", name)
        editor.apply()
    }

    fun getTutorialState(): List<Boolean> = listOf(
        sharedPrefs.getBoolean("isFirstTime", true),
        sharedPrefs.getBoolean("tutorial1", false),
        sharedPrefs.getBoolean("tutorial2", false)
    )

    fun completeTutorial(part: Int) {
        when (part) {
            0 -> editor.putBoolean("isFirstTime", false)
            1 -> editor.putBoolean("tutorial1", true)
            2 -> editor.putBoolean("tutorial2", true)
        }
        editor.apply()
    }

    fun isSnackbarAlreadyShown(): Boolean =
        sharedPrefs.getBoolean("isSnackbarAlreadyShown", false)

    fun setAutostartDialogAlreadyShown() {
        editor.putBoolean("isSnackbarAlreadyShown", true)
        editor.apply()
    }
}