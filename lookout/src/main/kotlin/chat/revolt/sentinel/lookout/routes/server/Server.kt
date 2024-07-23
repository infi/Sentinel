package chat.revolt.sentinel.lookout.routes.server

import chat.revolt.sentinel.lookout.RevoltAPI
import chat.revolt.sentinel.lookout.RevoltError
import chat.revolt.sentinel.lookout.RevoltHttp
import chat.revolt.sentinel.lookout.RevoltJson
import chat.revolt.sentinel.lookout.schemas.Member
import chat.revolt.sentinel.lookout.schemas.User
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
data class FetchMembersResponse(
    val members: List<Member>,
    val users: List<User>
)

suspend fun fetchMembers(
    serverId: String,
    includeOffline: Boolean = false,
    pure: Boolean = false
): FetchMembersResponse {
    val response = RevoltHttp.get("/servers/$serverId/members") {
        parameter("exclude_offline", !includeOffline)
    }

    val responseContent = response.bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), responseContent)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val membersResponse =
        RevoltJson.decodeFromString(FetchMembersResponse.serializer(), responseContent)

    if (pure) {
        return membersResponse
    }

    membersResponse.members.forEach { member ->
        if (!RevoltAPI.members.hasMember(serverId, member.id!!.user)) {
            RevoltAPI.members.setMember(serverId, member)
        }
    }

    membersResponse.users.forEach { user ->
        user.id?.let { RevoltAPI.userCache.putIfAbsent(it, user) }
    }

    return membersResponse
}

suspend fun fetchMember(serverId: String, userId: String, pure: Boolean = false): Member {
    val response = RevoltHttp.get("/servers/$serverId/members/$userId")

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response.bodyAsText())
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val member = RevoltJson.decodeFromString(Member.serializer(), response.bodyAsText())

    if (!pure) {
        member.id?.let {
            if (!RevoltAPI.members.hasMember(serverId, it.user)) {
                RevoltAPI.members.setMember(serverId, member)
            }
        }
    }

    return member
}