package com.example.odumonitor.data.remote

import com.example.odumonitor.data.model.OduNetInfoPayload
import com.example.odumonitor.data.model.UbusResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

class OduApiService(
    private val client: OkHttpClient = SharedClient,
    private val json: Json = SharedJson
) {
    companion object {
        val SharedJson = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }

        private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        private val netInfoPayloadRequestBody = """[{"jsonrpc":"2.0","id":1,"method":"call","params":["00000000000000000000000000000000","zte_nwinfo_api","nwinfo_get_netinfo",{}]}]""".toRequestBody(jsonMediaType)

        val SharedClient: OkHttpClient by lazy {
            try {
                val trustAllCerts = arrayOf<TrustManager>(
                    object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    }
                )
                val sslContext = SSLContext.getInstance("SSL").apply {
                    init(null, trustAllCerts, SecureRandom())
                }
                OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                    .hostnameVerifier { _, _ -> true }
                    .connectTimeout(2, TimeUnit.SECONDS)
                    .readTimeout(3, TimeUnit.SECONDS)
                    .writeTimeout(2, TimeUnit.SECONDS)
                    .build()
            } catch (e: Exception) {
                OkHttpClient.Builder()
                    .connectTimeout(2, TimeUnit.SECONDS)
                    .readTimeout(3, TimeUnit.SECONDS)
                    .writeTimeout(2, TimeUnit.SECONDS)
                    .build()
            }
        }
    }

    suspend fun fetchNetInfo(): Result<OduNetInfoPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val timestamp = System.currentTimeMillis()
            val ubusUrlWithQuery = "http://192.168.254.1/ubus/?t=$timestamp"

            val request = Request.Builder()
                .url(ubusUrlWithQuery)
                .post(netInfoPayloadRequestBody)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Origin", "http://192.168.254.1")
                .header("Referer", "http://192.168.254.1/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Z-Mode", "0")
                .header("Z-Tag", "nwinfo_get_netinfo")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    throw IOException("HTTP error ${response.code}")
                }
                if (bodyString.isBlank()) {
                    throw IOException("Empty response body")
                }

                // Parse JSON-RPC response
                val ubusResponses = json.decodeFromString<List<UbusResponse>>(bodyString)
                val firstResp = ubusResponses.firstOrNull() 
                    ?: throw IOException("No JSON-RPC response items found")

                firstResp.extractPayload<OduNetInfoPayload>(json)
                    ?: throw IOException("Failed to extract netinfo payload from UBUS response")
            }
        }.recoverCatching { throwable ->
            when (throwable) {
                is SocketTimeoutException -> throw IOException("Connection timed out (192.168.254.1 unreachable)")
                is IOException -> throw throwable
                else -> throw IOException("Failed to connect to ZTE ODU: ${throwable.localizedMessage}")
            }
        }
    }
}
