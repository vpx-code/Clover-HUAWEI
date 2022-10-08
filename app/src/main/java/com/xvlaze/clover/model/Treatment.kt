package com.xvlaze.clover.model

import android.content.Context
import com.xvlaze.clover.R
import kotlinx.serialization.Serializable

@Serializable
data class Treatment(
    val mNombre: String,
    var mBlister: Int,
    var fechaInicio: Long,
    var mHoras: ArrayList<String>?,
    var mPastillasPorTreatment: Int,
    var mFreqNum: Int,
    var mFreqTime: Int,
    var mFreqDias: BooleanArray,
    var mTreatmentRestantes: Int,
    var upNext: String
) {
    var upNextLong: Long = 0
    var mId = mNombre.hashCode() // Usado para las notificaciones de stock.
    var isRunningOut = mTreatmentRestantes <= 3

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Treatment

        if (mNombre != other.mNombre) return false
        if (mBlister != other.mBlister) return false
        if (fechaInicio != other.fechaInicio) return false
        if (mHoras != other.mHoras) return false
        if (mPastillasPorTreatment != other.mPastillasPorTreatment) return false
        if (mFreqNum != other.mFreqNum) return false
        if (mFreqTime != other.mFreqTime) return false
        if (!mFreqDias.contentEquals(other.mFreqDias)) return false
        if (mId != other.mId) return false
        if (mTreatmentRestantes != other.mTreatmentRestantes) return false
        if (upNext != other.upNext) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mNombre.hashCode()
        result = 31 * result + mBlister
        result = 31 * result + fechaInicio.hashCode()
        result = 31 * result + (mHoras?.hashCode() ?: 0)
        result = 31 * result + mPastillasPorTreatment
        result = 31 * result + mFreqNum
        result = 31 * result + mFreqTime
        result = 31 * result + mFreqDias.contentHashCode()
        result = 31 * result + mId
        result = 31 * result + mTreatmentRestantes
        result = upNext.hashCode()
        return result
    }

    fun prettifyFrequency(context: Context): String {
        // Construimos un String que informe al usuario de la frecuencia de sus dosis.
        val sb = StringBuilder(context.getString(R.string.cada))
        val res: String = when {
            mFreqDias.all { true } -> {
                // Cada día a las XX:XX, YY:YY...
                sb.append(context.getString(R.string.dia))
                concatHoras(sb)
            }
            mFreqDias.all { false } -> {
                // Cada X días/semanas a las XX:XX, YY:YY...
                sb.append(mFreqNum)
                if (mFreqTime == 0) {
                    // Días
                    sb.append(context.getString(if (mFreqNum > 1) R.string.dias else R.string.dia_espacio))
                } else {
                    // Semanas
                    sb.append(context.getString(if (mFreqNum > 1) R.string.semanas else R.string.semana))
                }
                concatHoras(sb)
            }
            else -> {
                // L/M/X/J/V/S/D... a las XX:XX, YY:YY...
                for (i in mFreqDias.indices) {
                    if (mFreqDias[i]) {
                        when (i) {
                            1 -> if (mFreqDias[i]) sb.append(context.getString(R.string.lunes))
                            2 -> if (mFreqDias[i]) sb.append(context.getString(R.string.martes))
                            3 -> if (mFreqDias[i]) sb.append(context.getString(R.string.miercoles))
                            4 -> if (mFreqDias[i]) sb.append(context.getString(R.string.jueves))
                            5 -> if (mFreqDias[i]) sb.append(context.getString(R.string.viernes))
                            6 -> if (mFreqDias[i]) sb.append(context.getString(R.string.sabado))
                        }

                        // FIXME: Arreglar en la próxima versión.
                        if (i != 0 && i < mFreqDias.size - 1) sb.append(", ")
                    }
                }

                // La lista tiene el domingo en la posición 0, pero nosotros lo queremos poner al final al mostrar los días de tratamiento al usuario.
                if (mFreqDias[0]) sb.append(context.getString(R.string.domingo))
                sb.append(context.getString(R.string.a_las))
                concatHoras(sb)
            }
        }
        return res
    }

    private fun concatHoras(sb: StringBuilder): String {
        for (i in mHoras!!.indices) {
            sb.append(mHoras!![i])
            if (i != mHoras!!.size - 1 && mHoras!!.size != 1) {
                sb.append(", ")
            }
        }
        sb.append(".")
        return sb.toString()
    }
}