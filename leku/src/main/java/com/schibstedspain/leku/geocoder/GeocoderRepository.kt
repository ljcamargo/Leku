package com.schibstedspain.leku.geocoder

import android.location.Address
import com.huawei.hms.maps.model.LatLng


class GeocoderRepository(
        private val internalGeocoder: GeocoderInteractorDataSource,
        private val providerGeocoder: GeocoderInteractorDataSource
) {

    suspend fun getFromLocationName(query: String): List<Address> {
        return try {
            internalGeocoder.getFromLocationName(query)
        } catch (exception: Exception) {
            providerGeocoder.getFromLocationName(query)
        }
    }

    suspend fun getFromLocationName(query: String, lowerLeft: LatLng, upperRight: LatLng)
            : List<Address> {
        return try {
            internalGeocoder.getFromLocationName(query, lowerLeft, upperRight)
        } catch (exception: Exception) {
            providerGeocoder.getFromLocationName(query, lowerLeft, upperRight)
        }
    }

    suspend fun getFromLocation(latLng: LatLng): List<Address> {
        return try {
            internalGeocoder.getFromLocation(latLng.latitude, latLng.longitude)
        } catch (exception: Exception) {
            providerGeocoder.getFromLocation(latLng.latitude, latLng.longitude)
        }
    }
}
