package com.xvlaze.clover.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xvlaze.clover.R
import com.xvlaze.clover.model.Treatment
import com.xvlaze.clover.repository.SharedPrefsRepository
import com.xvlaze.clover.repository.TreatmentsRepository

class MainViewModel(val app: Application) : AndroidViewModel(app) {

    private val treatmentsRepository = TreatmentsRepository(app.applicationContext)
    private val sharedPrefsRepository = SharedPrefsRepository(app.applicationContext)

    val treatmentsList = MutableLiveData<ArrayList<Treatment>>()
    var treatmentUpNext = MutableLiveData<String>()
    var currentDialog = MutableLiveData<String>()
    val tutorialState = MutableLiveData<Int>()
    val hasSnackbarBeenShownOnce = MutableLiveData<Boolean>()
    val theme = MutableLiveData<Int>()

    fun delete(position: Int) {
        // Bastante mierda pero no parece haber otra solución a priori.
        treatmentsList.postValue(treatmentsList.value?.filter {
            it.mNombre != treatmentsList.value!![position].mNombre
        } as ArrayList<Treatment>?)

        if (treatmentsList.value?.isNotEmpty()!!) {
            treatmentsRepository.deleteTreatment(treatmentsList.value!![position])
        }
    }

    fun loadDialogs() {
        val dialogsDawn: Array<String> =
            app.applicationContext.resources.getStringArray(R.array.main_dialogs_dawn)
        val dialogsNoon: Array<String> =
            app.applicationContext.resources.getStringArray(R.array.main_dialogs_noon)
        val dialogsDusk: Array<String> =
            app.applicationContext.resources.getStringArray(R.array.main_dialogs_dusk)
        val dialogsNight: Array<String> =
            app.applicationContext.resources.getStringArray(R.array.main_dialogs_night)

        val dialogNumber = (Math.random() * dialogsNoon.size).toInt()

        when (sharedPrefsRepository.getTheme()) {
            0 -> currentDialog.postValue(dialogsNight[dialogNumber])
            1 -> currentDialog.postValue(dialogsDawn[dialogNumber])
            3 -> currentDialog.postValue(dialogsDusk[dialogNumber])
            else -> currentDialog.postValue(dialogsNoon[dialogNumber])
        }
    }

    fun restoreTreatmentList() =
        treatmentsList.postValue(treatmentsRepository.restoreTreatmentList())

    fun restoreAlarms() = treatmentsRepository.restoreAlarms()

    fun findTreatmentsUpNext(name: String) =
        treatmentUpNext.postValue(treatmentsRepository.findTreatmentUpNext(name))

    fun isFirstTime() {
        val flags = sharedPrefsRepository.getTutorialFlags()
        val isOnboardingChecked = flags[0]
        val isTutorial1Checked = flags[1]
        val isTutorial2Checked = flags[2]
        tutorialState.postValue(
            when {
                isOnboardingChecked && !isTutorial1Checked && !isTutorial2Checked -> {
                    0
                }
                isOnboardingChecked && isTutorial1Checked && !isTutorial2Checked -> {
                    1
                }
                isOnboardingChecked && isTutorial1Checked && isTutorial2Checked -> {
                    2
                }
                else -> {
                    3
                }
            }
        )
    }

    fun getTheme() = theme.postValue(sharedPrefsRepository.getTheme())

    fun completeTutorialSequence(part: Int) = sharedPrefsRepository.completeTutorial(part)

    fun checkSnackbarStatus() = hasSnackbarBeenShownOnce.postValue(sharedPrefsRepository.isSnackbarAlreadyShown())

    fun setAutostartDialogAlreadyShown() = sharedPrefsRepository.setAutostartDialogAlreadyShown()

    class MyViewModelFactory(val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                MainViewModel(app) as T
            } else {
                throw IllegalArgumentException("ViewModel Not Found")
            }
        }
    }
}