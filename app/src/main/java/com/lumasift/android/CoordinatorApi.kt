package com.lumasift.android

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class CoordinatorApi(private val settings: CoordinatorSettings) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build()

    init {
        require(settings.baseUrl.startsWith("https://")) { "Coordinator URL must use HTTPS." }
        require(settings.accessToken.isNotBlank()) { "Coordinator access token is required." }
    }

    fun progress(): Progress = get("/api/lumasift/status", Progress.serializer()) ?: error("Coordinator returned no status payload.")
    fun plan(): Plan? = get("/api/lumasift/plan", Plan.serializer())
    fun start(selectedTypes: List<String>): Progress = post("/api/lumasift/start", json.encodeToString(StartRequest(selectedTypes)), Progress.serializer())
    fun apply(planId: String): Plan = post("/api/lumasift/plan/apply", json.encodeToString(mapOf("plan_id" to planId)), Plan.serializer())

    private fun <T> get(path: String, serializer: kotlinx.serialization.KSerializer<T>): T? {
        val request = request(path).get().build()
        client.newCall(request).execute().use { response ->
            if (response.code == 404 && path.endsWith("/plan")) return null
            val body = response.body?.string().orEmpty()
            check(response.isSuccessful) { "Coordinator request failed (${response.code}): ${body.take(240)}" }
            return json.decodeFromString(serializer, body)
        }
    }

    private fun <R> post(path: String, payload: String, serializer: kotlinx.serialization.KSerializer<R>): R {
        val body = payload.toRequestBody("application/json".toMediaType())
        client.newCall(request(path).post(body).build()).execute().use { response ->
            val content = response.body?.string().orEmpty()
            check(response.isSuccessful) { "Coordinator request failed (${response.code}): ${content.take(240)}" }
            return json.decodeFromString(serializer, content)
        }
    }

    private fun request(path: String): Request.Builder = Request.Builder()
        .url(settings.baseUrl.trimEnd('/') + path)
        .header("Authorization", "Bearer ${settings.accessToken}")
        .header("Accept", "application/json")
}
