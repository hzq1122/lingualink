package com.lingualink.network.discovery

import android.util.Log
import com.lingualink.domain.model.DeviceInfo
import com.lingualink.network.client.DeviceHttpClient
import com.lingualink.network.dto.DeviceInfoDto
import com.lingualink.network.dto.MulticastDto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.MulticastSocket

class MulticastDiscovery(
    private val port: Int = DeviceInfo.DEFAULT_PORT,
    private val deviceClient: DeviceHttpClient,
    private val scope: CoroutineScope
) {
    companion object {
        const val MULTICAST_GROUP = "224.0.0.167"
        val ANNOUNCE_DELAYS = listOf(0L, 200L, 1000L)
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val _discovered = MutableSharedFlow<DeviceInfo>(extraBufferCapacity = 16)
    val discovered: SharedFlow<DeviceInfo> = _discovered
    private var listenJob: Job? = null
    private val seenFingerprints = mutableSetOf<String>()
    private val answeredAnnouncements = mutableSetOf<String>()

    fun startListener(fingerprint: String, getSelfDto: () -> MulticastDto) {
        listenJob = scope.launch(Dispatchers.IO) {
            var socket: MulticastSocket? = null
            try {
                socket = MulticastSocket(port).apply {
                    joinGroup(InetAddress.getByName(MULTICAST_GROUP))
                    soTimeout = 3000
                }
                val buffer = ByteArray(4096)
                val group = InetAddress.getByName(MULTICAST_GROUP)
                Log.i("Multicast", "Listening on port $port")

                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (_: java.net.SocketTimeoutException) {
                        continue
                    }
                    try {
                        val dto = json.decodeFromString<MulticastDto>(
                            String(packet.data, 0, packet.length)
                        )
                        if (dto.fingerprint == fingerprint) continue
                        if (dto.fingerprint in seenFingerprints) {
                            if (dto.announcement && dto.fingerprint !in answeredAnnouncements) {
                                answeredAnnouncements.add(dto.fingerprint)
                                answerAnnouncement(
                                    DeviceInfo(
                                        ip = packet.address.hostAddress ?: continue,
                                        alias = dto.alias,
                                        deviceModel = dto.deviceModel,
                                        fingerprint = dto.fingerprint,
                                        port = dto.port
                                    ), getSelfDto
                                )
                            }
                            continue
                        }
                        seenFingerprints.add(dto.fingerprint)

                        val device = DeviceInfo(
                            ip = packet.address.hostAddress ?: continue,
                            alias = dto.alias,
                            deviceModel = dto.deviceModel,
                            fingerprint = dto.fingerprint,
                            port = dto.port
                        )
                        _discovered.emit(device)

                        if (dto.announcement && dto.fingerprint !in answeredAnnouncements) {
                            answeredAnnouncements.add(dto.fingerprint)
                            answerAnnouncement(device, getSelfDto)
                        }
                    } catch (e: Exception) {
                        Log.w("Multicast", "Parse error: ${e.message}")
                    }
                }
                // Leave multicast group and close socket
                try { socket.leaveGroup(group) } catch (_: Exception) {}
                try { socket.close() } catch (_: Exception) {}
            } catch (e: Exception) {
                Log.e("Multicast", "Listener error: ${e.message}")
                try { socket?.leaveGroup(InetAddress.getByName(MULTICAST_GROUP)) } catch (_: Exception) {}
                try { socket?.close() } catch (_: Exception) {}
            }
        }
    }

    fun sendAnnouncement(selfDto: MulticastDto) {
        scope.launch(Dispatchers.IO) {
            val data = json.encodeToString(selfDto).toByteArray()
            val group = InetAddress.getByName(MULTICAST_GROUP)
            for (delayMs in ANNOUNCE_DELAYS) {
                delay(delayMs)
                try {
                    DatagramSocket().use { socket ->
                        socket.send(DatagramPacket(data, data.size, group, port))
                    }
                } catch (e: Exception) {
                    Log.w("Multicast", "Send failed: ${e.message}")
                }
            }
        }
    }

    private suspend fun answerAnnouncement(peer: DeviceInfo, getSelfDto: () -> MulticastDto) {
        try {
            val selfInfo = getSelfDto()
            val registerDto = DeviceInfoDto(
                alias = selfInfo.alias,
                deviceModel = selfInfo.deviceModel,
                fingerprint = selfInfo.fingerprint,
                port = selfInfo.port
            )
            deviceClient.register(peer, registerDto)
        } catch (e: Exception) {
            // Fallback: UDP answer
            try {
                val data = json.encodeToString(getSelfDto().copy(announcement = false)).toByteArray()
                DatagramSocket().use { socket ->
                    socket.send(DatagramPacket(data, data.size, InetAddress.getByName(peer.ip), port))
                }
            } catch (e2: Exception) {
                Log.w("Multicast", "Answer failed: ${e2.message}")
            }
        }
    }

    fun stop() {
        listenJob?.cancel()
        listenJob = null
        seenFingerprints.clear()
        answeredAnnouncements.clear()
    }
}
