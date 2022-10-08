package com.xvlaze.clover.repository

import android.content.Context
import com.huawei.hms.site.api.model.NearbySearchRequest
import com.xvlaze.clover.model.MapsSource

class MapsRepository (private val c: Context) {
    fun getCurrentLocation(
        callback: MapsSource.IMapCallback) =
        MapsSource.getCurrentLocation(
            c,
            callback
        )

    fun searchNearbyPharmacies(
        req: NearbySearchRequest,
        apikey: String,
        callback: MapsSource.IMapCallback) =
        MapsSource.searchNearbyPharmacies(
            c,
            req,
            apikey,
            callback
        )
}