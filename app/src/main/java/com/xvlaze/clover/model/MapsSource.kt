package com.xvlaze.clover.model

import android.content.Context
import com.huawei.hms.location.LocationServices
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.site.api.SearchResultListener
import com.huawei.hms.site.api.SearchServiceFactory
import com.huawei.hms.site.api.model.NearbySearchRequest
import com.huawei.hms.site.api.model.NearbySearchResponse
import com.huawei.hms.site.api.model.SearchStatus

object MapsSource {
    fun getCurrentLocation(c: Context, callback: IMapCallback) {
        val provider = LocationServices.getFusedLocationProviderClient(c)
        provider.lastLocation.addOnCompleteListener {
            if (it.isSuccessful) {
                val lastKnownLocation = it.result
                if (lastKnownLocation != null) {
                    callback.onSuccess(LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude))
                }
            }
        }
    }

    fun searchNearbyPharmacies(c: Context, req: NearbySearchRequest, apikey: String, callback: IMapCallback) {
        val searchService = SearchServiceFactory.create(c, apikey)

        val listener: SearchResultListener<NearbySearchResponse?> =
            object : SearchResultListener<NearbySearchResponse?> {
                override fun onSearchResult(response: NearbySearchResponse?) {
                    when {
                        response == null -> {
                            return
                        }
                        response.totalCount <= 0 -> {
                            callback.onSuccess(response.sites)
                        }
                    }
                    if (response != null) {
                        callback.onSuccess(response.sites)
                    }
                }

                override fun onSearchError(p0: SearchStatus?) {
                    callback.onSuccess(-1)
                }
            }
        searchService.nearbySearch(req, listener)
    }

    interface IMapCallback {
        fun onSuccess(payload: Any)
    }
}