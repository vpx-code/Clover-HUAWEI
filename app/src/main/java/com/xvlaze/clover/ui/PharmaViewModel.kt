package com.xvlaze.clover.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.site.api.model.Coordinate
import com.huawei.hms.site.api.model.HwLocationType
import com.huawei.hms.site.api.model.NearbySearchRequest
import com.huawei.hms.site.api.model.Site
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.xvlaze.clover.model.MapsSource
import com.xvlaze.clover.repository.MapsRepository
import com.xvlaze.clover.repository.SharedPrefsRepository

class PharmaViewModel (val app: Application): AndroidViewModel(app) {
    private val mapsRepository = MapsRepository(app.applicationContext)
    private val sharedPrefsRepository = SharedPrefsRepository(app.applicationContext)

    var lastLocation = MutableLiveData<LatLng>()
    var pois = MutableLiveData<List<Site>>()
    var isCallPermissionGranted = MutableLiveData<Boolean>()
    val theme = MutableLiveData<Int>()

    @SuppressLint("MissingPermission")
    fun getCurrentLocation() {
        mapsRepository.getCurrentLocation(
            object: MapsSource.IMapCallback {
                override fun onSuccess(payload: Any) {
                    lastLocation.postValue(payload as LatLng)
                }
            })
    }

    fun searchNearbyPharmacies(location: LatLng, apikey: String) {
        val searchRequest = NearbySearchRequest()
        searchRequest.location = Coordinate(location.latitude, location.longitude)
        searchRequest.radius = 50000
        searchRequest.hwPoiType = HwLocationType.PHARMACY // FIXME: Al parecer, no hay resultados.
        mapsRepository.searchNearbyPharmacies(
            searchRequest,
            apikey,
        object: MapsSource.IMapCallback {
            override fun onSuccess(payload: Any) {
                if (payload == -1) {
                    pois.postValue(mutableListOf())
                }
                else {
                    pois.postValue(payload as MutableList<Site>)
                }
            }
        })
    }

    fun handleCall(c: Context) {
        Dexter.withContext(c)
            .withPermissions(Manifest.permission.CALL_PHONE)
            .withListener(object :
                MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        isCallPermissionGranted.postValue(true)
                    }
                    if (report.isAnyPermissionPermanentlyDenied) {
                        isCallPermissionGranted.postValue(false)
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

    class MyViewModelFactory(val app: Application): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(PharmaViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                PharmaViewModel(app) as T
            } else {
                throw IllegalArgumentException("ViewModel Not Found")
            }
        }
    }
}