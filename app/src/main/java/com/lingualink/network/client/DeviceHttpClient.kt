package com.lingualink.network.client

import android.util.Log
import com.lingualink.domain.model.DeviceInfo
import com.lingualink.network.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class DeviceHttpClient {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
            connectTimeoutMillis = 3000
        }
    }

    suspend fun discover(ip: String, port: Int = DeviceInfo.DEFAULT_PORT): DeviceInfoDto? {
        return try {
            client.get("http://$ip:$port/api/lingualink/v1/info").body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun register(target: DeviceInfo, selfInfo: DeviceInfoDto): DeviceInfoDto? {
        return try {
            client.post("http://${target.ip}:${target.port}/api/lingualink/v1/register") {
                contentType(ContentType.Application.Json)
                setBody(selfInfo)
            }.body()
        } catch (e: Exception) {
            Log.w("DeviceClient", "Register failed: ${e.message}")
            null
        }
    }

    suspend fun sendTranslationRequest(target: DeviceInfo, request: TranslateRequestDto): Boolean {
        return try {
            val response = client.post("http://${target.ip}:${target.port}/api/lingualink/v1/translate") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            Log.w("DeviceClient", "Translate request failed: ${e.message}")
            false
        }
    }

    suspend fun sendTranslationResult(target: DeviceInfo, result: TranslateResultDto): Boolean {
        return try {
            val response = client.post("http://${target.ip}:${target.port}/api/lingualink/v1/translate/result") {
                contentType(ContentType.Application.Json)
                setBody(result)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            Log.w("DeviceClient", "Translate result failed: ${e.message}")
            false
        }
    }

    suspend fun ping(target: DeviceInfo): Boolean {
        return try {
            val response = client.post("http://${target.ip}:${target.port}/api/lingualink/v1/ping")
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }
}
