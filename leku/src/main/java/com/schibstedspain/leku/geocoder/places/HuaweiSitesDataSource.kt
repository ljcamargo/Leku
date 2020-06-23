package com.schibstedspain.leku.geocoder.places

import android.location.Address
import com.huawei.hms.maps.model.LatLngBounds
import com.huawei.hms.site.api.SearchResultListener
import com.huawei.hms.site.api.SearchService
import com.huawei.hms.site.api.model.Coordinate
import com.huawei.hms.site.api.model.CoordinateBounds
import com.huawei.hms.site.api.model.DetailSearchRequest
import com.huawei.hms.site.api.model.DetailSearchResponse
import com.huawei.hms.site.api.model.QuerySuggestionRequest
import com.huawei.hms.site.api.model.QuerySuggestionResponse
import com.huawei.hms.site.api.model.SearchStatus
import com.huawei.hms.site.api.model.Site
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class HuaweiSitesDataSource(private val searchService: SearchService) {

    suspend fun getFromLocationName(query: String, bounds: LatLngBounds): List<Address> {
        val southwest = Coordinate(bounds.southwest.latitude, bounds.southwest.longitude)
        val northeast = Coordinate(bounds.northeast.latitude, bounds.northeast.longitude)
        val locationBias = CoordinateBounds(northeast, southwest)
        val result = resultListener(QuerySuggestionRequest().also {
            it.query = query
            it.bounds = locationBias
        })
        return getAddressListFromPrediction(result)
    }

    suspend fun resultListener(request: QuerySuggestionRequest): QuerySuggestionResponse? {
        return suspendCoroutine { coroutine ->
            val callback = object: SearchResultListener<QuerySuggestionResponse>  {
                override fun onSearchError(status: SearchStatus?) {
                    coroutine.resume(null)
                }
                override fun onSearchResult(response: QuerySuggestionResponse?) {
                    coroutine.resume(response)
                }
            }
            searchService.querySuggestion(request, callback)
        }
    }

    suspend fun getAddressListFromPrediction(result: QuerySuggestionResponse?): List<Address> {
        return result?.sites
                ?.mapNotNull { getSiteData(it.siteId) }
                ?.map { siteToAddress(it) }
                ?: listOf()
    }

    suspend fun getSiteData(siteId:String): Site? = suspendCoroutine { coroutine ->
        val detailRequest = DetailSearchRequest().also {
            it.siteId = siteId
        }
        val listener = object: SearchResultListener<DetailSearchResponse> {
            override fun onSearchError(status: SearchStatus?) {
                coroutine.resume(null)
            }
            override fun onSearchResult(response: DetailSearchResponse?) {
                coroutine.resume(response?.site)
            }
        }
        searchService.detailSearch(detailRequest, listener)
    }


    private fun siteToAddress(site: Site): Address {
        val address = Address(Locale.getDefault())
        site.location?.let {
            address.latitude = it.lat
            address.longitude = it.lng
        }
        val addressName = site.name.toString() + " - " + site.address.toString()
        address.setAddressLine(0, addressName)
        address.featureName = addressName
        return address
    }
}
