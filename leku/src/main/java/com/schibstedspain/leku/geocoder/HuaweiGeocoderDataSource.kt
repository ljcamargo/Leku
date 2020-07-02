package com.schibstedspain.leku.geocoder

import android.location.Address
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.LatLngBounds
import com.schibstedspain.leku.geocoder.places.HuaweiSitesDataSource


class HuaweiGeocoderDataSource(private val source: HuaweiSitesDataSource)
    : GeocoderInteractorDataSource {

    override suspend fun getFromLocationName(query: String): List<Address> {
        return source.getGeocodeFromLocationName(query)
    }

    override suspend fun getFromLocationName(query: String, lowerLeft: LatLng, upperRight: LatLng):
            List<Address> {
        return source.getGeocodeFromLocationName(query, LatLngBounds(lowerLeft, upperRight))
    }

    override suspend fun getFromLocation(latitude: Double, longitude: Double): List<Address> {
        return source.getGeocodeLocation(latitude, longitude)
    }
}