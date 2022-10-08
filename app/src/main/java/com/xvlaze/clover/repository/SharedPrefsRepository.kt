package com.xvlaze.clover.repository

import android.content.Context
import com.xvlaze.clover.model.SharedPrefsModel

class SharedPrefsRepository(c: Context)
{
    private var model: SharedPrefsModel = SharedPrefsModel(c)
    fun setTheme(theme: Int) = model.setTheme(theme)
    fun getTheme() = model.getTheme()
    fun saveUsername(name: String) = model.saveUsername(name)
    fun getTutorialFlags(): List<Boolean> = model.getTutorialState()
    fun completeTutorial(part: Int) = model.completeTutorial(part)
    fun isSnackbarAlreadyShown(): Boolean = model.isSnackbarAlreadyShown()
    fun setAutostartDialogAlreadyShown() = model.setAutostartDialogAlreadyShown()
}