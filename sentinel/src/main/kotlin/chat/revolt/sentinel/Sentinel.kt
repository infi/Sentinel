package chat.revolt.sentinel

import chat.revolt.sentinel.lookout.RevoltAPI
import chat.revolt.sentinel.lookout.auth.BotToken
import chat.revolt.sentinel.lookout.realtime.frames.receivable.MessageFrame
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("Sentinel")

fun main() {
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
        withContext(RevoltAPI.realtimeContext) {
            runBlocking {
                flow {
                    while (true) {
                        emit(RevoltAPI.wsFrameChannel.receive())
                    }
                }.onEach {
                    logger.debug("Got {}", it)
                }
            }
        }
    }

    runBlocking {
        RevoltAPI.login(BotToken(""))
    }
}