package com.schibstedspain.leku.geocoder.api

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.net.UnknownHostException
import java.nio.charset.Charset
import javax.net.ssl.HttpsURLConnection

private const val REPONSE_MAX_LENGTH = 1024
private const val READ_TIMEOUT = 3000
private const val CONNECT_TIMEOUT = 3000

class NetworkClient {

    fun getUrl(request: String): String? {
        var result: String? = null
        var stream: InputStream? = null
        var connection: HttpsURLConnection? = null
        try {
            val url = URL(request)
            connection = url.openConnection() as HttpsURLConnection
            connection.readTimeout = READ_TIMEOUT
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.requestMethod = "GET"
            connection.doInput = true
            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw NetworkException("HTTP error code: $responseCode")
            }
            stream = connection.inputStream
            if (stream != null) {
                result = readStream(stream, REPONSE_MAX_LENGTH)
            }
        } catch (ignore: UnknownHostException) {
        } catch (ioException: IOException) {
            throw NetworkException(ioException)
        } finally {
            if (stream != null) {
                try {
                    stream.close()
                } catch (ioException: IOException) {
                    throw NetworkException(ioException)
                }
            }
            connection?.disconnect()
        }
        return result
    }

    fun postUrl(request: String, body: String): String? {
        var result: String? = null
        var stream: InputStream? = null
        var connection: HttpsURLConnection? = null
        try {
            val url = URL(request)
            connection = url.openConnection() as HttpsURLConnection
            connection.readTimeout = READ_TIMEOUT
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.doInput = true
            val outputStream = connection.outputStream
            outputStream.write(body.toByteArray(Charset.defaultCharset()))
            outputStream.close()
            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw NetworkException("HTTP error code: $responseCode")
            }
            stream = connection.inputStream
            if (stream != null) {
                result = readStream(stream, REPONSE_MAX_LENGTH)
            }
        } catch (ignore: UnknownHostException) {
        } catch (ioException: IOException) {
            throw NetworkException(ioException)
        } finally {
            if (stream != null) {
                try {
                    stream.close()
                } catch (ioException: IOException) {
                    throw NetworkException(ioException)
                }
            }
            connection?.disconnect()
        }
        return result
    }

    @Throws(IOException::class)
    private fun readStream(stream: InputStream, maxLength: Int): String {
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(maxLength)
        var length = stream.read(buffer)
        while (length != -1) {
            result.write(buffer, 0, length)
            length = stream.read(buffer)
        }
        return result.toString("UTF-8")
    }
}
