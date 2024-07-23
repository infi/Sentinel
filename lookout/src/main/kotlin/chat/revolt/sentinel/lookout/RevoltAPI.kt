package chat.revolt.sentinel.lookout

import chat.revolt.sentinel.lookout.auth.LoginStrategy
import chat.revolt.sentinel.lookout.internals.Members
import chat.revolt.sentinel.lookout.realtime.RealtimeSocket
import chat.revolt.sentinel.lookout.routes.user.fetchSelf
import chat.revolt.sentinel.lookout.schemas.Emoji
import chat.revolt.sentinel.lookout.schemas.Message
import chat.revolt.sentinel.lookout.schemas.Server
import chat.revolt.sentinel.lookout.schemas.User
import chat.revolt.sentinel.lookout.schemas.Channel as ChannelSchema
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.net.SocketException
import java.util.*

val RevoltJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

val RevoltHttp = HttpClient(OkHttp) {
    install(DefaultRequest)
    install(ContentNegotiation) {
        json(RevoltJson)
    }

    install(WebSockets)

    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = 5)
        retryOnException(maxRetries = 5)

        modifyRequest { request ->
            request.headers.append("x-retry-count", retryCount.toString())
        }

        exponentialDelay()
    }

    engine {
        addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .apply {
                    RevoltAPI.loginStrategy?.let { strategy ->
                        val headerName = strategy.loginHeaderName()
                        if (chain.request().headers[headerName] == null) {
                            RevoltAPI.authToken?.let { token ->
                                header(headerName, token)
                            }
                        }
                    }
                }
                .build()
            chain.proceed(request)
        }
    }

    defaultRequest {
        url(Constants.REVOLT_BASE)
        header("User-Agent", buildUserAgent())
    }
}

private fun buildUserAgent(): String {
    return "Lookout (Kotlin ${KotlinVersion.CURRENT})"
}

object RevoltAPI {
    val logger = LoggerFactory.getLogger(this::class.java)!!

    val userCache = mutableMapOf<String, User>()
    val serverCache = mutableMapOf<String, Server>()
    val channelCache = mutableMapOf<String, ChannelSchema>()
    val emojiCache = mutableMapOf<String, Emoji>()
    val messageCache = mutableMapOf<String, Message>()

    var loginStrategy: LoginStrategy? = null

    var authToken: String? = null
    var selfId: String? = null

    var sessionToken: String = ""
        private set
    var sessionId: String = ""
        private set

    val members = Members()

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    val realtimeContext = newSingleThreadContext("RealtimeContext")
    val wsFrameChannel = Channel<Any>(Channel.UNLIMITED)

    suspend fun login(strat: LoginStrategy) {
        loginStrategy = strat
        authToken = loginStrategy?.receiveToken()
        startSocketOps()
    }

    val scope = CoroutineScope(Dispatchers.IO)

    class SendPingTask : TimerTask() {
        override fun run() {
            scope.launch {
                RealtimeSocket.sendPing()
            }
        }
    }

    private var socketCoroutine: Job? = null
    suspend fun connectWS() {
        socketCoroutine = scope.launch {
            withContext(realtimeContext) {
                try {
                    val result = RealtimeSocket.connect()
                    if (result.isFailure) {
                        throw result.exceptionOrNull() ?: Exception("WS failure")
                    }
                } catch (e: Exception) {
                    if (e is SocketException) {
                        logger.debug(
                            "Socket closed, probably no big deal /// " + e.message
                        )
                    } else {
                        logger.error("WebSocket error", e)
                    }
                }
            }
        }
    }

    var timer: Timer? = null
    private suspend fun startSocketOps() {
        fetchSelf()
        connectWS()

        // Send a ping to the WS server every 30 seconds. Unlike Killjoy this uses a JVM timer
        timer = Timer().apply {
            scheduleAtFixedRate(SendPingTask(), 0, 30 * 1000)
        }
    }

    fun logout() {
        selfId = null
        sessionToken = ""
        sessionId = ""

        userCache.clear()
        serverCache.clear()
        channelCache.clear()
        emojiCache.clear()
        messageCache.clear()

        members.clear()
        /*
        TODO unreads.clear()
         */

        socketCoroutine?.cancel()
        timer?.cancel()
    }
}

@Serializable
data class RevoltError(val type: String)