package com.schibstedspain.leku.geocoder.places

import android.location.Address
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.RuntimeExecutionException
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.huawei.hms.maps.model.LatLngBounds
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observable.defer
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.listOf
import com.google.android.gms.tasks.Tasks as GoogleTasks

private const val PREDICTIONS_WAITING_TIME: Long = 6
private const val PLACE_BY_ID_WAITING_TIME: Long = 3

class GooglePlacesDataSource(private val geoDataClient: PlacesClient) {

    /*fun <T>resultConversion(task: GoogleTask<T>): Task<T> {

    }*/

    fun getFromLocationName(query: String, latLngBounds: LatLngBounds): Observable<List<Address>> {
        val southwest = LatLng(latLngBounds.southwest.latitude, latLngBounds.southwest.longitude)
        val northeast = LatLng(latLngBounds.northeast.latitude, latLngBounds.northeast.longitude)
        val locationBias = RectangularBounds.newInstance(southwest, northeast)
        return defer {
            val findAutocompletePredictionsRequest = FindAutocompletePredictionsRequest
                .builder()
                .setQuery(query)
                .setLocationBias(locationBias)
                .build()
            val results = geoDataClient.findAutocompletePredictions(findAutocompletePredictionsRequest)
            try {
                GoogleTasks.await(results, PREDICTIONS_WAITING_TIME, TimeUnit.SECONDS)
            } catch (ignored: ExecutionException) {
            } catch (ignored: InterruptedException) {
            } catch (ignored: TimeoutException) {
            }

            try {
                val addressList = getAddressListFromPrediction(results.result)
                return@defer Observable.just(addressList)
            } catch (e: RuntimeExecutionException) {
                return@defer Observable.just(ArrayList<Address>())
            }
        }
    }

    private fun getAddressListFromPrediction(result: FindAutocompletePredictionsResponse?): List<Address> {
        val addressList = ArrayList<Address>()
        result?.let { predictionsResults ->
            for (prediction in predictionsResults.autocompletePredictions) {
                val placeFields = listOf(Place.Field.ID, Place.Field.NAME)
                val fetchPlaceRequest = FetchPlaceRequest.builder(prediction.placeId, placeFields).build()
                val placeBufferResponseTask = geoDataClient.fetchPlace(fetchPlaceRequest)
                try {
                    GoogleTasks.await(placeBufferResponseTask, PLACE_BY_ID_WAITING_TIME, TimeUnit.SECONDS)
                } catch (ignored: ExecutionException) {
                } catch (ignored: InterruptedException) {
                } catch (ignored: TimeoutException) {
                }

                val placeBufferResponse = placeBufferResponseTask.result
                val place = placeBufferResponse?.place
                place?.let {
                    addressList.add(mapPlaceToAddress(it))
                }
            }
        }
        return addressList
    }

    private fun mapPlaceToAddress(place: Place): Address {
        val address = Address(Locale.getDefault())
        place.latLng?.let {
            address.latitude = it.latitude
            address.longitude = it.longitude
        }
        val addressName = place.name.toString() + " - " + place.address.toString()
        address.setAddressLine(0, addressName)
        address.featureName = addressName
        return address
    }
}
