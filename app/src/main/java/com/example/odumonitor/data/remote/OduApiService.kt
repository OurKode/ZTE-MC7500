package com.example.odumonitor.data.remote

import com.example.odumonitor.data.local.RouterCredentials
import com.example.odumonitor.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.MessageDigest
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

        val SharedClient: OkHttpClient by lazy {
            val builder = OkHttpClient.Builder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(4, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)

            runCatching {
                val trustAllCerts = arrayOf<TrustManager>(
                    object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    }
                )
                val sslContext = SSLContext.getInstance("TLS").apply {
                    init(null, trustAllCerts, SecureRandom())
                }
                builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                    .hostnameVerifier { _, _ -> true }
            }
            builder.build()
        }

        fun sha256HexUpper(input: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(input.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02X".format(it) }
        }

        fun computeZtePasswordHash(password: String, salt: String): String {
            val passHash = sha256HexUpper(password)
            return sha256HexUpper(passHash + salt)
        }
    }

    @Volatile
    private var sessionToken: String = "00000000000000000000000000000000"

    @Volatile
    var isCurrentlyLoggedIn: Boolean = false
        private set

    fun resetSession() {
        sessionToken = "00000000000000000000000000000000"
        isCurrentlyLoggedIn = false
    }

    suspend fun login(host: String, username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val timestamp = System.currentTimeMillis()
            val cleanHost = host.trim()

            // 1. Request Salt
            val saltPayload = """[{"jsonrpc":"2.0","id":1,"method":"call","params":["00000000000000000000000000000000","zwrt_web","web_login_info",{}]}]"""
            val saltResp = executeRpc(cleanHost, "web_login_info", saltPayload, timestamp)
            val saltJson = json.decodeFromString<List<UbusResponse>>(saltResp)
            val loginInfo = saltJson.firstOrNull()?.extractPayload<OduLoginInfoPayload>(json)
                ?: throw IOException("Gagal memperoleh salt autentikasi dari router")

            val salt = loginInfo.zteWebSault
            if (salt.isNullOrBlank()) {
                throw IOException("Router tidak mengembalikan zte_web_sault")
            }

            // 2. Compute Double-SHA256 Uppercase Hash
            val passwordHash = computeZtePasswordHash(password, salt)

            // 3. Submit Web Login
            val loginPayload = """[{"jsonrpc":"2.0","id":2,"method":"call","params":["00000000000000000000000000000000","zwrt_web","web_login",{"username":"$username","password":"$passwordHash"}]}]"""
            val loginResp = executeRpc(cleanHost, "web_login", loginPayload, timestamp + 1)
            val loginJson = json.decodeFromString<List<UbusResponse>>(loginResp)
            val loginResult = loginJson.firstOrNull()?.extractPayload<OduLoginResultPayload>(json)
                ?: throw IOException("Gagal memproses respons login router")

            if (loginResult.result == 0 && !loginResult.ubusRpcSession.isNullOrBlank()) {
                sessionToken = loginResult.ubusRpcSession
                isCurrentlyLoggedIn = true
                sessionToken
            } else {
                isCurrentlyLoggedIn = false
                throw IOException("Kredensial salah (kode respons router: ${loginResult.result})")
            }
        }
    }

    suspend fun fetchTelemetry(creds: RouterCredentials): Result<OduTelemetryBundle> = withContext(Dispatchers.IO) {
        val cleanHost = creds.host.trim().ifBlank { "192.168.254.1" }

        // If login is enabled and we are not currently logged in, try authenticating first
        if (creds.isLoginEnabled && (!isCurrentlyLoggedIn || sessionToken == "00000000000000000000000000000000")) {
            login(cleanHost, creds.username, creds.password).onFailure {
                // Login failed, but don't hard crash: proceed unauthenticated so RF metrics still work
                isCurrentlyLoggedIn = false
                sessionToken = "00000000000000000000000000000000"
            }
        }

        var attemptResult = fetchTelemetryInternal(cleanHost, creds.isLoginEnabled && isCurrentlyLoggedIn)

        // Auto-reconnect if session timed out (-32002 Access Denied)
        if (attemptResult.isFailure && creds.isLoginEnabled) {
            val err = attemptResult.exceptionOrNull()?.message ?: ""
            if (err.contains("-32002") || err.contains("Access denied") || err.contains("session")) {
                resetSession()
                login(cleanHost, creds.username, creds.password).onSuccess {
                    attemptResult = fetchTelemetryInternal(cleanHost, true)
                }
            }
        }

        // Final fallback: if authenticated fetch still failed, try unauthenticated RF polling
        if (attemptResult.isFailure) {
            attemptResult = fetchTelemetryInternal(cleanHost, isAuth = false)
        }

        attemptResult
    }

    private fun fetchTelemetryInternal(host: String, isAuth: Boolean): Result<OduTelemetryBundle> {
        return runCatching {
            val timestamp = System.currentTimeMillis()
            val token = if (isAuth) sessionToken else "00000000000000000000000000000000"

            val payload = if (isAuth) {
                // Batch query: netinfo + router status + traffic + sim info + thermal + device info in 1 single HTTP request
                """[
                    {"jsonrpc":"2.0","id":1,"method":"call","params":["$token","zte_nwinfo_api","nwinfo_get_netinfo",{}]},
                    {"jsonrpc":"2.0","id":2,"method":"call","params":["$token","zwrt_router.api","router_get_status",{}]},
                    {"jsonrpc":"2.0","id":3,"method":"call","params":["$token","zwrt_data","get_wwandst",{"source_module":"web","cid":1,"type":4}]},
                    {"jsonrpc":"2.0","id":4,"method":"call","params":["$token","zwrt_zte_mdm.api","get_sim_info",{}]},
                    {"jsonrpc":"2.0","id":6,"method":"call","params":["$token","zwrt_bsp.thermal","get_cpu_temp",{}]},
                    {"jsonrpc":"2.0","id":7,"method":"call","params":["$token","zwrt_mc.device.manager","get_device_info",{}]}
                ]""".trimIndent()
            } else {
                """[{"jsonrpc":"2.0","id":1,"method":"call","params":["00000000000000000000000000000000","zte_nwinfo_api","nwinfo_get_netinfo",{}]}]"""
            }

            val tag = if (isAuth) "router_get_status" else "nwinfo_get_netinfo"
            val respString = executeRpc(host, tag, payload, timestamp)
            val responses = json.decodeFromString<List<UbusResponse>>(respString)

            // Check if router rejected due to expired session
            val authError = responses.firstOrNull { resp ->
                val errStr = resp.error?.toString() ?: ""
                errStr.contains("-32002")
            }
            if (authError != null) {
                throw IOException("Session expired (-32002)")
            }

            val netInfo = responses.find { it.id == 1 }?.extractPayload<OduNetInfoPayload>(json)
                ?: throw IOException("Gagal mengekstrak netinfo dari router")

            val routerStatus = if (isAuth) responses.find { it.id == 2 }?.extractPayload<OduRouterStatusPayload>(json) else null
            val traffic = if (isAuth) responses.find { it.id == 3 }?.extractPayload<OduTrafficPayload>(json) else null
            val simInfo = if (isAuth) responses.find { it.id == 4 }?.extractPayload<OduSimInfoPayload>(json) else null
            val thermal = if (isAuth) responses.find { it.id == 6 }?.extractPayload<OduThermalPayload>(json) else null
            val deviceInfo = if (isAuth) responses.find { it.id == 7 }?.extractPayload<OduDeviceInfoPayload>(json) else null

            OduTelemetryBundle(
                netInfo = netInfo,
                routerStatus = routerStatus,
                traffic = traffic,
                simInfo = simInfo,
                userListNum = null,
                thermal = thermal,
                deviceInfo = deviceInfo,
                isLoggedIn = isAuth
            )
        }
    }

    private fun executeRpc(host: String, tag: String, jsonBody: String, timestamp: Long): String {
        val httpsUrl = "https://$host/ubus/?t=$timestamp"
        val httpUrl = "http://$host/ubus/?t=$timestamp"

        return runCatching {
            executeRequest(httpsUrl, tag, jsonBody, host)
        }.recoverCatching {
            executeRequest(httpUrl, tag, jsonBody, host)
        }.getOrThrow()
    }

    private fun executeRequest(targetUrl: String, tag: String, jsonBody: String, host: String): String {
        val isHttps = targetUrl.startsWith("https", ignoreCase = true)
        val scheme = if (isHttps) "https" else "http"
        val origin = "$scheme://$host"
        val referer = "$scheme://$host/"

        val request = Request.Builder()
            .url(targetUrl)
            .post(jsonBody.toRequestBody(jsonMediaType))
            .header("Content-Type", "application/json; charset=utf-8")
            .header("Accept", "application/json, text/javascript, */*; q=0.01")
            .header("Origin", origin)
            .header("Referer", referer)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Z-Mode", "0")
            .header("Z-Tag", tag)
            .build()

        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw IOException("HTTP error ${response.code}")
            }
            if (bodyString.isBlank()) {
                throw IOException("Respons router kosong")
            }
            return bodyString
        }
    }

    suspend fun executeAuthenticatedCommand(
        creds: RouterCredentials,
        service: String,
        method: String,
        paramsJson: String,
        tag: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanHost = creds.host.trim().ifBlank { "192.168.254.1" }

        // Ensure session is alive
        if (!isCurrentlyLoggedIn || sessionToken == "00000000000000000000000000000000") {
            val loginRes = login(cleanHost, creds.username, creds.password)
            if (loginRes.isFailure) {
                return@withContext Result.failure(
                    loginRes.exceptionOrNull() ?: IOException("Autentikasi router diperlukan untuk perintah ini")
                )
            }
        }

        var lastException: Throwable? = null
        for (attempt in 1..3) {
            val result = runCatching {
                val timestamp = System.currentTimeMillis()
                val payload = """[{"jsonrpc":"2.0","id":1,"method":"call","params":["$sessionToken","$service","$method",$paramsJson]}]"""
                val respStr = executeRpc(cleanHost, tag, payload, timestamp)
                val respList = json.decodeFromString<List<UbusResponse>>(respStr)
                val resp = respList.firstOrNull() ?: throw IOException("Respons router kosong")

                val errStr = resp.error?.toString() ?: ""
                if (errStr.contains("-32002")) {
                    resetSession()
                    login(cleanHost, creds.username, creds.password).getOrThrow()
                    throw IOException("Session expired (-32002), retrying...")
                }

                val resultCode = resp.result?.firstOrNull()?.toString()?.toIntOrNull() ?: 0
                if (resultCode != 0) {
                    throw IOException("Router menolak konfigurasi (kode respons: $resultCode)")
                }
                true
            }

            if (result.isSuccess) {
                return@withContext Result.success(true)
            } else {
                lastException = result.exceptionOrNull()
                kotlinx.coroutines.delay(250)
            }
        }

        Result.failure(lastException ?: IOException("Gagal mengeksekusi $method"))
    }

    suspend fun fetchNetInfo(): Result<OduNetInfoPayload> = withContext(Dispatchers.IO) {
        fetchTelemetry(RouterCredentials(isLoginEnabled = false)).map { it.netInfo }
    }
}
