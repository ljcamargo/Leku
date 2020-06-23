package com.schibstedspain.leku.geocoder

import android.location.Address
import android.location.Geocoder
import com.huawei.hms.maps.model.LatLng

private const val MAX_RESULTS = 5

class AndroidGeocoderDataSource(private val geocoder: Geocoder) : GeocoderInteractorDataSource {

    override suspend fun getFromLocationName(query: String): List<Address> {
        return geocoder.getFromLocationName(query, MAX_RESULTS)
    }

    override suspend fun getFromLocationName(query: String, lowerLeft: LatLng, upperRight: LatLng): List<Address> {
        return geocoder.getFromLocationName(query, MAX_RESULTS, lowerLeft.latitude,
                lowerLeft.longitude, upperRight.latitude, upperRight.longitude)
    }

    override suspend fun getFromLocation(latitude: Double, longitude: Double): List<Address> {
        return geocoder.getFromLocation(latitude, longitude, MAX_RESULTS)
    }
}
