package chat.revolt.sentinel.lookout.realtime

import chat.revolt.sentinel.lookout.Constants
import chat.revolt.sentinel.lookout.RevoltAPI
import chat.revolt.sentinel.lookout.RevoltHttp
import chat.revolt.sentinel.lookout.RevoltJson
import chat.revolt.sentinel.lookout.realtime.frames.receivable.*
import chat.revolt.sentinel.lookout.realtime.frames.sendable.AuthorizationFrame
import chat.revolt.sentinel.lookout.realtime.frames.sendable.BeginTypingFrame
import chat.revolt.sentinel.lookout.realtime.frames.sendable.EndTypingFrame
import chat.revolt.sentinel.lookout.realtime.frames.sendable.PingFrame
import chat.revolt.sentinel.lookout.routes.server.fetchMember
import chat.revolt.sentinel.lookout.schemas.Member
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import org.slf4j.LoggerFactory

enum class DisconnectionState {
    Disconnected,
    Reconnecting,
    Connected
}

sealed class RealtimeSocketFrames {
    data object Reconnected : RealtimeSocketFrames()
}

object RealtimeSocket {
    private val logger = LoggerFactory.getLogger(this::class.java)!!

    var socket: WebSocketSession? = null

    var disconnectionState = DisconnectionState.Reconnecting
        private set

    suspend fun connect(): Result<Unit> {
        if (disconnectionState == DisconnectionState.Connected) {
            Result.failure<Unit>(Exception("Already connected to websocket. Refusing to connect again."))
        }
        socket?.close(CloseReason(CloseReason.Codes.NORMAL, "Reconnecting to websocket."))

        val token = RevoltAPI.authToken ?: return Result.failure<Unit>(Exception("No token, did you log in?"))

        RevoltHttp.ws(Constants.REVOLT_WEBSOCKET) {
            socket = this

            disconnectionState = DisconnectionState.Connected
            pushReconnectEvent()

            // Send authorization frame
            val authFrame = AuthorizationFrame("Authenticate", token)

            send(RevoltJson.encodeToString(AuthorizationFrame.serializer(), authFrame))

            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val frameString = frame.readText()
                    val frameType =
                        RevoltJson.decodeFromString(AnyFrame.serializer(), frameString).type

                    RevoltAPI.wsFrameChannel.send(frameType)

                    handleFrame(frameType, frameString)
                }
            }
        }

        return Result.success(Unit)
    }

    suspend fun sendPing() {
        if (disconnectionState != DisconnectionState.Connected) return

        val pingPacket = PingFrame("Ping", System.currentTimeMillis())
        socket?.send(RevoltJson.encodeToString(PingFrame.serializer(), pingPacket))
    }

    private suspend fun handleFrame(type: String, rawFrame: String) {
        when (type) {
            "Pong" -> {
                val pongFrame = RevoltJson.decodeFromString(PingFrame.serializer(), rawFrame)
                logger.debug("Pong received: ${pongFrame.data}ms")
                RevoltAPI.wsFrameChannel.send(pongFrame)
            }

            "Bulk" -> {
                val bulkFrame = RevoltJson.decodeFromString(BulkFrame.serializer(), rawFrame)
                bulkFrame.v.forEach { subFrame ->
                    val subFrameType =
                        RevoltJson.decodeFromString(AnyFrame.serializer(), subFrame.toString()).type
                    handleFrame(subFrameType, subFrame.toString())
                }
            }

            "Ready" -> {
                val readyFrame = RevoltJson.decodeFromString(ReadyFrame.serializer(), rawFrame)

                val userMap = readyFrame.users.associateBy { it.id!! }
                RevoltAPI.userCache.putAll(userMap)
                val serverMap = readyFrame.servers.associateBy { it.id!! }
                RevoltAPI.serverCache.putAll(serverMap)
                val channelMap = readyFrame.channels.associateBy { it.id!! }
                RevoltAPI.channelCache.putAll(channelMap)
                val emojiMap = readyFrame.emojis.associateBy { it.id!! }
                RevoltAPI.emojiCache.putAll(emojiMap)
            }

            "Message" -> {
                val messageFrame = RevoltJson.decodeFromString(MessageFrame.serializer(), rawFrame)

                if (messageFrame.id == null) {
                    return
                }

                RevoltAPI.messageCache[messageFrame.id] = messageFrame

                messageFrame.channel?.let {
                    if (RevoltAPI.channelCache[it] == null) {
                        return
                    }

                    RevoltAPI.channelCache[it] =
                        RevoltAPI.channelCache[it]!!.copy(lastMessageID = messageFrame.id)

                    RevoltAPI.wsFrameChannel.send(messageFrame)
                }
            }

            "ChannelCreate" -> {
                val channelCreateFrame = RevoltJson.decodeFromString(ChannelCreateFrame.serializer(), rawFrame)

                if (channelCreateFrame.id == null) {
                    return
                }

                RevoltAPI.channelCache[channelCreateFrame.id] = channelCreateFrame

                RevoltAPI.wsFrameChannel.send(channelCreateFrame)
            }

            "ServerMemberLeave" -> {
                val serverMemberLeaveFrame = RevoltJson.decodeFromString(ServerMemberLeaveFrame.serializer(), rawFrame)

                RevoltAPI.members.removeMember(serverMemberLeaveFrame.id, serverMemberLeaveFrame.user)

                RevoltAPI.wsFrameChannel.send(serverMemberLeaveFrame)
            }

            "ServerMemberJoin" -> {
                val serverMemberJoinFrame = RevoltJson.decodeFromString(ServerMemberJoinFrame.serializer(), rawFrame)

                fetchMember(serverMemberJoinFrame.id, serverMemberJoinFrame.user)

                RevoltAPI.wsFrameChannel.send(serverMemberJoinFrame)
            }

            // UI-heavy frames that do not need serverside handling.
            // They are simply passed onto the WS frame channel.

            "ChannelStopTyping" -> {
                val channelStopTypingFrame = RevoltJson.decodeFromString(ChannelStopTypingFrame.serializer(), rawFrame)
                RevoltAPI.wsFrameChannel.send(channelStopTypingFrame)
            }

            "ChannelStartTyping" -> {
                val channelStartTypingFrame =
                    RevoltJson.decodeFromString(ChannelStartTypingFrame.serializer(), rawFrame)
                RevoltAPI.wsFrameChannel.send(channelStartTypingFrame)
            }

            else -> {
                logger.debug("Unknown frame of type \"$type\"")
            }
        }
    }

    private suspend fun pushReconnectEvent() {
        RevoltAPI.wsFrameChannel.send(RealtimeSocketFrames.Reconnected)
    }

    suspend fun beginTyping(channelId: String) {
        if (disconnectionState != DisconnectionState.Connected) return

        val beginTypingFrame = BeginTypingFrame("BeginTyping", channelId)
        socket?.send(
            RevoltJson.encodeToString(
                BeginTypingFrame.serializer(),
                beginTypingFrame
            )
        )
    }

    suspend fun endTyping(channelId: String) {
        if (disconnectionState != DisconnectionState.Connected) return

        val endTypingFrame = EndTypingFrame("EndTyping", channelId)
        socket?.send(
            RevoltJson.encodeToString(
                EndTypingFrame.serializer(),
                endTypingFrame
            )
        )
    }
}