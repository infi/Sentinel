package chat.revolt.sentinel.lookout.routes.channel

import chat.revolt.sentinel.lookout.RevoltHttp
import chat.revolt.sentinel.lookout.internals.ULID
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class SendMessageReply(
    val id: String,
    val mention: Boolean
)

@Serializable
data class SendMessageBody(
    val content: String,
    val nonce: String = ULID.makeNext(),
    val replies: List<SendMessageReply> = emptyList(),
    val attachments: List<String>?,
    val embeds: List<SendableEmbed>?
)

@Serializable
data class SendableEmbed(
    val iconUrl: String? = null,
    val url: String? = null,
    val title: String? = null,
    val description: String? = null,
    val media: String? = null,
    val colour: String? = null,
)

suspend fun sendMessage(
    channelId: String,
    content: String? = null,
    nonce: String = ULID.makeNext(),
    replies: List<SendMessageReply>? = null,
    attachments: List<String>? = null,
    embeds: List<SendableEmbed>? = null,
    idempotencyKey: String = ULID.makeNext()
): String {
    val response = RevoltHttp.post("/channels/$channelId/messages") {
        contentType(ContentType.Application.Json)
        setBody(
            SendMessageBody(
                content = content ?: "",
                nonce = nonce,
                replies = replies ?: emptyList(),
                attachments = attachments,
                embeds = embeds
            )
        )
        header("Idempotency-Key", idempotencyKey)
    }
        .bodyAsText()

    return response
}
