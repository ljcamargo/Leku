package com.schibstedspain.leku.geocoder.timezone

import com.schibstedspain.leku.geocoder.api.NetworkClient
import org.json.JSONObject
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class HuaweiTimeZoneDataSource(private val apiKey: String) {

    val url = "https://siteapi.cloud.huawei.com/mapApi/v1/timezoneService/getTimezone?key="
    val networkClient = NetworkClient()

    suspend fun getTimeZone(latitude: Double, longitude: Double): TimeZone? = suspendCoroutine {
        coroutine ->
        val location = JSONObject().apply {
            put("lng", longitude)
            put("lat", latitude)
        }
        val json = JSONObject().apply {
            put("location", location)
            put("timestamp", System.currentTimeMillis() / 1000)
            //put("language", "es")
        }
        val result = networkClient.postUrl(
            request = url + apiKey,
            body = json.toString(4)
        )
        try {
            val response = JSONObject(result)
            val id = response.getString("timeZoneId")
            coroutine.resume(TimeZone.getTimeZone(id))
        } catch (exception: Exception) {
            exception.printStackTrace()
            coroutine.resume(null)
        }
    }
}