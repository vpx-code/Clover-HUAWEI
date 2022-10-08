package com.xvlaze.clover.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xvlaze.clover.repository.SharedPrefsRepository

class ThemedActivityViewModel (val app: Application): AndroidViewModel(app) {
    private val repository = SharedPrefsRepository(app.applicationContext)
    val theme = MutableLiveData<Int>()

    fun setTheme(theme: Int) = repository.setTheme(theme)

    class MyViewModelFactory(val app: Application): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(ThemedActivityViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                ThemedActivityViewModel(app) as T
            } else {
                throw IllegalArgumentException("ViewModel Not Found")
            }
        }
    }
}