package com.lingualink.network.client

import android.util.Log
import com.lingualink.domain.model.DeviceInfo
import com.lingualink.network.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class DeviceHttpClient {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun discover(ip: String, port: Int = DeviceInfo.DEFAULT_PORT): DeviceInfoDto? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("http://$ip:$port/api/lingualink/v1/info")
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { json.decodeFromString<DeviceInfoDto>(it) }
                } else null
            } catch (e: Exception) {
                null
            }
        }

    suspend fun register(target: DeviceInfo, selfInfo: DeviceInfoDto): DeviceInfoDto? =
        withContext(Dispatchers.IO) {
            try {
                val body = json.encodeToString(selfInfo).toRequestBody(JSON_TYPE)
                val request = Request.Builder()
                    .url("http://${target.ip}:${target.port}/api/lingualink/v1/register")
                    .post(body)
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { json.decodeFromString<DeviceInfoDto>(it) }
                } else null
            } catch (e: Exception) {
                Log.w("DeviceClient", "Register failed: ${e.message}")
                null
            }
        }

    suspend fun sendTranslationRequest(target: DeviceInfo, request: TranslateRequestDto): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val body = json.encodeToString(request).toRequestBody(JSON_TYPE)
                val req = Request.Builder()
                    .url("http://${target.ip}:${target.port}/api/lingualink/v1/translate")
                    .post(body)
                    .build()
                client.newCall(req).execute().isSuccessful
            } catch (e: Exception) {
                Log.w("DeviceClient", "Translate request failed: ${e.message}")
                false
            }
        }

    suspend fun sendTranslationResult(target: DeviceInfo, result: TranslateResultDto): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val body = json.encodeToString(result).toRequestBody(JSON_TYPE)
                val req = Request.Builder()
                    .url("http://${target.ip}:${target.port}/api/lingualink/v1/translate/result")
                    .post(body)
                    .build()
                client.newCall(req).execute().isSuccessful
            } catch (e: Exception) {
                Log.w("DeviceClient", "Translate result failed: ${e.message}")
                false
            }
        }

    suspend fun ping(target: DeviceInfo): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("http://${target.ip}:${target.port}/api/lingualink/v1/ping")
                .post("".toRequestBody(JSON_TYPE))
                .build()
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
