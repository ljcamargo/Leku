package com.schibstedspain.leku.utils

import android.content.Context
import android.location.Location
import com.huawei.hmf.tasks.Tasks
import com.huawei.hms.api.HuaweiApiClient
import com.huawei.hms.location.FusedLocationProviderClient
import com.huawei.hms.location.LocationServices
import com.huawei.hms.support.api.client.ApiClient
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class BaseLocationService(val context: Context) {

    fun getApiClient(ready: (ApiClient?) -> Unit) {
        var client: HuaweiApiClient? = null
        client = HuaweiApiClient.Builder(context)
            .addConnectionCallbacks(object : HuaweiApiClient.ConnectionCallbacks {
                override fun onConnected() = ready(client)
                override fun onConnectionSuspended(error: Int) = ready(null)
            })
            .addOnConnectionFailedListener { ready(null) }
            .build()
    }

    fun getLocationProvider(): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    suspend fun getLastKnownLocation(cc: CoroutineContext): Location? = withContext(cc) {
        val provider = getLocationProvider()
        try {
            return@withContext Tasks.await(provider.lastLocation)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
        return@withContext null
    }
}
