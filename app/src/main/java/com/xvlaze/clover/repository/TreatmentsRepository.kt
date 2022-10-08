package com.xvlaze.clover.repository

import android.content.Context
import android.net.Uri
import com.xvlaze.clover.model.AlarmPool
import com.xvlaze.clover.model.MLProvider
import com.xvlaze.clover.model.Treatment
import com.xvlaze.clover.model.TreatmentsSource

class TreatmentsRepository (private val c: Context) {
    fun findTreatmentByName(name: String) = TreatmentsSource.findTreatmentByName(name, c)
    fun saveTreatment(t: Treatment) = TreatmentsSource.addTreatment(c, t)
    fun deleteTreatment(t: Treatment) = TreatmentsSource.deleteTreatment(c, t)
    fun restoreTreatmentList(): ArrayList<Treatment> {
        TreatmentsSource.checkStock(c)
        return TreatmentsSource.readTreatmentsFromJson(c)
    }
    fun restoreAlarms() {
        TreatmentsSource.restoreAlarms(c)
        AlarmsRepository().syncAndSet(restoreTreatmentList())
    }
    fun consumeTreatment(t: Treatment): Int = TreatmentsSource.consume(c, t)
    fun findTreatmentUpNext(name: String): String = AlarmPool.instance.findTreatmentsUpNext(name).second
    fun scanTreatment(imageUri: Uri, callback: MLProvider.IMLRecognitionCallback) = MLProvider.readTreatmentPackage(imageUri, callback)
}