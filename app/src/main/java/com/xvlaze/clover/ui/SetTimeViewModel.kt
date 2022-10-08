package com.xvlaze.clover.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xvlaze.clover.model.Treatment
import com.xvlaze.clover.repository.TreatmentsRepository

class SetTimeViewModel(val app: Application): AndroidViewModel(app) {

    private val repository = TreatmentsRepository(app.applicationContext)
    var isEditMode = MutableLiveData<Boolean>()
    val timesList = MutableLiveData<ArrayList<String>>()

    init {
        timesList.postValue(arrayListOf())
    }

    fun addTime(time: String) {
        // TODO: Algún snackbar  alg así.
        if (!timesList.value?.contains(time)!!) timesList.postValue((timesList.value?.plus(time) as ArrayList<String>?)?.apply { sort() })
    }

    fun deleteTime(time: String) {
        timesList.postValue((timesList.value?.minus(time) as ArrayList<String>?)?.apply { sort() })
    }

    fun saveTreatment(t: Treatment) = repository.saveTreatment(t)

    fun areWeEditing(name: String?) {
        isEditMode.postValue(name?.isNotEmpty() ?: false)
    }

    fun restoreTimes(editedTreatmentName: String) {
        timesList.postValue(repository.findTreatmentByName(editedTreatmentName)?.mHoras ?: arrayListOf())
    }

    class MyViewModelFactory(val app: Application): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(SetTimeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                SetTimeViewModel(app) as T
            } else {
                throw IllegalArgumentException("ViewModel Not Found")
            }
        }
    }
}
