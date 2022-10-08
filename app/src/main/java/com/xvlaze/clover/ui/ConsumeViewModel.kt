package com.xvlaze.clover.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xvlaze.clover.model.Treatment
import com.xvlaze.clover.repository.TreatmentsRepository
import com.xvlaze.clover.watch.Watch

class ConsumeViewModel (val app: Application): AndroidViewModel(app) {
    private val repository = TreatmentsRepository(app.applicationContext)
    var foundTreatment = MutableLiveData<Treatment>()
    var remainingIntakes = MutableLiveData<Int>()

    fun findTreatment(nombreTratamiento: String?) {
        foundTreatment.postValue(repository.findTreatmentByName(nombreTratamiento!!))
    }

    fun consume(d: Treatment) {
        Watch.killConsumeDialog(d.mNombre)
        remainingIntakes.postValue(repository.consumeTreatment(d))
    }

    class MyViewModelFactory(val app: Application): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(ConsumeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                ConsumeViewModel(app) as T
            } else {
                throw IllegalArgumentException("ViewModel Not Found")
            }
        }
    }
}