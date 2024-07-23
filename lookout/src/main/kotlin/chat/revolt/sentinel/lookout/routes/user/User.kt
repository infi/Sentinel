package chat.revolt.sentinel.lookout.routes.user

import chat.revolt.sentinel.lookout.RevoltAPI
import chat.revolt.sentinel.lookout.RevoltError
import chat.revolt.sentinel.lookout.RevoltHttp
import chat.revolt.sentinel.lookout.RevoltJson
import chat.revolt.sentinel.lookout.schemas.User
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.SerializationException

suspend fun fetchSelf(): User {
    val response = RevoltHttp.get("/users/@me")
        .bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val user = RevoltJson.decodeFromString(User.serializer(), response)

    if (user.id == null) {
        throw Exception("Self user ID is null")
    }

    RevoltAPI.userCache[user.id] = user
    RevoltAPI.selfId = user.id

    return user
}