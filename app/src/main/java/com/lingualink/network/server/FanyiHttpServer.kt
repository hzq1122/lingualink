package com.lingualink.network.server

import android.util.Log
import com.lingualink.BuildConfig
import com.lingualink.domain.model.DeviceInfo
import com.lingualink.network.dto.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class FanyiHttpServer(
    private val port: Int = DeviceInfo.DEFAULT_PORT,
    private val scope: CoroutineScope
) {
    private var server: ApplicationEngine? = null
    var onTranslateRequest: (suspend (TranslateRequestDto) -> Unit)? = null
    var onTranslateResult: (suspend (TranslateResultDto) -> Unit)? = null
    var getDeviceInfo: (suspend () -> DeviceInfoDto)? = null

    fun start() {
        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
            routing {
                get("/api/lingualink/v1/info") {
                    val info = getDeviceInfo?.invoke()
                    if (info != null) call.respond(info)
                    else call.respond(HttpStatusCode.ServiceUnavailable)
                }

                post("/api/lingualink/v1/register") {
                    val dto = call.receive<DeviceInfoDto>()
                    Log.i("HttpServer", "Device registered: ${dto.alias}")
                    val info = getDeviceInfo?.invoke()
                    if (info != null) call.respond(info)
                    else call.respond(HttpStatusCode.OK)
                }

                post("/api/lingualink/v1/translate") {
                    val request = call.receive<TranslateRequestDto>()
                    scope.launch(Dispatchers.IO) { onTranslateRequest?.invoke(request) }
                    call.respond(HttpStatusCode.OK)
                }

                post("/api/lingualink/v1/translate/result") {
                    val result = call.receive<TranslateResultDto>()
                    scope.launch(Dispatchers.IO) { onTranslateResult?.invoke(result) }
                    call.respond(HttpStatusCode.OK)
                }

                post("/api/lingualink/v1/ping") {
                    call.respond(PingResponse(System.currentTimeMillis()))
                }
            }
        }.start()
        Log.i("HttpServer", "Started on port $port")
    }

    fun stop() {
        server?.stop(1000, 5000)
        server = null
        Log.i("HttpServer", "Stopped")
    }
}
