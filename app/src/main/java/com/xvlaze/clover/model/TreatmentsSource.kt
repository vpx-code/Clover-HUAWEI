package com.xvlaze.clover.model

import android.content.Context
import com.xvlaze.clover.BuildConfig
import com.xvlaze.clover.services.Reminder
import com.xvlaze.clover.util.Constants.TAG
import com.xvlaze.clover.util.MyApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException

object TreatmentsSource {
    private val format = Json { prettyPrint = true }

    /**
     * Reads saved treatments from a JSON file.
     * @param c: Context
     * @return An ArrayList<Treatment>, if any, or an empty ArrayList.
     */
    fun readTreatmentsFromJson(c: Context): ArrayList<Treatment> {
        if (BuildConfig.DEBUG)
            Timber.tag(TAG + " ${this.javaClass.simpleName}")
        Timber.d("Reading treatment data from JSON...")
        return try {
            format.decodeFromString(File(c.filesDir.toString() + "/config.json").readText(Charsets.UTF_8))
        } catch (ex: FileNotFoundException) {
            arrayListOf()
        }
    }

    fun restoreAlarms(c: Context) = AlarmPool.instance.readAlarmsFromJson(c)

    fun addTreatment(c: Context, t: Treatment) {
        val updatedList = readTreatmentsFromJson(c)

        // Reemplaza la dosis si la estamos editando.
        val treatmentToEdit = updatedList.find { it.mNombre == t.mNombre }
        if (treatmentToEdit != null) {
            updatedList.remove(treatmentToEdit)
            AlarmPool.instance.remove(treatmentToEdit)
        }

        // Añadimos el tratamiento a la lista y reprogramamos las alarmas.
        updatedList.add(t)
        AlarmPool.instance.sync(updatedList)
        AlarmPool.instance.setNotification()

        /* Ahora que conocemos la fecha de alarma próxima del tratamiento, lo actualizamos en la lista y escribimos el JSON.
            Lo hacemos así porque el método sync ha de usarse en muchos otros casos y no en todos hay que modificar la lista.
         */
        updateUpNext(t, updatedList)

        updatedList.sortBy {
            it.upNextLong
        }
        writeToJson(c, updatedList)
    }

    private fun updateUpNext(
        t: Treatment,
        updatedList: ArrayList<Treatment>
    ) {
        val upNext = AlarmPool.instance.findTreatmentsUpNext(t.mNombre)
        val treatmentInList = updatedList.find { it.mNombre == t.mNombre }
        treatmentInList!!.upNextLong = upNext.first
        treatmentInList.upNext = upNext.second
    }

    fun deleteTreatment(c: Context, t: Treatment) {
        val treatmentList = readTreatmentsFromJson(c)
        AlarmPool.instance.remove(t)
        treatmentList.removeIf { it.mNombre == t.mNombre }
        writeToJson(c, treatmentList)
        AlarmPool.instance.sync(treatmentList)
        AlarmPool.instance.setNotification()
    }

    fun findTreatmentByName(name: String?, context: Context): Treatment? {
        return readTreatmentsFromJson(context).find { name.equals(it.mNombre) }
    }

    fun consume(c: Context, t: Treatment): Int {
        val treatmentList = readTreatmentsFromJson(c)
        val treatmentInList = treatmentList.find { it.mNombre == t.mNombre } ?: return -1
        treatmentInList.mTreatmentRestantes = treatmentInList.mTreatmentRestantes.minus(1)
        AlarmPool.instance.sync(treatmentList)
        AlarmPool.instance.setNotification()
        updateUpNext(treatmentInList, treatmentList)
        writeToJson(c, treatmentList)
        return treatmentInList.mTreatmentRestantes
    }

    private fun writeToJson(c: Context, list: ArrayList<Treatment>) {
        /*
        FIXME!! f != list!
         comparar posiciones de memoria y, si no, cómo se envía este JSON tras consumir un medicamento.
         Quizá la clave esté en la asignación esa rara de restantes. Mirar especificación de data classes.

         ACTUALIZACION 4/05/2022 No parece que funcione mal, la verdad.
        */
        File(c.filesDir.toString() + "/config.json").writeText(format.encodeToString(list))
    }

    /**
     * Controls whether treatments are running out or not and throws notifications if so.
     * @param c: Context
     */
    fun checkStock(c: Context) {
        if (BuildConfig.DEBUG) Timber.tag(TAG).d("Checking stock...")
        val mNotificationManager = MyApplication.getApp()?.notificationManager
        for (d in readTreatmentsFromJson(c)) {
            if (AlarmPool.instance.isNotificationInside(d.mId)) {
                mNotificationManager?.cancel(d.mId)
                AlarmPool.instance.popNotification(d.mNombre, d.mId)
            }

            if (d.isRunningOut) {
                // Si no tenemos la notificación activa ya, la creamos.
                if (!AlarmPool.instance.isNotificationInside(d.mId)) {
                    /*val notificationServiceIntent = Intent(c, NotificationService::class.java)
                    notificationServiceIntent.putExtra("numeroDosis", d.mPastillasPorTreatment)
                    notificationServiceIntent.putExtra("nombreTratamiento", d.mNombre)
                    notificationServiceIntent.putExtra("cantidadPaquete", d.mBlister)
                    notificationServiceIntent.putExtra("remaining", d.mTreatmentRestantes)
                    notificationServiceIntent.putExtra("alarmId", d.mId)*/
                    Reminder.throwStock(c,
                        d.mPastillasPorTreatment,
                        d.mNombre,
                        d.mTreatmentRestantes,
                        d.mId,
                        d.mBlister)
                }
            }
        }
    }
}