package chat.revolt.sentinel

import chat.revolt.sentinel.framework.SentinelFramework
import chat.revolt.sentinel.lookout.RevoltAPI
import chat.revolt.sentinel.lookout.auth.BotToken
import chat.revolt.sentinel.lookout.realtime.frames.receivable.MessageFrame
import chat.revolt.sentinel.lookout.routes.channel.SendableEmbed
import chat.revolt.sentinel.lookout.routes.channel.sendMessage
import chat.revolt.sentinel.lookout.routes.user.fetchSelf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("Sentinel")

fun main() {
    SentinelFramework.init(Database.connect("jdbc:h2:./sentinel", driver = "org.h2.Driver"))

    runBlocking {
        launch {
            withContext(RevoltAPI.realtimeContext) {
                // Receive from the Channel RevoltAPI.wsFrameChannel and log the frame type
                while (true) {
                    RevoltAPI.wsFrameChannel.receive().let {
                        when (it) {
                            is MessageFrame -> {
                                SentinelFramework.onMessage(it)
                            }
                        }
                    }
                }
            }
        }
        launch {
            RevoltAPI.login(BotToken("UiSsro1RLxsBdwdvQOZQ5wGQrv2CR6_RZKESQ37g2OWDLBC-XcO1CMOzGqEariDH"))
        }
    }
}