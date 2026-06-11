package com.xvlaze.clover.watch

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import com.huawei.hmf.tasks.Task
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.auth.AuthCallback
import com.huawei.wearengine.auth.Permission
import com.huawei.wearengine.common.WearEngineErrorCode
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.p2p.Message
import com.huawei.wearengine.p2p.P2pClient
import com.huawei.wearengine.p2p.Receiver
import com.huawei.wearengine.p2p.SendCallback
import com.xvlaze.clover.BuildConfig
import com.xvlaze.clover.model.AlarmPool
import com.xvlaze.clover.model.TreatmentsSource
import com.xvlaze.clover.ui.RestockDialog
import com.xvlaze.clover.util.Constants
import com.xvlaze.clover.util.Extensions.toPlainText
import com.xvlaze.clover.util.MyApplication
import timber.log.Timber
import java.io.File
import java.nio.charset.StandardCharsets


// FIXME: Cuando estás en "Toca 2 veces para consumir", ¡si consumes desde el Watch no sales de esa pantalla y puedes consumir 2 veces!
object Watch {
    private var firstSync = true
    private val authClient = HiWear.getAuthClient(MyApplication.getApp()!!.applicationContext)
    private val permissions: Array<Permission> = arrayOf(
        Permission.DEVICE_MANAGER,
        Permission.NOTIFY
    )
    private lateinit var connectedDevice: Device
    private const val peerPkgName = "com.xvlaze.clover.watch"
    private val peerFingerPrint = buildPeerFingerprint()
    private var p2pClient: P2pClient = HiWear.getP2pClient(MyApplication.getApp()!!.applicationContext)
    private var currentTreatmentName = ""
    private val notificationManager by lazy { MyApplication.getApp()!!.notificationManager }
    private var alarmId = -1
    private lateinit var consumeReceiver: Receiver

    init {
        //TODO("Seguro que has cambiado la fingerprint a release?")

        if (isBluetoothEnabled())
        {
            p2pClient.setPeerPkgName(peerPkgName)
            p2pClient.setPeerFingerPrint(peerFingerPrint)

            checkPermissions()
                ?.addOnSuccessListener {
                    checkDevices()
                }
                ?.addOnFailureListener {
                    val authCallback: AuthCallback = object : AuthCallback {
                        override fun onOk(permissions: Array<Permission>) {
                            // Return a list of permissions granted by the user.
                        }
                        override fun onCancel() {}
                    }
                    authClient.requestPermission(authCallback, Permission.DEVICE_MANAGER)
                }

            consumeReceiver = Receiver { msg ->
                if (BuildConfig.DEBUG) Timber.tag(Constants.TAG).d("Receiver triggered!")

                if (msg.type == Message.MESSAGE_TYPE_DATA) {
                    val context = MyApplication.getApp()!!.applicationContext
                    when (val clearText = msg.toPlainText()) {
                        "sync" -> {
                            sendTreatmentsJSON(context)
                        }
                        "restock" -> {
                            throwRestockDialog(context)
                        }
                        else -> {
                            val name = clearText.split("\n")[0]
                            val queueSize = clearText.split("\n")[1].toInt()
                            currentTreatmentName = name
                            consumeRoutine(context, queueSize)
                        }
                    }
                }
            }
        }
    }

    fun checkDevices() {
        if (isBluetoothEnabled()) {
            // Store the paired device list.
            val deviceList: MutableList<Device> = ArrayList()

            // Step 1: Obtain the DeviceClient object.
            val deviceClient = HiWear.getDeviceClient(MyApplication.getApp()!!.applicationContext)

            // Step 2: Obtain the list of paired devices.
            deviceClient.bondedDevices
                .addOnSuccessListener { devices ->
                    deviceList.addAll(devices!!)
                    if (deviceList.isNotEmpty()) {
                        for (device in deviceList) {
                            if (device.isConnected) {
                                connectedDevice = device
                                if (BuildConfig.DEBUG) Timber.tag(Constants.TAG).d("Registering Receiver...")
                                p2pClient.registerReceiver(
                                    device,
                                    consumeReceiver
                                )

                                if (BuildConfig.DEBUG) Timber.tag(Constants.TAG).d("Sending fist_sync message to Watch...")
                                sendMessage(
                                    connectedDevice,
                                    "first_sync"
                                )
                                break
                            }
                        }
                    }
                }
        }
    }

    fun sendReminder(
        context: Context,
        nombreTratamiento: String,
        numeroDosis: Int,
        alarmId: Int
    ) {
        if (isBluetoothEnabled()) {
            checkDevices()
            currentTreatmentName = nombreTratamiento
            this.alarmId = alarmId
            val remainingAfterIntake = (
                    TreatmentsSource.findTreatmentByName(
                        nombreTratamiento,
                        context
                    )!!
                        .mTreatmentRestantes - 1)
            val severity = when {
                remainingAfterIntake == 0 -> {
                    2
                }
                remainingAfterIntake > 3 -> {
                    0
                }
                else -> {
                    1
                }
            }
            ping(
                connectedDevice,
                "${nombreTratamiento}\n" +
                        "$numeroDosis\n" +
                        "$severity"
            )
        }
    }

    private fun ping(device: Device, message: String? = null) {
        p2pClient.ping(device) {
            when (it) {
                WearEngineErrorCode.ERROR_CODE_P2P_WATCH_APP_NOT_RUNNING -> {
                    ping(device, message)
                }

                WearEngineErrorCode.ERROR_CODE_P2P_WATCH_APP_RUNNING -> {
                    message?.let { message ->
                        sendMessage(device, message)
                    }
                }
            }
        }
    }

    private fun sendMessage(device: Device, message: String) {
        p2pClient
            .send(
                device,
                Message.Builder()
                    .setPayload(message.toByteArray(StandardCharsets.UTF_8))
                    .build(),
                object : SendCallback {
                    override fun onSendResult(code: Int) {
                        if (code != WearEngineErrorCode.ERROR_CODE_COMM_SUCCESS &&
                            message != "first_sync")
                            sendMessage(device, message)
                    }
                    override fun onSendProgress(progress: Long) {
                    }
                }
            )
    }

    private fun consumeRoutine(context: Context, queue: Int) {
        val t = TreatmentsSource.findTreatmentByName(currentTreatmentName, context)!!
        TreatmentsSource.consume(
            context,
            t
        )
        t.let { notificationManager.cancel(it.mId) } // Elimina notificaciones de stock.

        // TODO: ¿Necesaria una lista? ¿Qué pasa con las notificaciones de stock (no parece que pase nada...)?
        /* TODO: Si a un usuario le suena la alarma y la ignora hasta que suene la siguiente de ese
            mismo tratamiento, con este código se consumirían las 2. Habría que notificar al usuario de que se ha dejado una, no?
         */
        for (pastAlarmId in AlarmPool.instance.getTreatmentsPastAlarmId(t.mNombre, t.mId)) {
            notificationManager.cancel(pastAlarmId)
        }

        try {
            // Sacamos las notificaciones de stock y otras que hayan podido quedar de la pool para que no se vuelven a llamar.
            currentTreatmentName.let { AlarmPool.instance.popNotification(it) }
        } catch (ignored: NullPointerException) {}

        if (queue == 0) {
            sendTreatmentsJSON(context)
        }
    }

    private fun throwRestockDialog(context: Context) {
        Intent(context, RestockDialog::class.java).apply {
            putExtra("nombreTratamiento", currentTreatmentName)
            putExtra("alarmId", alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(this)
        }
    }

    private fun checkPermissions(): Task<Array<Boolean>>? = authClient.checkPermissions(permissions)

    private fun sendTreatmentsJSON(c: Context) {
        // Step 2: Build a message that can be used for sending files.
        val filePath = c.filesDir.toString() + "/config.json"
        val sendFile = File(filePath)
        val builder = Message.Builder()
        if (sendFile.exists())
            builder.setPayload(sendFile)
        else
            builder.setPayload("empty".toByteArray())

        val fileMessage = builder.build()

        val sendCallback: SendCallback = object : SendCallback {
            override fun onSendResult(resultCode: Int) {
            }

            override fun onSendProgress(progress: Long) {}
        }

        // Store the paired device list.
        val deviceList: MutableList<Device> = ArrayList()

        // Step 1: Obtain the DeviceClient object.
        val deviceClient = HiWear.getDeviceClient(MyApplication.getApp()!!.applicationContext)

        // Step 2: Obtain the list of paired devices.
        deviceClient.bondedDevices
            .addOnSuccessListener { devices ->
                deviceList.addAll(devices!!)
                if (deviceList.isNotEmpty()) {
                    for (device in deviceList) {
                        if (device.isConnected) {
                            connectedDevice = device
                            break
                        }
                    }
                    if (connectedDevice.isConnected &&
                        fileMessage != null) {
                        p2pClient.send(connectedDevice, fileMessage, sendCallback)
                    }
                }
            }
    }

    fun killConsumeDialog(name: String) {
        if (isBluetoothEnabled()) {
            val builder = Message.Builder()
            builder.setPayload("kill\n$name".toByteArray(StandardCharsets.UTF_8))
            val message = builder.build()

            val sendCallback: SendCallback = object : SendCallback {
                override fun onSendResult(resultCode: Int) {
                }

                override fun onSendProgress(progress: Long) {}
            }

            // Store the paired device list.d
            val deviceList: MutableList<Device> = ArrayList()

            // Step 1: Obtain the DeviceClient object.
            val deviceClient = HiWear.getDeviceClient(MyApplication.getApp()!!.applicationContext)

            // Step 2: Obtain the list of paired devices.
            deviceClient.bondedDevices
                .addOnSuccessListener { devices ->
                    deviceList.addAll(devices!!)
                    if (deviceList.isNotEmpty()) {
                        for (device in deviceList) {
                            if (device.isConnected) {
                                connectedDevice = device
                                break
                            }
                        }
                        if (connectedDevice.isConnected &&
                            message != null) {
                            p2pClient.send(connectedDevice, message, sendCallback)
                        }
                    }
                }
        }
    }

    private fun isBluetoothEnabled() : Boolean = BluetoothAdapter.getDefaultAdapter().isEnabled

    private fun buildPeerFingerprint(): String {
        return "$peerPkgName" + "_" + BuildConfig.APPLICATION_ID
    }
}


