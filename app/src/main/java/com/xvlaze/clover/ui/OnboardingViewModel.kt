package com.xvlaze.clover.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xvlaze.clover.repository.SharedPrefsRepository

class OnboardingViewModel (val app: Application): AndroidViewModel(app) {
    private val repository = SharedPrefsRepository(app.applicationContext)
    val isComplete = MutableLiveData<Boolean>()

    fun setCompleteFlag() {
        repository.completeTutorial(1)
        isComplete.postValue(true)
    }

    fun saveUsername(name: String) = repository.saveUsername(name)

    class MyViewModelFactory(val app: Application): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                OnboardingViewModel(app) as T
            } else {
                throw IllegalArgumentException("ViewModel Not Found")
            }
        }
    }
}