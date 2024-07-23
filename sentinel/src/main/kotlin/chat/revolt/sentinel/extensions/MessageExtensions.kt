package chat.revolt.sentinel.extensions

import chat.revolt.sentinel.lookout.RevoltAPI
import chat.revolt.sentinel.lookout.internals.ULID
import chat.revolt.sentinel.lookout.routes.channel.SendMessageReply
import chat.revolt.sentinel.lookout.routes.channel.SendableEmbed
import chat.revolt.sentinel.lookout.routes.channel.sendMessage
import chat.revolt.sentinel.lookout.schemas.Message

suspend fun Message.respond(
    content: String? = null,
    nonce: String = ULID.makeNext(),
    replies: List<SendMessageReply>? = null,
    attachments: List<String>? = null,
    embeds: List<SendableEmbed>? = null,
    idempotencyKey: String = ULID.makeNext()
) {
    if (this.channel == null)
        throw IllegalStateException("Message does not have a channel.")

    sendMessage(
        this.channel!!,
        content,
        nonce,
        replies,
        attachments,
        embeds,
        idempotencyKey
    )
}

suspend fun Message.reply(
    content: String? = null,
    nonce: String = ULID.makeNext(),
    replies: List<SendMessageReply> = emptyList(),
    attachments: List<String>? = null,
    embeds: List<SendableEmbed>? = null,
    idempotencyKey: String = ULID.makeNext()
) {
    if (this.channel == null)
        throw IllegalStateException("Message does not have a channel.")
    if (this.id == null)
        throw IllegalStateException("Message does not have an ID.")


    sendMessage(
        this.channel!!,
        content,
        nonce,
        replies + SendMessageReply(this.id!!, true),
        attachments,
        embeds,
        idempotencyKey
    )
}

val Message.server: String?
    get() = this.channel?.let { RevoltAPI.channelCache[it]?.server }