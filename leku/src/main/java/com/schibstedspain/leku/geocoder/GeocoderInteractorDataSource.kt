package com.schibstedspain.leku.geocoder

import android.location.Address
import com.huawei.hms.maps.model.LatLng

interface GeocoderInteractorDataSource {
    suspend fun getFromLocationName(query: String): List<Address>

    suspend fun getFromLocationName(query: String, lowerLeft: LatLng, upperRight: LatLng): List<Address>

    suspend fun getFromLocation(latitude: Double, longitude: Double): List<Address>
}
