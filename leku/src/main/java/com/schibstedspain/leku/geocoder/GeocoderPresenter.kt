package com.schibstedspain.leku.geocoder

import android.location.Address
import android.util.Log
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.LatLngBounds
import com.schibstedspain.leku.geocoder.places.HuaweiSitesDataSource
import com.schibstedspain.leku.geocoder.timezone.HuaweiTimeZoneDataSource
import com.schibstedspain.leku.utils.BaseLocationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

private const val RETRY_COUNT = 3
private const val MAX_PLACES_RESULTS = 3

class GeocoderPresenter @JvmOverloads constructor(
    private val locationService: BaseLocationService,
    private val geocoderRepository: GeocoderRepository,
    private val huaweiSitesDataSource: HuaweiSitesDataSource? = null,
    private val timeZoneDataSource: HuaweiTimeZoneDataSource? = null,
    private val scope: CoroutineScope = GlobalScope
) {

    private var view: GeocoderViewInterface? = null
    private val nullView = GeocoderViewInterface.NullView()
    private var isHuaweiSitesEnabled = false

    init {
        this.view = nullView
    }

    fun setUI(geocoderViewInterface: GeocoderViewInterface) {
        this.view = geocoderViewInterface
    }

    fun stop() {
        this.view = nullView
    }

    fun getLastKnownLocation() {
        scope.launch(Dispatchers.Main) {
            try {
                locationService.getLastKnownLocation(Dispatchers.Main)?.let {
                    view?.showLastLocation(it)
                }
            } catch (exception: java.lang.Exception) {
            } finally {
                view?.didGetLastLocation()
            }
        }
    }

    fun getFromLocationName(query: String) {
        view?.willLoadLocation()
        scope.launch(Dispatchers.Main) {
            try {
                val locations = geocoderRepository.getFromLocationName(query)
                view?.showLocations(locations)
            } catch (exception: java.lang.Exception) {
                exception.printStackTrace()
                view?.showLoadLocationError()
            } finally {
                view?.didLoadLocation()
            }
        }
    }

    fun getFromLocationName(query: String, lowerLeft: LatLng, upperRight: LatLng) {
        view?.willLoadLocation()
        scope.launch(Dispatchers.Main) {
            try {
                val geoCodeAddresses = geocoderRepository.getFromLocationName(query, lowerLeft, upperRight)
                val sitesAddresses = getPlacesFromLocationName(query, lowerLeft, upperRight)
                val allAddresses = sitesAddresses + geoCodeAddresses
                view?.showLocations(allAddresses)
            } catch (exception: Exception) {
                exception.printStackTrace()
                view?.showLoadLocationError()
            } finally {
                view?.didLoadLocation()
            }
        }
    }

    fun getInfoFromLocation(latLng: LatLng) {
        view?.willGetLocationInfo(latLng)
        scope.launch(Dispatchers.Main) {
            try {
                val location = geocoderRepository.getFromLocation(latLng).first()
                view?.showLocationInfo(returnTimeZone(location)!!)
            } catch (exception: Exception) {
                view?.showGetLocationInfoError()
            } finally {
                view?.didGetLocationInfo()
            }
        }
    }

    private suspend fun returnTimeZone(address: Address): Pair<Address, TimeZone?>? {
        val timeZone = try {
            timeZoneDataSource?.getTimeZone(address.latitude, address.longitude)
        } catch (exception: Exception) {
            null
        }
        return address to timeZone
    }

    fun enableGooglePlaces() {
        this.isHuaweiSitesEnabled = true
    }

    private suspend fun getPlacesFromLocationName(
        query: String,
        lowerLeft: LatLng,
        upperRight: LatLng
    ): List<Address> {
        return if (isHuaweiSitesEnabled && huaweiSitesDataSource != null) {
            huaweiSitesDataSource
                    .getFromLocationName(query, LatLngBounds(lowerLeft, upperRight))
                    .take(MAX_PLACES_RESULTS)
                    .toList()
        } else {
            listOf()
        }
    }
}
