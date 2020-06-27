package com.schibstedspain.leku.geocoder

import android.location.Address
import com.huawei.hms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val RETRY_COUNT = 3

class GeocoderRepository(
    private val androidGeocoder: GeocoderInteractorDataSource,
    private val googleGeocoder: GeocoderInteractorDataSource
) {

    suspend fun getFromLocationName(query: String): List<Address> = withContext(Dispatchers.IO) {
        return@withContext try {
            androidGeocoder.getFromLocationName(query)
        } catch (exception: Exception) {
            googleGeocoder.getFromLocationName(query)
        }
    }

    suspend fun getFromLocationName(query: String, lowerLeft: LatLng, upperRight: LatLng)
            : List<Address> = withContext(Dispatchers.IO) {
        return@withContext try {
            androidGeocoder.getFromLocationName(query, lowerLeft, upperRight)
        } catch (exception: Exception) {
            googleGeocoder.getFromLocationName(query, lowerLeft, upperRight)
        }
    }

    suspend fun getFromLocation(latLng: LatLng): List<Address> = withContext(Dispatchers.IO) {
        return@withContext try {
            androidGeocoder.getFromLocation(latLng.latitude, latLng.longitude)
        } catch (exception: Exception) {
            googleGeocoder.getFromLocation(latLng.latitude, latLng.longitude)
        }
    }
}
