package com.xvlaze.clover.model

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Pair
import com.xvlaze.clover.BuildConfig
import com.xvlaze.clover.util.Constants.TAG
import com.xvlaze.clover.util.Extensions.trimMinute
import com.xvlaze.clover.util.MyApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.*

// FIXME: si suena un (o dos) alarmas cuando la app está cerrada, al volver a entrar se resetea y desaparecen las notificaciones.
class AlarmPool private constructor() {
    private var orderedDates: ArrayList<Alarm> =
        arrayListOf() // No puedo usar ArrayDeque (= stack) porque el JSON no sabe codificarlo a String.
    private var dosisList: List<Treatment>? = null
    private var alarmIntent: Intent? = null
    private var alarmManager: AlarmManager? = null
    private val format: Json = Json {
        prettyPrint = true
    }

    /**
     * Contiene los IDs de las notificaciones activas en cada momento.
     * Se utiliza también para controlar que, si un usuario edita/elimina un tratamiento de la lista,
     * todas sus notificaciones activas (de stock o no) se eliminen.
     *
     * Cada alarma tiene su propio ID, que se reprograma cada vez que se entra a la app de cualquier modo. Al entrar, se ejecuta AlarmPool.restore()
     * y se reprograman todas las alarmas de cara al futuro. Si una alarma al haber entrado tenía notificaciones activas, esas se pierden para siempre y no habrá manera
     * de eliminarlas si borramos el tratamiento. Si las intentamos tocar, romperán la app.
     *
     * Es necesario mantener un estado de persistencia de esas notificaciones "pasadas" para el sistema pero activas en nuestro teléfono, y eso se hace con esta lista.
     *
     * Integer 1: El primer int es el ID de la Treatment de la cual se está generando una notificación, que es el nombre hasheado.
     * Integer 2: El segundo es el ID de la alarma en sí.
     */
    private val mNotificationPool = ArrayList<Pair<Int, Int>>()


    // FIXME: Este método siempre se usa antes de setNotification. Plantear fusio´n?
    /**
     * Given an array of Treatment, updates the alarm pool in order to set reminders for the list's elements, if needed.
     * @param treatmentList The Treatment list for whose alarms are to be set.
     */
    fun sync(treatmentList: ArrayList<Treatment>) {
        val context = MyApplication.getApp()?.applicationContext!!
        dosisList = treatmentList

        if (BuildConfig.DEBUG) Timber.tag(TAG).d("Syncing: ${dosisList.toString()}")

        if (alarmManager == null) alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (alarmIntent == null) alarmIntent = Intent(context, AlarmReceiver::class.java)
        crearCola()
    }

    /**
     * Unsets all existing alarms -if any- and creates a new alarm queue out of a previously saved treatment list.
     */
    fun crearCola() {
        val context = MyApplication.getApp()?.applicationContext!!
        clearAlarmQueue(context)
        generateAlarms()
        writeToJson(context)
    }

    private fun generateAlarms() {
        val formatter: DateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        for (d in dosisList!!) {
            if (BuildConfig.DEBUG) {
                Timber.tag(TAG + " ${this.javaClass.simpleName}")
                Timber.d("Procesando dosis: \"" + d.mNombre + "\"")
            }
            alarmIntent?.putExtra("nombreTratamiento", d.mNombre)
            alarmIntent?.putExtra("cantidadDosis", d.mPastillasPorTreatment)
            alarmIntent?.putExtra("cantidadPaquete", d.mBlister)

            /*
             * Para cada dosis y cada hora...
             */
            for (i in 0 until d.mHoras!!.size) {
                val id = Random().nextInt()
                alarmIntent!!.putExtra("alarmId", id)
                if (BuildConfig.DEBUG) {
                    Timber.tag(TAG + " ${this.javaClass.simpleName}")
                    Timber.d("Procesando hora: \"" + d.mHoras!![i] + "\"")
                }
                val hora = formatter.parse(d.mHoras!![i])
                val fechaInicio = combine(Date(d.fechaInicio), hora!!)
                val fechaProxima = fechaInicio.time
                val ahora = System.currentTimeMillis()

                /*
                Días específicos.
                */

                /*
                 FIXME: Al editar un tratamiento y ponerle días específicos, puede ser que
                  el sistema se vuelva loco porque no sabe gestionar alarmas pasadas. Mirar desencadenante
                  y añadir un caso para las alarmas pasadas.
                */
                when {
                    !d.mFreqDias.all { true } &&
                            !d.mFreqDias.all { false } -> {
                        if (BuildConfig.DEBUG) {
                            Timber.tag(TAG + " ${this.javaClass.simpleName}")
                            Timber.d("Tipo de alarma: días específicos.")
                        }
                        for (j in d.mFreqDias.indices) {
                            if (d.mFreqDias[j]) {
                                var ld = LocalDate.now()
                                when (j) {
                                    0 -> ld = if (fechaProxima < ahora) ld.with(
                                        TemporalAdjusters.next(
                                            DayOfWeek.SUNDAY
                                        )
                                    ) else ld.with(
                                        TemporalAdjusters.nextOrSame(
                                            DayOfWeek.SUNDAY
                                        )
                                    )
                                    1 -> ld = if (fechaProxima < ahora) ld.with(
                                        TemporalAdjusters.next(
                                            DayOfWeek.MONDAY
                                        )
                                    ) else ld.with(
                                        TemporalAdjusters.nextOrSame(
                                            DayOfWeek.MONDAY
                                        )
                                    )
                                    2 -> ld = if (fechaProxima < ahora) ld.with(
                                        TemporalAdjusters.next(
                                            DayOfWeek.TUESDAY
                                        )
                                    ) else ld.with(
                                        TemporalAdjusters.nextOrSame(
                                            DayOfWeek.TUESDAY
                                        )
                                    )
                                    3 -> ld = if (fechaProxima < ahora) ld.with(
                                        TemporalAdjusters.next(
                                            DayOfWeek.WEDNESDAY
                                        )
                                    ) else ld.with(
                                        TemporalAdjusters.nextOrSame(
                                            DayOfWeek.WEDNESDAY
                                        )
                                    )
                                    4 -> ld = if (fechaProxima < ahora) ld.with(
                                        TemporalAdjusters.next(
                                            DayOfWeek.THURSDAY
                                        )
                                    ) else ld.with(
                                        TemporalAdjusters.nextOrSame(
                                            DayOfWeek.THURSDAY
                                        )
                                    )
                                    5 -> ld = if (fechaProxima < ahora) ld.with(
                                        TemporalAdjusters.next(
                                            DayOfWeek.FRIDAY
                                        )
                                    ) else ld.with(
                                        TemporalAdjusters.nextOrSame(
                                            DayOfWeek.FRIDAY
                                        )
                                    )
                                    6 -> ld = if (fechaProxima < ahora) ld.with(
                                        TemporalAdjusters.next(
                                            DayOfWeek.SATURDAY
                                        )
                                    ) else ld.with(
                                        TemporalAdjusters.nextOrSame(
                                            DayOfWeek.SATURDAY
                                        )
                                    )
                                }
                                assert(ld !== LocalDate.now())
                                val fecha = combine(
                                    Date.from(
                                        ld.atStartOfDay(ZoneId.systemDefault()).toInstant()
                                    ), hora
                                )
                                val alarma =
                                    Alarm(
                                        d.mNombre,
                                        fecha.time.trimMinute(),
                                        id,
                                        alarmIntent!!.toUri(0)
                                    )
                                orderedDates.add(alarma)
                            }
                        }
                    }
                    else -> {
                        when {
                            fechaProxima <= ahora -> {
                                val dt = Date()
                                val c = Calendar.getInstance()
                                c.time = dt

                                /*
                                Cada día.
                                */
                                when {
                                    d.mFreqDias.all { true } -> {
                                        if (BuildConfig.DEBUG) {
                                            Timber.tag(TAG + " ${this.javaClass.simpleName}")
                                            Timber.d("Tipo de alarma: cada día.")
                                        }
                                        // FIXME: MENUDA MIERDA!!!
                                        val diaProgramado =
                                            Instant.ofEpochMilli(fechaProxima)
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDate().dayOfMonth
                                        val horaProgramada = Instant.ofEpochMilli(fechaProxima)
                                            .atZone(ZoneId.systemDefault()).hour
                                        val minutoProgramado =
                                            Instant.ofEpochMilli(fechaProxima).atZone(
                                                ZoneId.systemDefault()
                                            ).minute

                                        val diaAhora =
                                            Instant.ofEpochMilli(ahora)
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDate().dayOfMonth
                                        val horaAhora =
                                            Instant.ofEpochMilli(ahora)
                                                .atZone(ZoneId.systemDefault()).hour
                                        val minutoAhora =
                                            Instant.ofEpochMilli(ahora)
                                                .atZone(ZoneId.systemDefault()).minute

                                        // Si el tratamiento empezó antes que hoy...
                                        if (diaAhora > diaProgramado) {
                                            // Movemos la fecha del calendario a hoy para programar la alarma para hoy.
                                            c.time = Date(ahora)
                                            if (horaAhora >= horaProgramada && minutoAhora >= minutoProgramado) {
                                                c.add(Calendar.DATE, 1)
                                            }
                                        } else {
                                            // Si no, programamos la alarma para mañana.
                                            c.add(Calendar.DATE, 1)
                                        }
                                    }
                                    d.mFreqDias.all { false } -> if (d.mFreqTime == 0) {
                                        // Días
                                        if (BuildConfig.DEBUG) {
                                            Timber.tag(TAG + " ${this.javaClass.simpleName}")
                                            Timber.d("Tipo de alarma: cada " + d.mFreqNum.toString() + " días.")
                                        }
                                        c.add(Calendar.DATE, d.mFreqNum)
                                    } else if (d.mFreqTime == 1) {
                                        // Semanas
                                        if (BuildConfig.DEBUG) {
                                            Timber.tag(TAG + " ${this.javaClass.simpleName}")
                                            Timber.d("Tipo de alarma: cada " + d.mFreqNum.toString() + " semanas.")
                                        }
                                        c.add(Calendar.DATE, 7 * d.mFreqNum)
                                    }
                                }
                                if (BuildConfig.DEBUG) {
                                    Timber.tag(TAG + " ${this.javaClass.simpleName}")
                                    Timber.d("Añadiendo a la cola...")
                                }
                                val alarma = Alarm(
                                    d.mNombre,
                                    combine(c.time, hora).time.trimMinute(),
                                    id,
                                    alarmIntent!!.toUri(0)
                                )
                                orderedDates.add(alarma)
                            }
                            /*
                            En el futuro.
                            */
                            else -> {
                                if (BuildConfig.DEBUG) {
                                    Timber.tag(TAG + " ${this.javaClass.simpleName}")
                                    Timber.d("Alarm en el futuro. Añadiendo a la cola...")
                                }
                                val alarma = Alarm(
                                    d.mNombre,
                                    Date(fechaProxima).time.trimMinute(),
                                    id,
                                    alarmIntent!!.toUri(0)
                                )
                                orderedDates.add(alarma)
                            }
                        }
                        if (BuildConfig.DEBUG) {
                            Timber.tag(TAG + " ${this.javaClass.simpleName}")
                            Timber.d("Añadida a la cola.")
                        }
                    }
                }
                pushNotification(d.mNombre, id)
            }
        }
        if (BuildConfig.DEBUG) {
            Timber.tag(TAG + " ${this.javaClass.simpleName}")
            Timber.d("Ordenando cola...")
        }
        orderedDates.sortWith(Comparator.comparing(Alarm::date))
    }

    private fun writeToJson(context: Context) {
        /*
         TODO: No podemos juntar config.json (Treatment) con este JSON nuevo, ya que si no hay alarmas nos quedamos sin registros (el archivo estaría vacío). Pero podríamos tratar de no meter un objeti Treatment
          entero y solo quedarnos con la referencia del nombre para rescatarla cuando haga falta. Eso ahorraría unos bytes de espacio cada vez que se añadiera algo al archivo.
          ACTUALIZACION: Igual sería bueno meter una lista de Alarm en Treatment y operar solo con un archivo
          */
        if (BuildConfig.DEBUG) {
            Timber.tag(TAG + " ${this.javaClass.simpleName}")
            Timber.d("Editando alarms.json...")
        }
        File(context.filesDir.toString() + "/alarms.json").writeText(
            format.encodeToString(
                orderedDates
            )
        )
        if (BuildConfig.DEBUG) {
            Timber.tag(TAG + " ${this.javaClass.simpleName}")
            Timber.d("Cola generada correctamente:")
            orderedDates.forEach { a ->
                Timber.tag(TAG + " ${this.javaClass.simpleName}")
                Timber.d(a.date.toString() + " [" + a.id + "]")
            }
        }
    }

    private fun clearAlarmQueue(context: Context) {
        // Desprograma todas las alarmas y limpia la cola.
        if (BuildConfig.DEBUG) {
            Timber.tag(TAG + " ${this.javaClass.simpleName}")
            Timber.d("Limpiando memoria...")
        }
        for (alarma in orderedDates) {
            if (BuildConfig.DEBUG) {
                Timber.tag(TAG + " ${this.javaClass.simpleName}")
                Timber.d("Código de alarma a borrar: " + alarma.id)
            }
            var i: Intent? = Intent()
            try {
                i = Intent.parseUri(alarma.alarmIntent, 0)
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }
            alarmManager!!.cancel(
                PendingIntent.getBroadcast(
                    context,
                    alarma.id,
                    i!!,
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        }
                        else -> PendingIntent.FLAG_UPDATE_CURRENT
                    }
                )
            )
            popNotification(alarma.treatmentName, alarma.id)
        }
        orderedDates.clear()
    }

    /**
     * Recorre la lista de fechas programadas y programa una alarma para cada una de ellas.
     */
    fun setNotification() {
        val context = MyApplication.getApp()?.applicationContext!!
        if (orderedDates.isNotEmpty()) {
            // Es posible que la primera posición de la cola sea compartida y 2 o más tratamientos tengan la misma fecha. Hay que aceptarlos a todos en ese caso.
            val primera = orderedDates.first()
            val primeras =
                orderedDates.filter { alarm -> alarm.date.trimMinute() == primera.date.trimMinute() }
            for (alarma in primeras) {
                if (BuildConfig.DEBUG) {
                    Timber.tag(TAG + " ${this.javaClass.simpleName}")
                    Timber.d(
                        "Alarm a programar: " + alarma.treatmentName
                        //.toString() + " - " + (alarma.treatment?.mHoras ?: "ERROR")
                    )
                    Timber.tag(TAG + " ${this.javaClass.simpleName}")
                    Timber.d("Programando alarma...")
                }
                var pendingIntent: PendingIntent? = null
                try {
                    pendingIntent = PendingIntent.getBroadcast(
                        context,
                        alarma.id,
                        Intent.parseUri(alarma.alarmIntent, 0),
                        when {
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            }
                            else -> PendingIntent.FLAG_UPDATE_CURRENT
                        }
                    )
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }

                if (alarmManager == null) { //He añadido esto (26032022)
                    alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                }
                alarmManager!!.setAlarmClock(
                    AlarmClockInfo(
                        alarma.date,
                        pendingIntent
                    ), pendingIntent
                )
                if (BuildConfig.DEBUG) {
                    Timber.tag(TAG + " ${this.javaClass.simpleName}")
                    Timber.d(
                        "Alarma programada: " + alarma.treatmentName
                                + " - DATE "
                                + alarma.date.toString()
                                + " - ID "
                                + " [" + alarma.id.toString() + "]"
                    )
                }
            }
        }
    }

    val isAlarmQueueEmpty: Boolean
        get() = orderedDates.isEmpty()

    /**
     * Given a treatment, deletes all related alarms and notifications.
     * @param d The treatment to be deleted.
     */
    fun remove(d: Treatment) {
        val notificationManager = MyApplication.getApp()!!.notificationManager
        // Dada una dosis, borra todas las alarmas y cancela todas las notificaciones relacionadas con esa dosis.
        orderedDates.filter { it.treatmentName == d.mNombre }.forEach { treatmentAlarm ->
            // Como al sonar la alarma se rehacen todas las dosis desde cero, perdemos el ID en memoria. Como el nombre también es único, comparamos por él.
            if (BuildConfig.DEBUG) {
                Timber.tag(TAG + " ${this.javaClass.simpleName}")
                Timber.d("Cancelando alarma: " + treatmentAlarm.id)
            }
            try {
                val i = Intent.parseUri(treatmentAlarm.alarmIntent, 0)
                alarmManager!!.cancel(
                    PendingIntent.getBroadcast(
                        MyApplication.getApp()?.applicationContext,
                        treatmentAlarm.id,
                        i,
                        when {
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            }
                            else -> PendingIntent.FLAG_UPDATE_CURRENT
                        }
                    )
                )
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }

            // Cleans stock notifications, if any.
            if (BuildConfig.DEBUG) {
                Timber.tag(TAG + " ${this.javaClass.simpleName}")
                Timber.d("Tamaño de la mNotificationPool: " + mNotificationPool.size)
            }
            val alarmIds = ArrayList<Int>()
            var i = 0
            val mNotificationPoolSize = mNotificationPool.size
            while (i < mNotificationPoolSize) {
                val notification = mNotificationPool[i]
                if (d.mNombre.hashCode() == notification.first) alarmIds.add(notification.second)
                i++
            }
            if (BuildConfig.DEBUG) {
                Timber.tag(TAG + " ${this.javaClass.simpleName}")
                Timber.d("Cancelando notificaciones: $alarmIds")
            }
            for (alarmId in alarmIds) {
                notificationManager.cancel(alarmId)
            }
        }
    }

    /**
     * Checks whether a given alarm ID is inside the persistence pool.
     * @param alarmId The alarm ID.
     * @return Whether the ID is contained in the pool or not.
     */
    fun isNotificationInside(alarmId: Int): Boolean {
        val ids = ArrayList<Int>()
        for (notification in mNotificationPool) ids.add(notification.second)
        return ids.contains(alarmId)
    }

    /**
     * Adds a notification ID to the notification persistence pool.
     * @param name The name of the treatment.
     * @param alarmId The alarm ID.
     */
    fun pushNotification(name: String, alarmId: Int) {
        mNotificationPool.add(Pair(name.hashCode(), alarmId))
    }

    /**
     * Removes all notifications associated to a given treatment from the persistence notification pool.
     * @param name The name of the treatment.
     */
    fun popNotification(name: String) {
        mNotificationPool.removeIf { n: Pair<Int, Int> -> name.hashCode() == n.first }
    }

    /**
     * Removes a given notification ID from the notification persistence pool.
     * @param name The name of the treatment.
     * @param alarmId The alarm ID.
     */
    fun popNotification(name: String, alarmId: Int) {
        mNotificationPool.removeIf { n: Pair<Int, Int> -> name.hashCode() == n.first && alarmId == n.second }
    }


    /**
     * Reads saved alarms from a JSON file and saves them to the AlarmPool class orderedDates ArrayList<Alarm>
     * @param c: Context
     */
    fun readAlarmsFromJson(c: Context) {
        if (BuildConfig.DEBUG) {
            Timber.tag(TAG + " ${this.javaClass.simpleName}")
            Timber.d("Restaurando alarmas desde JSON...")
        }
        try {
            orderedDates = format.decodeFromString(
                File(c.filesDir.toString() + "/alarms.json").readText(Charsets.UTF_8)
            )
            if (BuildConfig.DEBUG) {
                Timber.tag(TAG + " ${this.javaClass.simpleName}")
                Timber.d("Alarmas restauradas con éxito. Tamaño: " + orderedDates.size)
            }
        } catch (ignored: IOException) {
            orderedDates = ArrayList<Alarm>()
        }
    }

    fun findTreatmentsUpNext(name: String): Pair<Long, String> {
        val dateFormat = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT
        )
        val date = orderedDates.find { it.treatmentName == name }!!.date
        return Pair(date, dateFormat.format(date))
    }

    // TODO: Qué pasa con las notis de stock? No parece que pase nada...
    fun getTreatmentsPastAlarmId(treatmentName: String, treatmentId: Int): ArrayList<Int> {
        val alarmas = orderedDates.filter {
            it.treatmentName == treatmentName
        }

        val misNotisEnLaPool = mNotificationPool.filter {
            it.first == treatmentId
        }
        val misNotisEnAlarms = alarmas.map { treatment -> treatment.id }

        val d = ArrayList<Int>()

        misNotisEnLaPool.forEach { notiPool ->
            for (notiAlarm in misNotisEnAlarms) {
                if (notiPool.second != notiAlarm) {
                    d.add(notiPool.second)
                }
            }
        }

        return d
    }

    companion object {
        val instance = AlarmPool()

        private fun combine(date: Date, time: Date): Date {
            val cal = Calendar.getInstance()
            cal.time = time
            val hour = cal[Calendar.HOUR_OF_DAY]
            val min = cal[Calendar.MINUTE]
            cal.time = date
            cal[Calendar.HOUR_OF_DAY] = hour
            cal[Calendar.MINUTE] = min
            cal[Calendar.SECOND] = 0
            return cal.time
        }
    }
}