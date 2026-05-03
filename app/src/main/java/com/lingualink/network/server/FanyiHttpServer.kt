package com.lingualink.network.server

import android.util.Log
import com.lingualink.domain.model.DeviceInfo
import com.lingualink.network.dto.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class FanyiHttpServer(
    private val port: Int = DeviceInfo.DEFAULT_PORT,
    private val scope: CoroutineScope
) {
    private var serverSocket: ServerSocket? = null
    private var running = false
    var onTranslateRequest: (suspend (TranslateRequestDto) -> Unit)? = null
    var onTranslateResult: (suspend (TranslateResultDto) -> Unit)? = null
    var getDeviceInfo: (suspend () -> DeviceInfoDto)? = null

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun start() {
        try {
            serverSocket = ServerSocket(port)
            running = true
            scope.launch(Dispatchers.IO) {
                while (isActive && running) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        launch(Dispatchers.IO) { handleConnection(socket) }
                    } catch (e: Exception) {
                        if (running) Log.w("HttpServer", "Accept error: ${e.message}")
                    }
                }
            }
            Log.i("HttpServer", "Started on port $port")
        } catch (e: Exception) {
            Log.e("HttpServer", "Failed to start on port $port", e)
        }
    }

    fun stop() {
        running = false
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }

    private suspend fun handleConnection(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val path = parts[1]

            // Read headers and body
            val headers = mutableMapOf<String, String>()
            var contentLength = 0
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    val key = line.substring(0, colonIdx).trim().lowercase()
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                    if (key == "content-length") contentLength = value.toIntOrNull() ?: 0
                }
            }

            var body = ""
            if (contentLength > 0) {
                val chars = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val r = reader.read(chars, read, contentLength - read)
                    if (r == -1) break
                    read += r
                }
                body = String(chars, 0, read)
            }

            val response = route(method, path, body)
            sendResponse(socket.getOutputStream(), response)
        } catch (e: Exception) {
            Log.w("HttpServer", "Connection error: ${e.message}")
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private suspend fun route(method: String, path: String, body: String): Pair<Int, String> {
        return try {
            when {
                method == "GET" && path == "/api/lingualink/v1/info" -> {
                    val info = getDeviceInfo?.invoke()
                    if (info != null) 200 to json.encodeToString(info)
                    else 503 to """{"error":"not ready"}"""
                }
                method == "POST" && path == "/api/lingualink/v1/register" -> {
                    val info = getDeviceInfo?.invoke()
                    if (info != null) 200 to json.encodeToString(info)
                    else 200 to "{}"
                }
                method == "POST" && path == "/api/lingualink/v1/translate" -> {
                    val request = json.decodeFromString<TranslateRequestDto>(body)
                    scope.launch(Dispatchers.IO) { onTranslateRequest?.invoke(request) }
                    200 to """{"ok":true}"""
                }
                method == "POST" && path == "/api/lingualink/v1/translate/result" -> {
                    val result = json.decodeFromString<TranslateResultDto>(body)
                    scope.launch(Dispatchers.IO) { onTranslateResult?.invoke(result) }
                    200 to """{"ok":true}"""
                }
                method == "POST" && path == "/api/lingualink/v1/ping" -> {
                    200 to """{"timestamp":${System.currentTimeMillis()}}"""
                }
                else -> 404 to """{"error":"not found"}"""
            }
        } catch (e: Exception) {
            Log.e("HttpServer", "Route error: ${e.message}")
            500 to """{"error":"${e.message?.replace("\"", "'") ?: "internal error"}"}"""
        }
    }

    private fun sendResponse(output: OutputStream, response: Pair<Int, String>) {
        val (code, body) = response
        val statusText = when (code) {
            200 -> "OK"; 404 -> "Not Found"; 500 -> "Internal Server Error"; else -> "Error"
        }
        val bytes = body.toByteArray()
        val header = "HTTP/1.1 $code $statusText\r\n" +
            "Content-Type: application/json; charset=utf-8\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "Connection: close\r\n\r\n"
        output.write(header.toByteArray())
        output.write(bytes)
        output.flush()
    }
}
