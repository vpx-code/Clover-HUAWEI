package com.xvlaze.clover.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.xvlaze.clover.R
import com.xvlaze.clover.model.MLProvider
import com.xvlaze.clover.model.Treatment
import com.xvlaze.clover.repository.SharedPrefsRepository
import com.xvlaze.clover.repository.TreatmentsRepository

class DataFormViewModel(val app: Application): AndroidViewModel(app) {
    private val treatmentsRepository = TreatmentsRepository(app.applicationContext)
    private val sharedPrefsRepository = SharedPrefsRepository(app.applicationContext)

    private val _blisterMax = 500
    private val _intakeMax = 5
    private val isNameEditTextEnabled = true

    private var treatmentName = MutableLiveData<String>()
    private var blisterQuantity = MutableLiveData<Int>()
    private var intakeQuantity = MutableLiveData<Double>()
    private var isMondayChecked = MutableLiveData<Boolean>()
    private var isTuesdayChecked = MutableLiveData<Boolean>()
    private var isWednesdayChecked = MutableLiveData<Boolean>()
    private var isThursdayChecked = MutableLiveData<Boolean>()
    private var isFridayChecked = MutableLiveData<Boolean>()
    private var isSaturdayChecked = MutableLiveData<Boolean>()
    private var isSundayChecked = MutableLiveData<Boolean>()
    private var specificDays = false

    val arePermissionsGranted = MutableLiveData<Boolean>()
    var scannedTreatment = MutableLiveData<String>()
    var treatmentDays = BooleanArray(7)
    var editedTreatment = MutableLiveData<Treatment>()
    var isEditMode = MutableLiveData<Boolean>()
    var correctTreatment = MutableLiveData<Boolean>()
    val theme = MutableLiveData<Int>()

    fun checkIntegrity() {
        when {
            treatmentName.value?.isNotBlank() ?: false &&
                    blisterQuantity.value ?: 0 != 0 &&
                    intakeQuantity.value ?: 0 != 0.0 &&
                    blisterQuantity.value ?: 0 > 0 &&
                    blisterQuantity.value ?: 0 <= _blisterMax &&
                    intakeQuantity.value ?: 0.0 > 0 &&
                    intakeQuantity.value ?: 0.0 <= _intakeMax &&
                    blisterQuantity.value ?: 0 >= intakeQuantity.value!! ->
            {
                when {
                    specificDays -> {
                        when {
                            treatmentDays.any { true } &&
                                    (isNameEditTextEnabled &&
                                            treatmentName.value?.let {
                                                treatmentsRepository.findTreatmentByName(
                                                    it
                                                )
                                            } == null ||
                                            !isNameEditTextEnabled) -> {
                                correctTreatment.postValue(true)
                            }
                            else -> {
                                correctTreatment.postValue(false)
                            }
                        }
                    }
                    isEditMode.value == false &&
                            treatmentName.value?.let {
                                treatmentsRepository.findTreatmentByName(
                                    it
                                )
                            } == null ||
                            isEditMode.value == true -> {
                        correctTreatment.postValue(true)
                    }
                    else -> {
                        correctTreatment.postValue(false)
                    }
                }
            }
            else -> {
                correctTreatment.postValue(false)
            }
        }
    }

    // TODO: Mi idea es hacer una lista de los elementos editables para poner en rojo lo que está mal. Pensarlo bien porque es complejo. Tocar solo si es requerimiento.
    /*fun checkIntegrity2() {
        val list = mutableListOf(0, 0, 0, 0, 0, 0, 0, 0, 0)
        when {
            treatmentName.value?.isNotBlank() == true -> {
                list[0] = 1
                when {
                    blisterQuantity.value ?: 0 != 0 &&
                            blisterQuantity.value ?: 0 > 0 &&
                            blisterQuantity.value ?: 0 <= _blisterMax -> {
                        when {
                            blisterQuantity.value ?: 0 >= intakeQuantity.value!! -> {
                                list[1] = 1
                                when {
                                    intakeQuantity.value ?: 0 != 0.0 &&
                                            intakeQuantity.value ?: 0.0 > 0 -> {
                                        when {
                                            intakeQuantity.value ?: 0.0 <= _intakeMax -> {
                                                list[2] = 1
                                                when {
                                                    specificDays -> {
                                                        when {
                                                            treatmentDays.any { true } &&
                                                                    (isNameEditTextEnabled &&
                                                                            treatmentName.value?.let {
                                                                                treatmentsRepository.findTreatmentByName(
                                                                                    it
                                                                                )
                                                                            } == null ||
                                                                            !isNameEditTextEnabled) -> {
                                                                list[3] = 1
                                                                correctTreatment.postValue(true)
                                                            }
                                                            else -> {
                                                                list[3] = 0
                                                                correctTreatment.postValue(false)
                                                            }
                                                        }
                                                    }
                                                    isEditMode.value == false &&
                                                            treatmentName.value?.let {
                                                                treatmentsRepository.findTreatmentByName(
                                                                    it
                                                                )
                                                            } == null || isEditMode.value == true -> {
                                                        correctTreatment.postValue(true)
                                                    }
                                                    else -> {
                                                        correctTreatment.postValue(false)
                                                    }
                                                }
                                            }
                                            else -> {
                                                list[2] = 0
                                                correctTreatment.postValue(false)
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                list[1] = 0
                            }
                        }
                    }
                }
            }
            else -> {
                list[0] = 0
            }
        }
    }*/

    fun onDayCheckedChange(id: Int, state: Boolean) {
        when (id) {
            R.string.lunes -> {
                isMondayChecked.setValue(state)
            }
            R.string.martes -> {
                isTuesdayChecked.setValue(state)
            }
            R.string.miercoles -> {
                isWednesdayChecked.setValue(state)
            }
            R.string.jueves -> {
                isThursdayChecked.setValue(state)
            }
            R.string.viernes -> {
                isFridayChecked.setValue(state)
            }
            R.string.sabado -> {
                isSaturdayChecked.setValue(state)
            }
            R.string.domingo -> {
                isSundayChecked.setValue(state)
            }
        }
        checkIntegrity()
    }

    fun onRadioButtonChecked(id: Int) {
        when (id) {
            R.id.optionA -> { treatmentDays.fill(true)
                specificDays = false
            }
            R.id.optionB -> { treatmentDays = booleanArrayOf(
                isSundayChecked.value ?: false,
                isMondayChecked.value ?: false,
                isTuesdayChecked.value ?: false,
                isWednesdayChecked.value ?: false,
                isThursdayChecked.value ?: false,
                isFridayChecked.value ?: false,
                isSundayChecked.value ?: false
            )
                specificDays = true
            }
            R.id.optionC -> { treatmentDays.fill(false)
                specificDays = false
            }
        }
        checkIntegrity()
    }

    fun onTextChanged(name: String) {
        treatmentName.value = name.ifBlank { "" }
    }

    fun onBlisterQuantityChanged(qty: String) {
        blisterQuantity.value = if (qty.isNotBlank()) qty.toInt() else 0
    }

    fun onIntakeQuantityChanged(qty: String) {
        intakeQuantity.value = if (qty.isNotBlank()) qty.toDouble() else 0.0
    }

    fun areWeEditing(name: String?) {
        isEditMode.postValue(name?.isNotBlank() ?: false)
    }

    fun findTreatmentByName(editedTreatmentName: String?) {
        editedTreatment.postValue(editedTreatmentName?.let { treatmentsRepository.findTreatmentByName(it) })
    }

    fun scanTreatment(imageUri: Uri) {
        treatmentsRepository.scanTreatment(imageUri, object: MLProvider.IMLRecognitionCallback {
            override fun onSuccess(text: String) {
                scannedTreatment.postValue(text)
            }
        })
    }

    fun askForPermissions(c: Context) {
        Dexter.withContext(c)
            .withPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        arePermissionsGranted.postValue(true)
                    }
                    if (report.isAnyPermissionPermanentlyDenied) {
                        arePermissionsGranted.postValue(false)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    fun getTheme() = theme.postValue(sharedPrefsRepository.getTheme())

    fun completeTutorialPart() = sharedPrefsRepository.completeTutorial(2)

    class MyViewModelFactory(val app: Application): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(DataFormViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                DataFormViewModel(app) as T
            } else {
                throw IllegalArgumentException("ViewModel Not Found")
            }
        }
    }
}
