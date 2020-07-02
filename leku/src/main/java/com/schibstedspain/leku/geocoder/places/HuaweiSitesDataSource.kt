package com.schibstedspain.leku.geocoder.places

import android.location.Address
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.LatLngBounds
import com.huawei.hms.site.api.SearchResultListener
import com.huawei.hms.site.api.SearchService
import com.huawei.hms.site.api.model.Coordinate
import com.huawei.hms.site.api.model.CoordinateBounds
import com.huawei.hms.site.api.model.DetailSearchRequest
import com.huawei.hms.site.api.model.DetailSearchResponse
import com.huawei.hms.site.api.model.LocationType
import com.huawei.hms.site.api.model.NearbySearchRequest
import com.huawei.hms.site.api.model.NearbySearchResponse
import com.huawei.hms.site.api.model.QuerySuggestionRequest
import com.huawei.hms.site.api.model.QuerySuggestionResponse
import com.huawei.hms.site.api.model.SearchStatus
import com.huawei.hms.site.api.model.Site
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class HuaweiSitesDataSource(private val searchService: SearchService) {

    suspend fun getGeocodeLocation(latitude: Double, longitude: Double): List<Address> {
        val result = onNearbySearch(NearbySearchRequest().also {
            it.location = Coordinate(latitude, longitude)
            it.poiType = LocationType.ADDRESS
        })
        return getAddressListFromPrediction(result?.sites)
    }

    suspend fun getGeocodeFromLocationName(query: String, bounds: LatLngBounds? = null):
            List<Address> {
        val result = onQuerySuggestion(QuerySuggestionRequest().also {
            it.query = query
            it.poiTypes = listOf(LocationType.ADDRESS)
            bounds?.let { bounds ->
                val southwest = Coordinate(bounds.southwest.latitude, bounds.southwest.longitude)
                val northeast = Coordinate(bounds.northeast.latitude, bounds.northeast.longitude)
                it.bounds = CoordinateBounds(northeast, southwest)
            }
        })
        return getAddressListFromPrediction(result?.sites)
    }

    suspend fun getFromLocationName(
            query: String,
            southwest: LatLng? = null,
            northeast: LatLng? = null,
            countryCode: String? = null,
            language: String? = null,
            types: String? = null
    ): List<Address> {
        val result = onQuerySuggestion(QuerySuggestionRequest().also {
            it.query = query
            if (southwest != null && northeast != null) {
                val sw = Coordinate(southwest.latitude, southwest.longitude)
                val ne = Coordinate(northeast.latitude, northeast.longitude)
                it.bounds = CoordinateBounds(ne, sw)
            }
            countryCode?.let { countryCode -> it.countryCode = countryCode }
            language?.let { language -> it.language = language }
            it.poiTypes = listOf(
                    LocationType.ADDRESS,
                    LocationType.ESTABLISHMENT,
                    LocationType.REGIONS
            )
            types?.let { types ->
                val poyTypes = types.split(", ").mapNotNull {
                    when (it.toUpperCase()) {
                        "GEOCODE" -> LocationType.GEOCODE
                        "ADDRESS" -> LocationType.ADDRESS
                        "ESTABLISHMENT" -> LocationType.ESTABLISHMENT
                        "REGIONS" -> LocationType.REGIONS
                        else -> null
                    }
                }
                it.poiTypes = poyTypes
            }
        })
        return getAddressListFromPrediction(result?.sites)
    }

    suspend fun getAddressListFromPrediction(sites: List<Site>?): List<Address> {
        return sites
                ?.mapNotNull { getSiteData(it.siteId) }
                ?.map { siteToAddress(it) }
                ?: listOf()
    }

    suspend fun getSiteData(siteId: String): Site? {
        return onSiteSearch(DetailSearchRequest().also {
            it.siteId = siteId
        })?.site
    }

    private fun siteToAddress(site: Site): Address {
        val address = Address(Locale.getDefault())
        site.location?.let {
            address.latitude = it.lat
            address.longitude = it.lng
        }
        //val addressName = site.name.toString() + " - " + site.address.toString()
        val addressName = "${site.name} - ${site.formatAddress}"
        address.setAddressLine(0, addressName)
        address.featureName = addressName
        return address
    }

    suspend fun onQuerySuggestion(request: QuerySuggestionRequest):
            QuerySuggestionResponse? = suspendCoroutine {
        val callback = object : SearchResultListener<QuerySuggestionResponse> {
            override fun onSearchError(status: SearchStatus?) {
                it.resume(null)
            }
            override fun onSearchResult(response: QuerySuggestionResponse?) {
                it.resume(response)
            }
        }
        searchService.querySuggestion(request, callback)
    }

    suspend fun onNearbySearch(request: NearbySearchRequest):
            NearbySearchResponse? = suspendCoroutine {
        val callback = object : SearchResultListener<NearbySearchResponse> {
            override fun onSearchError(status: SearchStatus?) {
                it.resume(null)
            }
            override fun onSearchResult(response: NearbySearchResponse?) {
                it.resume(response)
            }
        }
        searchService.nearbySearch(request, callback)
    }

    suspend fun onSiteSearch(request: DetailSearchRequest):
            DetailSearchResponse? = suspendCoroutine {
        val listener = object : SearchResultListener<DetailSearchResponse> {
            override fun onSearchError(status: SearchStatus?) {
                it.resume(null)
            }
            override fun onSearchResult(response: DetailSearchResponse?) {
                it.resume(response)
            }
        }
        searchService.detailSearch(request, listener)
    }
}
