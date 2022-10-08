package com.xvlaze.clover.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xvlaze.clover.model.Treatment
import com.xvlaze.clover.repository.TreatmentsRepository
import com.xvlaze.clover.util.MyApplication

class RestockViewModel (val app: Application): AndroidViewModel(app) {
    private val repository = TreatmentsRepository(app.applicationContext)
    val treatment = MutableLiveData<Treatment>()

    fun findTreatmentByName(name: String) {
        treatment.postValue(repository.findTreatmentByName(name))
    }

    fun restock(t: Treatment?) {
        repository.deleteTreatment(t!!)
        t.mTreatmentRestantes = t.mBlister
        repository.saveTreatment(t)
    }

    fun cancelAlarm(id: Int) = MyApplication.getApp()!!.notificationManager.cancel(id)

    class MyViewModelFactory(val app: Application): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(RestockViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                RestockViewModel(app) as T
            } else {
                throw IllegalArgumentException("ViewModel Not Found")
            }
        }
    }
}